package com.mun0n.spider.data

import com.mun0n.spider.game.Difficulty
import com.mun0n.spider.game.Records
import com.mun0n.spider.game.RecordsCodec
import com.mun0n.spider.game.SaveCodec
import com.mun0n.spider.game.SaveData

class GameRepository(private val files: SaveFiles) {
    fun load(): SaveData {
        val text = files.read(SAVE_FILE) ?: return SaveData(Difficulty.ONE, game = null, history = emptyList())
        return try {
            SaveCodec.decode(text)
        } catch (_: RuntimeException) {
            SaveData(Difficulty.ONE, game = null, history = emptyList())
        }
    }

    fun save(data: SaveData) {
        files.write(SAVE_FILE, SaveCodec.encode(data))
    }

    fun loadRecords(): Records {
        val text = files.read(RECORDS_FILE) ?: return Records()
        return try {
            RecordsCodec.decode(text)
        } catch (_: RuntimeException) {
            Records()
        }
    }

    fun saveRecords(records: Records) {
        files.write(RECORDS_FILE, RecordsCodec.encode(records))
    }

    private companion object {
        const val SAVE_FILE = "spider_save.txt"
        const val RECORDS_FILE = "spider_records.txt"
    }
}
