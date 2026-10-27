package com.mun0n.spider.data

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

// Application Support: no se ve en la app Archivos y entra en la copia de iCloud.
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class DirSaveFiles : SaveFiles {
    private val dir: String = run {
        val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
            .first() as String
        NSFileManager.defaultManager.createDirectoryAtPath(
            base,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        base
    }

    override fun read(name: String): String? =
        NSString.stringWithContentsOfFile("$dir/$name", encoding = NSUTF8StringEncoding, error = null)

    override fun write(name: String, text: String) {
        // atomically: escribe en un temporal y lo renombra. Si falla, la partida en memoria sigue.
        NSString.create(string = text)
            .writeToFile("$dir/$name", atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }
}
