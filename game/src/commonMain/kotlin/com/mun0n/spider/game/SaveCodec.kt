package com.mun0n.spider.game

data class SaveData(
    val lastDifficulty: Difficulty,
    val game: GameState?,
    val history: List<GameState>,
)

object SaveCodec {
    fun encode(save: SaveData): String = buildString {
        appendLine("SPIDER1")
        appendLine(save.lastDifficulty.name)
        val game = save.game
        if (game == null) {
            appendLine("NOGAME")
            return@buildString
        }
        appendLine("GAME")
        appendState(this, game)
        appendLine("HISTORY ${save.history.size}")
        save.history.forEach { appendState(this, it) }
    }

    fun decode(text: String): SaveData {
        val parser = Parser(text)
        check(parser.next() == "SPIDER1") { "archivo desconocido" }
        val difficulty = Difficulty.valueOf(parser.next())
        return when (val marker = parser.next()) {
            "NOGAME" -> SaveData(difficulty, game = null, history = emptyList())
            "GAME" -> {
                val game = parser.state()
                val historyHeader = parser.next().split(" ")
                check(historyHeader.size == 2 && historyHeader[0] == "HISTORY")
                val count = historyHeader[1].toInt()
                val history = List(count) { parser.state() }
                SaveData(difficulty, game, history)
            }
            else -> error("marcador $marker")
        }
    }

    private fun appendState(out: StringBuilder, state: GameState) {
        out.appendLine(state.difficulty.name)
        out.appendLine(state.score)
        out.appendLine(state.moves)
        out.appendLine(state.won)
        out.appendLine("FINISHED ${state.finished.size}")
        state.finished.forEach { out.appendLine(it.name) }
        out.appendLine("STOCK ${state.stock.size}")
        state.stock.forEach { appendCard(out, it) }
        state.columns.forEach { column ->
            out.appendLine("COL ${column.size}")
            column.forEach { appendCard(out, it) }
        }
    }

    private fun appendCard(out: StringBuilder, card: Card) {
        out.appendLine("${card.id} ${card.suit.name} ${card.rank.name} ${card.faceUp}")
    }

    private class Parser(text: String) {
        private val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        private var index = 0

        fun next(): String = lines[index++]

        fun state(): GameState {
            val difficulty = Difficulty.valueOf(next())
            val score = next().toInt()
            val moves = next().toInt()
            val won = next().toBoolean()
            val finishedHeader = next().split(" ")
            check(finishedHeader[0] == "FINISHED")
            val finished = List(finishedHeader[1].toInt()) { Suit.valueOf(next()) }
            val stockHeader = next().split(" ")
            check(stockHeader[0] == "STOCK")
            val stock = List(stockHeader[1].toInt()) { card() }
            val columns = List(SpiderRules.COLUMN_COUNT) {
                val header = next().split(" ")
                check(header[0] == "COL")
                List(header[1].toInt()) { card() }
            }
            return GameState(
                difficulty = difficulty,
                columns = columns,
                stock = stock,
                finished = finished,
                score = score,
                moves = moves,
                won = won,
            )
        }

        private fun card(): Card {
            val parts = next().split(" ")
            check(parts.size == 4)
            return Card(
                id = parts[0].toInt(),
                suit = Suit.valueOf(parts[1]),
                rank = Rank.valueOf(parts[2]),
                faceUp = parts[3].toBoolean(),
            )
        }
    }
}
