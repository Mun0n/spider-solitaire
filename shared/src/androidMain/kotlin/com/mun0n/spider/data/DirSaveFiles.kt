package com.mun0n.spider.data

import java.io.File
import java.io.IOException

class DirSaveFiles(private val dir: File) : SaveFiles {
    override fun read(name: String): String? {
        val file = File(dir, name)
        if (!file.exists()) return null
        return file.readText()
    }

    override fun write(name: String, text: String) {
        val target = File(dir, name)
        try {
            val tmp = File(target.parentFile, "${target.name}.tmp")
            tmp.writeText(text)
            if (target.exists() && !target.delete()) {
                target.writeText(text)
                tmp.delete()
                return
            }
            if (!tmp.renameTo(target)) {
                target.writeText(text)
                tmp.delete()
            }
        } catch (_: IOException) {
            // La partida en memoria sigue. El próximo movimiento reintenta.
        }
    }
}
