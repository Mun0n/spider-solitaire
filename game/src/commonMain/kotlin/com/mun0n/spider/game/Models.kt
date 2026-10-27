package com.mun0n.spider.game

enum class Suit(val symbol: String, val red: Boolean, val spanish: String) {
    SPADES("♠", false, "picas"),
    HEARTS("♥", true, "corazones"),
    DIAMONDS("♦", true, "diamantes"),
    CLUBS("♣", false, "tréboles"),
}

enum class Rank(val value: Int, val label: String, val spanish: String) {
    ACE(1, "A", "as"),
    TWO(2, "2", "2"),
    THREE(3, "3", "3"),
    FOUR(4, "4", "4"),
    FIVE(5, "5", "5"),
    SIX(6, "6", "6"),
    SEVEN(7, "7", "7"),
    EIGHT(8, "8", "8"),
    NINE(9, "9", "9"),
    TEN(10, "10", "10"),
    JACK(11, "J", "jota"),
    QUEEN(12, "Q", "reina"),
    KING(13, "K", "rey"),
    ;

    companion object {
        fun fromValue(value: Int): Rank = entries.first { it.value == value }
    }
}

enum class Difficulty(val suitCount: Int) {
    ONE(1),
    TWO(2),
    FOUR(4),
}

data class Card(
    val id: Int,
    val suit: Suit,
    val rank: Rank,
    val faceUp: Boolean,
) {
    fun description(): String = "${suit.spanish} ${rank.spanish}"
}

data class GameState(
    val difficulty: Difficulty,
    val columns: List<List<Card>>,
    val stock: List<Card>,
    val finished: List<Suit>,
    val score: Int,
    val moves: Int,
    val won: Boolean,
) {
    init {
        require(columns.size == SpiderRules.COLUMN_COUNT) {
            "Spider tiene ${SpiderRules.COLUMN_COUNT} columnas"
        }
    }

    val completed: Int get() = finished.size
}

data class RemovedRun(
    val column: Int,
    val cards: List<Card>,
)

data class ActionResult(
    val state: GameState,
    val midColumns: List<List<Card>>,
    val removed: List<RemovedRun>,
    val dealtCards: List<Card>,
)

sealed interface Hint {
    data class MoveCards(
        val fromCol: Int,
        val fromIndex: Int,
        val toCol: Int,
    ) : Hint

    data object Deal : Hint
}

enum class DealBlock {
    EmptyColumn,
    NoCards,
}

data class Slide(
    val fromCol: Int,
    val toCol: Int,
    val cards: List<Card>,
)
