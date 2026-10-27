package com.mun0n.spider.game

import kotlin.random.Random

object SpiderRules {
    const val COLUMN_COUNT = 10
    const val START_SCORE = 500
    const val SUIT_BONUS = 100
    const val DECK_SIZE = 104
    private const val RUN_LENGTH = 13

    fun newGame(difficulty: Difficulty, random: Random = Random.Default): GameState {
        val deck = buildDeck(difficulty).shuffled(random).toMutableList()
        val columns = List(COLUMN_COUNT) { mutableListOf<Card>() }
        for (col in 0 until COLUMN_COUNT) {
            val count = if (col < 4) 6 else 5
            repeat(count) {
                columns[col] += deck.removeAt(0)
            }
            val top = columns[col].last()
            columns[col][columns[col].lastIndex] = top.copy(faceUp = true)
        }
        return GameState(
            difficulty = difficulty,
            columns = columns.map { it.toList() },
            stock = deck.toList(),
            finished = emptyList(),
            score = START_SCORE,
            moves = 0,
            won = false,
        )
    }

    fun buildDeck(difficulty: Difficulty): List<Card> {
        val suits = when (difficulty) {
            Difficulty.ONE -> List(8) { Suit.SPADES }
            Difficulty.TWO -> List(4) { Suit.SPADES } + List(4) { Suit.HEARTS }
            Difficulty.FOUR -> Suit.entries.flatMap { suit -> List(2) { suit } }
        }
        var id = 0
        return suits.flatMap { suit ->
            Rank.entries.map { rank ->
                Card(id = id++, suit = suit, rank = rank, faceUp = false)
            }
        }
    }

    fun isMovableRun(column: List<Card>, fromIndex: Int): Boolean {
        if (fromIndex !in column.indices) return false
        if (!column[fromIndex].faceUp) return false
        for (i in fromIndex until column.lastIndex) {
            val current = column[i]
            val next = column[i + 1]
            if (!next.faceUp) return false
            if (current.suit != next.suit) return false
            if (current.rank.value != next.rank.value + 1) return false
        }
        return true
    }

    fun canMove(state: GameState, fromCol: Int, fromIndex: Int, toCol: Int): Boolean {
        if (state.won) return false
        if (fromCol !in 0 until COLUMN_COUNT || toCol !in 0 until COLUMN_COUNT) return false
        if (fromCol == toCol) return false
        val from = state.columns[fromCol]
        if (!isMovableRun(from, fromIndex)) return false
        val moving = from[fromIndex]
        val dest = state.columns[toCol]
        if (dest.isEmpty()) return true
        val top = dest.last()
        return top.faceUp && top.rank.value == moving.rank.value + 1
    }

    fun move(state: GameState, fromCol: Int, fromIndex: Int, toCol: Int): ActionResult? {
        if (!canMove(state, fromCol, fromIndex, toCol)) return null
        val cols = state.columns.map { it.toMutableList() }
        val moving = cols[fromCol].subList(fromIndex, cols[fromCol].size).toList()
        repeat(moving.size) { cols[fromCol].removeAt(cols[fromCol].lastIndex) }
        revealTop(cols[fromCol])
        cols[toCol].addAll(moving)
        return commit(state, cols, dealtCards = emptyList())
    }

    fun dealBlock(state: GameState): DealBlock? {
        if (state.won || state.stock.size < COLUMN_COUNT) return DealBlock.NoCards
        if (state.columns.any { it.isEmpty() }) return DealBlock.EmptyColumn
        return null
    }

    fun canDeal(state: GameState): Boolean = dealBlock(state) == null

    fun deal(state: GameState): ActionResult? {
        if (!canDeal(state)) return null
        val cols = state.columns.map { it.toMutableList() }
        val dealt = state.stock.take(COLUMN_COUNT)
        dealt.forEachIndexed { index, card ->
            cols[index].add(card.copy(faceUp = true))
        }
        val next = state.copy(stock = state.stock.drop(COLUMN_COUNT))
        return commit(next, cols, dealtCards = dealt.map { it.copy(faceUp = true) })
    }

    fun hint(state: GameState): Hint? {
        if (state.won) return null
        data class Candidate(
            val fromCol: Int,
            val fromIndex: Int,
            val toCol: Int,
            val uncovers: Boolean,
            val inSuit: Boolean,
            val toEmpty: Boolean,
        )

        val candidates = mutableListOf<Candidate>()
        for (fromCol in 0 until COLUMN_COUNT) {
            val column = state.columns[fromCol]
            for (fromIndex in column.indices) {
                if (!isMovableRun(column, fromIndex)) continue
                val uncovers = fromIndex > 0 && !column[fromIndex - 1].faceUp
                for (toCol in 0 until COLUMN_COUNT) {
                    if (!canMove(state, fromCol, fromIndex, toCol)) continue
                    val dest = state.columns[toCol]
                    candidates += Candidate(
                        fromCol = fromCol,
                        fromIndex = fromIndex,
                        toCol = toCol,
                        uncovers = uncovers,
                        inSuit = dest.isNotEmpty() && dest.last().suit == column[fromIndex].suit,
                        toEmpty = dest.isEmpty(),
                    )
                }
            }
        }

        fun Candidate.asHint() = Hint.MoveCards(fromCol, fromIndex, toCol)

        candidates.firstOrNull { it.uncovers && !it.toEmpty }?.let { return it.asHint() }
        candidates.firstOrNull { it.uncovers }?.let { return it.asHint() }
        candidates.firstOrNull { it.inSuit }?.let { return it.asHint() }
        candidates.firstOrNull { !it.toEmpty }?.let { return it.asHint() }
        candidates.firstOrNull()?.let { return it.asHint() }
        if (canDeal(state)) return Hint.Deal
        return null
    }

    fun bestTarget(state: GameState, fromCol: Int, fromIndex: Int): Int? =
        landingSpots(state, fromCol, fromIndex).firstOrNull()

    /**
     * La jugada que sugiere [hint], y después los otros montones donde esa
     * misma escalera encaja. Una columna vacía solo entra si es la sugerida.
     */
    fun hintTour(state: GameState): List<Hint> {
        val chosen = hint(state) ?: return emptyList()
        val move = chosen as? Hint.MoveCards ?: return listOf(chosen)
        val extras = landingSpots(state, move.fromCol, move.fromIndex)
            .filter { toCol -> toCol != move.toCol && state.columns[toCol].isNotEmpty() }
        return buildList {
            add(move)
            extras.forEach { toCol -> add(Hint.MoveCards(move.fromCol, move.fromIndex, toCol)) }
        }
    }

    private fun landingSpots(state: GameState, fromCol: Int, fromIndex: Int): List<Int> {
        val column = state.columns.getOrNull(fromCol) ?: return emptyList()
        if (fromIndex !in column.indices || !isMovableRun(column, fromIndex)) return emptyList()
        val moving = column[fromIndex]
        return (0 until COLUMN_COUNT)
            .filter { toCol -> canMove(state, fromCol, fromIndex, toCol) }
            .sortedWith(
                compareBy<Int> { toCol ->
                    val dest = state.columns[toCol]
                    when {
                        dest.isNotEmpty() && dest.last().suit == moving.suit -> 0
                        dest.isNotEmpty() -> 1
                        else -> 2
                    }
                }.thenBy { it },
            )
    }

    fun readyToAutoFinish(state: GameState): Boolean {
        if (state.won || state.stock.isNotEmpty()) return false
        if (state.columns.all { it.isEmpty() }) return false
        return state.columns.all { column -> column.all { it.faceUp } }
    }

    /**
     * Siguiente movimiento que alarga un palo y no deshace una unión ya hecha.
     * Cada paso aumenta las parejas del mismo palo, así que no cicla.
     */
    fun nextInSuitMove(state: GameState): Triple<Int, Int, Int>? {
        if (!readyToAutoFinish(state)) return null
        var best: Triple<Int, Int, Int>? = null
        var bestLength = 0
        for (fromCol in 0 until COLUMN_COUNT) {
            val column = state.columns[fromCol]
            for (fromIndex in column.indices) {
                if (!isMovableRun(column, fromIndex)) continue
                if (alreadyInSuit(column, fromIndex)) continue
                val length = column.size - fromIndex
                for (toCol in 0 until COLUMN_COUNT) {
                    if (!canMove(state, fromCol, fromIndex, toCol)) continue
                    val dest = state.columns[toCol]
                    if (dest.isEmpty() || dest.last().suit != column[fromIndex].suit) continue
                    if (length > bestLength) {
                        bestLength = length
                        best = Triple(fromCol, fromIndex, toCol)
                    }
                }
            }
        }
        return best
    }

    fun restoreRuns(
        columns: List<List<Card>>,
        removed: List<RemovedRun>,
    ): List<List<Card>> {
        val extra = removed
            .groupBy { it.column }
            .mapValues { (_, runs) -> runs.asReversed().flatMap { it.cards } }
        return columns.mapIndexed { index, column -> column + extra[index].orEmpty() }
    }

    private fun alreadyInSuit(column: List<Card>, fromIndex: Int): Boolean {
        if (fromIndex == 0) return false
        val below = column[fromIndex - 1]
        val card = column[fromIndex]
        return below.faceUp &&
            below.suit == card.suit &&
            below.rank.value == card.rank.value + 1
    }

    private fun commit(
        state: GameState,
        columns: List<MutableList<Card>>,
        dealtCards: List<Card>,
    ): ActionResult {
        val mid = columns.map { it.toList() }
        val removed = mutableListOf<RemovedRun>()
        val finished = state.finished.toMutableList()
        var guard = 0
        while (guard++ < 16) {
            var found = false
            for (col in 0 until COLUMN_COUNT) {
                val start = completedRunStart(columns[col]) ?: continue
                val cards = columns[col].subList(start, columns[col].size).toList()
                repeat(RUN_LENGTH) { columns[col].removeAt(columns[col].lastIndex) }
                revealTop(columns[col])
                removed += RemovedRun(col, cards)
                finished += cards.first().suit
                found = true
            }
            if (!found) break
        }
        val completed = finished.size
        return ActionResult(
            state = state.copy(
                columns = columns.map { it.toList() },
                finished = finished.toList(),
                score = state.score - 1 + removed.size * SUIT_BONUS,
                moves = state.moves + 1,
                won = completed >= 8,
            ),
            midColumns = mid,
            removed = removed.toList(),
            dealtCards = dealtCards,
        )
    }

    private fun revealTop(column: MutableList<Card>) {
        if (column.isEmpty()) return
        val top = column.last()
        if (!top.faceUp) column[column.lastIndex] = top.copy(faceUp = true)
    }

    private fun completedRunStart(column: List<Card>): Int? {
        if (column.size < RUN_LENGTH) return null
        val start = column.size - RUN_LENGTH
        val suit = column[start].suit
        for (offset in 0 until RUN_LENGTH) {
            val card = column[start + offset]
            if (!card.faceUp) return null
            if (card.suit != suit) return null
            if (card.rank.value != Rank.KING.value - offset) return null
        }
        return start
    }
}
