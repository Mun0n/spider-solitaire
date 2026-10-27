package com.mun0n.spider.game

data class Records(
    val best: Map<Difficulty, Int> = emptyMap(),
) {
    fun consider(difficulty: Difficulty, score: Int): Pair<Records, Boolean> {
        val previous = best[difficulty]
        if (previous != null && score <= previous) return this to false
        return copy(best = best + (difficulty to score)) to true
    }
}

object RecordsCodec {
    fun encode(records: Records): String = buildString {
        appendLine("SPIDER_RECORDS1")
        Difficulty.entries.forEach { difficulty ->
            records.best[difficulty]?.let { score ->
                appendLine("${difficulty.name} $score")
            }
        }
    }

    fun decode(text: String): Records {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.isEmpty() || lines.first() != "SPIDER_RECORDS1") return Records()
        val best = mutableMapOf<Difficulty, Int>()
        for (line in lines.drop(1)) {
            val parts = line.split(" ")
            if (parts.size != 2) return Records()
            val difficulty = runCatching { Difficulty.valueOf(parts[0]) }.getOrNull() ?: return Records()
            val score = parts[1].toIntOrNull() ?: return Records()
            best[difficulty] = score
        }
        return Records(best)
    }
}
