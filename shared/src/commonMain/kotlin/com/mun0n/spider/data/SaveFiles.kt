package com.mun0n.spider.data

interface SaveFiles {
    fun read(name: String): String?

    fun write(name: String, text: String)
}
