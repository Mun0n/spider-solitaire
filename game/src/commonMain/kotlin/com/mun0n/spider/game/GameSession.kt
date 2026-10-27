package com.mun0n.spider.game

class GameSession(
    initial: GameState,
    history: List<GameState> = emptyList(),
) {
    var state: GameState = initial
        private set

    private val history = ArrayDeque<GameState>(history.size.coerceAtLeast(16))

    init {
        this.history.addAll(history.takeLast(HISTORY_LIMIT))
    }

    val canUndo: Boolean get() = history.isNotEmpty()

    fun history(): List<GameState> = history.toList()

    fun undo(): Boolean {
        val previous = history.removeLastOrNull() ?: return false
        state = previous
        return true
    }

    fun move(fromCol: Int, fromIndex: Int, toCol: Int): ActionResult? {
        val result = SpiderRules.move(state, fromCol, fromIndex, toCol) ?: return null
        push(result)
        return result
    }

    fun deal(): ActionResult? {
        val result = SpiderRules.deal(state) ?: return null
        push(result)
        return result
    }

    private fun push(result: ActionResult) {
        history.addLast(state)
        while (history.size > HISTORY_LIMIT) history.removeFirst()
        state = result.state
    }

    companion object {
        const val HISTORY_LIMIT = 200
    }
}
