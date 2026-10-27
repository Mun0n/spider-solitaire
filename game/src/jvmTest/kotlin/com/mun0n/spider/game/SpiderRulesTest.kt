package com.mun0n.spider.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SpiderRulesTest {
    @Test
    fun dealCountsAndFaceUpTops() {
        val state = SpiderRules.newGame(Difficulty.ONE, Random(7))
        assertEquals(10, state.columns.size)
        assertEquals(6, state.columns[0].size)
        assertEquals(6, state.columns[3].size)
        assertEquals(5, state.columns[4].size)
        assertEquals(5, state.columns[9].size)
        assertEquals(50, state.stock.size)
        assertEquals(104, state.columns.sumOf { it.size } + state.stock.size)
        state.columns.forEach { column ->
            assertTrue(column.last().faceUp)
            assertTrue(column.dropLast(1).all { !it.faceUp })
        }
        assertTrue(state.stock.all { !it.faceUp })
        val ids = (state.columns.flatten() + state.stock).map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(SpiderRules.START_SCORE, state.score)
        assertFalse(state.won)
    }

    @Test
    fun deckComposition() {
        fun ranks(cards: List<Card>, suit: Suit) =
            cards.filter { it.suit == suit }.groupingBy { it.rank }.eachCount()

        val one = SpiderRules.buildDeck(Difficulty.ONE)
        assertEquals(104, one.size)
        assertTrue(one.all { it.suit == Suit.SPADES })
        assertTrue(Rank.entries.all { ranks(one, Suit.SPADES)[it] == 8 })

        val two = SpiderRules.buildDeck(Difficulty.TWO)
        assertTrue(two.all { it.suit == Suit.SPADES || it.suit == Suit.HEARTS })
        assertTrue(Rank.entries.all { ranks(two, Suit.SPADES)[it] == 4 })
        assertTrue(Rank.entries.all { ranks(two, Suit.HEARTS)[it] == 4 })

        val four = SpiderRules.buildDeck(Difficulty.FOUR)
        Suit.entries.forEach { suit ->
            assertTrue(Rank.entries.all { ranks(four, suit)[it] == 2 })
        }
    }

    @Test
    fun sameSuitRunMovesOntoAnySuitOfNextRank() {
        val queen = c(1, Suit.HEARTS, Rank.QUEEN)
        val jack = c(2, Suit.HEARTS, Rank.JACK)
        val king = c(3, Suit.SPADES, Rank.KING)
        val state = board(listOf(listOf(queen, jack), listOf(king)))
        assertTrue(SpiderRules.canMove(state, 0, 0, 1))
        assertFalse(SpiderRules.canMove(state, 0, 1, 1))
        val moved = SpiderRules.move(state, 0, 0, 1)!!
        assertEquals(listOf(king, queen, jack), moved.state.columns[1])
        assertTrue(moved.state.columns[0].isEmpty())
        assertEquals(499, moved.state.score)
        assertEquals(1, moved.state.moves)
    }

    @Test
    fun mixedSuitRunIsRejectedButTheTailCanMove() {
        val king = c(1, Suit.SPADES, Rank.KING)
        val queen = c(2, Suit.HEARTS, Rank.QUEEN)
        val jack = c(3, Suit.HEARTS, Rank.JACK)
        val target = c(4, Suit.CLUBS, Rank.KING)
        val state = board(listOf(listOf(king, queen, jack), listOf(target)))
        assertFalse(SpiderRules.isMovableRun(state.columns[0], 0))
        assertTrue(SpiderRules.isMovableRun(state.columns[0], 1))
        assertNull(SpiderRules.move(state, 0, 0, 1))
        val moved = SpiderRules.move(state, 0, 1, 1)!!
        assertEquals(listOf(king), moved.state.columns[0])
        assertEquals(listOf(target, queen, jack), moved.state.columns[1])
    }

    @Test
    fun faceDownCardCannotMoveAndWrongRankIsRejected() {
        val hidden = c(1, Suit.SPADES, Rank.KING, up = false)
        val five = c(2, Suit.SPADES, Rank.FIVE)
        val queen = c(3, Suit.HEARTS, Rank.QUEEN)
        val state = board(listOf(listOf(hidden, five), listOf(queen)))
        assertFalse(SpiderRules.canMove(state, 0, 0, 1))
        assertFalse(SpiderRules.canMove(state, 0, 1, 1))
    }

    @Test
    fun emptyColumnAcceptsAnyRunAndUncoversTheCardBelow() {
        val hidden = c(1, Suit.CLUBS, Rank.TEN, up = false)
        val nine = c(2, Suit.DIAMONDS, Rank.NINE)
        val eight = c(3, Suit.DIAMONDS, Rank.EIGHT)
        val state = board(listOf(listOf(hidden, nine, eight), emptyList()))
        val moved = SpiderRules.move(state, 0, 1, 1)!!
        assertEquals(1, moved.state.columns[0].size)
        assertTrue(moved.state.columns[0].single().faceUp)
        assertEquals(Rank.TEN, moved.state.columns[0].single().rank)
        assertEquals(listOf(nine, eight), moved.state.columns[1])
    }

    @Test
    fun emptyColumnBlocksTheDeal() {
        val columns = List(10) { index ->
            when (index) {
                0 -> listOf(c(0, Suit.SPADES, Rank.FOUR))
                1 -> listOf(c(1, Suit.HEARTS, Rank.FIVE))
                else -> listOf(c(index, Suit.CLUBS, Rank.KING))
            }
        }
        val emptied = SpiderRules.move(
            board(
                columns = columns,
                stock = List(10) { c(100 + it, Suit.SPADES, Rank.ACE, up = false) },
            ),
            0,
            0,
            1,
        )!!.state
        assertTrue(emptied.columns[0].isEmpty())
        assertEquals(DealBlock.EmptyColumn, SpiderRules.dealBlock(emptied))
        assertNull(SpiderRules.deal(emptied))
    }

    @Test
    fun oneDealCanFinishTwoSuits() {
        val columns = MutableList(10) { index ->
            listOf(c(200 + index, Suit.CLUBS, Rank.KING))
        }
        columns[0] = spadeRun(1).dropLast(1)
        columns[1] = spadeRun(20).map { it.copy(suit = Suit.HEARTS) }.dropLast(1)
        val stock = MutableList(10) { index ->
            c(300 + index, Suit.DIAMONDS, Rank.KING, up = false)
        }
        stock[0] = c(400, Suit.SPADES, Rank.ACE, up = false)
        stock[1] = c(401, Suit.HEARTS, Rank.ACE, up = false)
        val dealt = SpiderRules.deal(board(columns, stock))!!
        assertEquals(2, dealt.removed.size)
        assertEquals(setOf(Suit.SPADES, Suit.HEARTS), dealt.state.finished.toSet())
        assertEquals(500 - 1 + 200, dealt.state.score)
        assertTrue(dealt.state.stock.isEmpty())
    }

    @Test
    fun dealAddsOneFaceUpCardPerColumnAndSpendsAPoint() {
        val state = board(
            columns = List(10) { listOf(c(it, Suit.HEARTS, Rank.KING)) },
            stock = List(10) { c(50 + it, Suit.SPADES, Rank.TWO, up = false) },
        )
        val dealt = SpiderRules.deal(state)!!
        assertTrue(dealt.state.stock.isEmpty())
        dealt.state.columns.forEach { column ->
            assertEquals(2, column.size)
            assertEquals(Rank.TWO, column.last().rank)
            assertTrue(column.last().faceUp)
        }
        assertEquals(10, dealt.dealtCards.size)
        assertEquals(499, dealt.state.score)
        assertEquals(DealBlock.NoCards, SpiderRules.dealBlock(dealt.state))
    }

    @Test
    fun completingKingToAceRemovesItFlipsTheCardBelowAndScores() {
        val buried = c(0, Suit.CLUBS, Rank.FOUR, up = false)
        val run = spadeRun(startId = 1)
        val state = board(
            columns = listOf(listOf(buried) + run.dropLast(1), listOf(run.last())),
            score = 480,
            completed = emptyList(),
        )
        assertEquals(Rank.TWO, state.columns[0].last().rank)
        assertEquals(Rank.ACE, state.columns[1].single().rank)
        val moved = SpiderRules.move(state, 1, 0, 0)!!
        assertEquals(1, moved.removed.size)
        assertEquals(Rank.KING, moved.removed.single().cards.first().rank)
        assertEquals(Rank.ACE, moved.removed.single().cards.last().rank)
        assertEquals(1, moved.state.columns[0].size)
        assertEquals(buried.copy(faceUp = true), moved.state.columns[0].single())
        assertEquals(listOf(Suit.SPADES), moved.state.finished)
        assertEquals(480 - 1 + 100, moved.state.score)
        assertFalse(moved.state.won)
    }

    @Test
    fun movingACoverOffAFinishedRunAlsoRemovesIt() {
        val run = spadeRun(startId = 1)
        val cover = c(99, Suit.HEARTS, Rank.SEVEN)
        val home = c(98, Suit.CLUBS, Rank.EIGHT)
        val state = board(listOf(run + cover, listOf(home)))
        val moved = SpiderRules.move(state, 0, 13, 1)!!
        assertTrue(moved.state.columns[0].isEmpty())
        assertEquals(listOf(home, cover), moved.state.columns[1])
        assertEquals(listOf(Suit.SPADES), moved.state.finished)
    }

    @Test
    fun eighthSuitWins() {
        val run = spadeRun(startId = 1)
        val finished = List(7) { Suit.SPADES }
        val state = board(
            columns = listOf(run.dropLast(1), listOf(run.last())),
            completed = finished,
        )
        val moved = SpiderRules.move(state, 1, 0, 0)!!
        assertTrue(moved.state.won)
        assertEquals(8, moved.state.completed)
        assertNull(SpiderRules.move(moved.state, 0, 0, 1))
        assertNull(SpiderRules.hint(moved.state))
    }

    @Test
    fun undoRestoresTheWholeSnapshotIncludingScore() {
        val session = GameSession(SpiderRules.newGame(Difficulty.FOUR, Random(3)))
        val before = session.state
        val hint = SpiderRules.hint(before) as Hint.MoveCards
        assertNotNull(session.move(hint.fromCol, hint.fromIndex, hint.toCol))
        assertTrue(session.canUndo)
        assertTrue(session.undo())
        assertEquals(before, session.state)
        assertFalse(session.canUndo)
    }

    @Test
    fun hintPrefersUncoveringAFaceDownCard() {
        val hidden = c(1, Suit.SPADES, Rank.TEN, up = false)
        val five = c(2, Suit.HEARTS, Rank.FIVE)
        val six = c(3, Suit.CLUBS, Rank.SIX)
        val queen = c(4, Suit.HEARTS, Rank.QUEEN)
        val jack = c(5, Suit.HEARTS, Rank.JACK)
        val state = board(
            listOf(
                listOf(hidden, five),
                listOf(six),
                listOf(queen, jack),
            ),
        )
        val hint = SpiderRules.hint(state)
        assertEquals(Hint.MoveCards(fromCol = 0, fromIndex = 1, toCol = 1), hint)
    }

    @Test
    fun tapTargetPrefersTheSameSuitOverAnotherRank() {
        val five = c(1, Suit.HEARTS, Rank.FIVE)
        val sixClubs = c(2, Suit.CLUBS, Rank.SIX)
        val sixHearts = c(3, Suit.HEARTS, Rank.SIX)
        val state = board(listOf(listOf(five), listOf(sixClubs), listOf(sixHearts)))
        assertEquals(2, SpiderRules.bestTarget(state, 0, 0))
    }

    @Test
    fun tapTargetKeepsAPileAheadOfAnEmptyColumn() {
        val five = c(1, Suit.SPADES, Rank.FIVE)
        val six = c(2, Suit.HEARTS, Rank.SIX)
        val state = board(listOf(listOf(five), listOf(six), emptyList()))
        assertEquals(1, SpiderRules.bestTarget(state, 0, 0))
    }

    @Test
    fun tapTargetUsesAnEmptyColumnWhenNothingElseFits() {
        val king = c(1, Suit.SPADES, Rank.KING)
        val ace = c(2, Suit.HEARTS, Rank.ACE)
        val state = board(listOf(listOf(king), emptyList(), listOf(ace)))
        assertEquals(1, SpiderRules.bestTarget(state, 0, 0))
    }

    @Test
    fun tapTargetIsNullWhenTheCardFitsNowhere() {
        val ace = c(1, Suit.HEARTS, Rank.ACE)
        val columns = List(10) { index ->
            if (index == 0) listOf(ace) else listOf(c(10 + index, Suit.SPADES, Rank.KING))
        }
        assertNull(SpiderRules.bestTarget(board(columns), 0, 0))
    }

    @Test
    fun hintTourShowsEachPileThatCanTakeTheRunThenStops() {
        val hidden = c(1, Suit.SPADES, Rank.TEN, up = false)
        val five = c(2, Suit.HEARTS, Rank.FIVE)
        val sixClubs = c(3, Suit.CLUBS, Rank.SIX)
        val sixHearts = c(4, Suit.HEARTS, Rank.SIX)
        val state = board(
            listOf(listOf(hidden, five), listOf(sixClubs), listOf(sixHearts), emptyList()),
        )
        assertEquals(
            listOf(
                Hint.MoveCards(fromCol = 0, fromIndex = 1, toCol = 1),
                Hint.MoveCards(fromCol = 0, fromIndex = 1, toCol = 2),
            ),
            SpiderRules.hintTour(state),
        )
    }

    @Test
    fun hintTourOfADealIsJustTheStock() {
        val columns = List(10) { index ->
            listOf(c(index, Suit.SPADES, Rank.KING))
        }
        val state = board(
            columns = columns,
            stock = List(10) { c(50 + it, Suit.SPADES, Rank.ACE, up = false) },
        )
        assertEquals(listOf(Hint.Deal), SpiderRules.hintTour(state))
    }

    @Test
    fun hintFallsBackToTheStockWhenNothingMoves() {
        val columns = List(10) { index ->
            listOf(c(index, Suit.SPADES, Rank.KING))
        }
        val state = board(
            columns = columns,
            stock = List(10) { c(50 + it, Suit.SPADES, Rank.ACE, up = false) },
        )
        assertEquals(Hint.Deal, SpiderRules.hint(state))
    }

    @Test
    fun inSuitAutoplayDoesNotBounceBetweenDuplicateRanks() {
        val seven = c(1, Suit.SPADES, Rank.SEVEN)
        val eightA = c(2, Suit.SPADES, Rank.EIGHT)
        val eightB = c(3, Suit.SPADES, Rank.EIGHT)
        var state = board(listOf(listOf(seven), listOf(eightA), listOf(eightB)))
        assertTrue(SpiderRules.readyToAutoFinish(state))
        var guard = 0
        while (guard++ < 12) {
            val move = SpiderRules.nextInSuitMove(state) ?: break
            state = SpiderRules.move(state, move.first, move.second, move.third)!!.state
        }
        assertTrue(guard < 12)
        assertNull(SpiderRules.nextInSuitMove(state))
        val built = state.columns.first { column -> column.size == 2 }
        assertEquals(listOf(Rank.EIGHT, Rank.SEVEN), built.map { it.rank })
        assertTrue(built.all { it.suit == Suit.SPADES })
    }

    @Test
    fun autoplayStaysOffWhileCardsAreFaceDownOrStockRemains() {
        val state = SpiderRules.newGame(Difficulty.ONE, Random(1))
        assertFalse(SpiderRules.readyToAutoFinish(state))
        assertNull(SpiderRules.nextInSuitMove(state))
    }

    @Test
    fun restoreRunsPutsTheTopRunBackLast() {
        val lower = spadeRun(1).map { it.copy(suit = Suit.HEARTS) }
        val upper = spadeRun(20)
        val prefix = listOf(c(0, Suit.CLUBS, Rank.ACE))
        val restored = SpiderRules.restoreRuns(
            columns = List(10) { if (it == 2) prefix else emptyList() },
            removed = listOf(
                RemovedRun(2, upper),
                RemovedRun(2, lower),
            ),
        )
        assertEquals(prefix + lower + upper, restored[2])
    }

    @Test
    fun saveRoundTripKeepsHistory() {
        val session = GameSession(SpiderRules.newGame(Difficulty.TWO, Random(11)))
        val hint = SpiderRules.hint(session.state) as Hint.MoveCards
        session.move(hint.fromCol, hint.fromIndex, hint.toCol)
        val save = SaveData(Difficulty.TWO, session.state, session.history())
        val decoded = SaveCodec.decode(SaveCodec.encode(save))
        assertEquals(save, decoded)
        val resumed = GameSession(decoded.game!!, decoded.history)
        assertTrue(resumed.undo())
        assertEquals(session.history().single(), resumed.state)
    }

    @Test
    fun emptySaveRoundTrip() {
        val save = SaveData(Difficulty.FOUR, game = null, history = emptyList())
        assertEquals(save, SaveCodec.decode(SaveCodec.encode(save)))
    }

    private fun c(id: Int, suit: Suit, rank: Rank, up: Boolean = true) =
        Card(id, suit, rank, up)

    private fun spadeRun(startId: Int): List<Card> =
        (0 until 13).map { offset ->
            c(startId + offset, Suit.SPADES, Rank.fromValue(13 - offset))
        }

    private fun board(
        columns: List<List<Card>>,
        stock: List<Card> = emptyList(),
        score: Int = 500,
        completed: List<Suit> = emptyList(),
    ): GameState {
        val padded = columns.toMutableList()
        while (padded.size < 10) padded.add(emptyList())
        return GameState(
            difficulty = Difficulty.ONE,
            columns = padded,
            stock = stock,
            finished = completed,
            score = score,
            moves = 0,
            won = false,
        )
    }
}
