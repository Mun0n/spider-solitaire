package com.mun0n.spider.ui

import com.mun0n.spider.game.Card

object ColumnLayout {
    fun tops(cards: List<Card>, cardH: Float, maxH: Float): FloatArray {
        val count = cards.size
        if (count == 0) return FloatArray(0)
        val steps = FloatArray(count)
        for (index in 1 until count) {
            steps[index] = if (cards[index - 1].faceUp) cardH * FACE_UP_STEP else cardH * FACE_DOWN_STEP
        }
        val natural = cardH + steps.sum()
        if (natural > maxH && count > 1) {
            val available = (maxH - cardH).coerceAtLeast(1f)
            val scale = available / steps.sum()
            for (index in 1 until count) steps[index] *= scale
        }
        val tops = FloatArray(count)
        for (index in 1 until count) tops[index] = tops[index - 1] + steps[index]
        return tops
    }

    private const val FACE_UP_STEP = 0.33f
    private const val FACE_DOWN_STEP = 0.14f
}
