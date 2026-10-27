package com.mun0n.spider.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordsTest {
    @Test
    fun firstWinIsARecord() {
        val (records, isRecord) = Records().consider(Difficulty.ONE, 812)
        assertTrue(isRecord)
        assertEquals(812, records.best[Difficulty.ONE])
    }

    @Test
    fun higherScoreReplacesTheRecord() {
        val current = Records(mapOf(Difficulty.TWO to 640))
        val (records, isRecord) = current.consider(Difficulty.TWO, 701)
        assertTrue(isRecord)
        assertEquals(701, records.best[Difficulty.TWO])
    }

    @Test
    fun equalOrLowerScoreKeepsTheRecord() {
        val current = Records(mapOf(Difficulty.FOUR to 500))
        val (same, sameIsRecord) = current.consider(Difficulty.FOUR, 500)
        assertFalse(sameIsRecord)
        assertEquals(current, same)
        val (lower, lowerIsRecord) = current.consider(Difficulty.FOUR, 499)
        assertFalse(lowerIsRecord)
        assertEquals(current, lower)
    }

    @Test
    fun oneDifficultyDoesNotTouchTheOthers() {
        val current = Records(mapOf(Difficulty.ONE to 900, Difficulty.FOUR to 400))
        val (records, isRecord) = current.consider(Difficulty.TWO, 100)
        assertTrue(isRecord)
        assertEquals(900, records.best[Difficulty.ONE])
        assertEquals(100, records.best[Difficulty.TWO])
        assertEquals(400, records.best[Difficulty.FOUR])
    }

    @Test
    fun codecRoundTrip() {
        val records = Records(mapOf(Difficulty.ONE to 812, Difficulty.FOUR to 640))
        assertEquals(records, RecordsCodec.decode(RecordsCodec.encode(records)))
    }

    @Test
    fun missingDifficultyIsNoRecord() {
        val text = """
            SPIDER_RECORDS1
            ONE 812
            FOUR 640
        """.trimIndent()
        val records = RecordsCodec.decode(text)
        assertEquals(812, records.best[Difficulty.ONE])
        assertNull(records.best[Difficulty.TWO])
        assertEquals(640, records.best[Difficulty.FOUR])
    }

    @Test
    fun brokenFileIsEmpty() {
        assertEquals(Records(), RecordsCodec.decode(""))
        assertEquals(Records(), RecordsCodec.decode("basura"))
        assertEquals(Records(), RecordsCodec.decode("SPIDER_RECORDS1\nONE nope"))
        assertEquals(Records(), RecordsCodec.decode("SPIDER_RECORDS1\nNOPE 10"))
    }
}
