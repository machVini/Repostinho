package com.mach.apps.repostinho.data.local

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

actual fun textFileStore(directory: String): TextFileStore = IosTextFileStore(directory)

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class IosTextFileStore(private val directory: String) : TextFileStore {

    override fun read(name: String): String? =
        NSString.stringWithContentsOfFile(path(name), NSUTF8StringEncoding, null)

    override fun write(name: String, content: String) {
        // `NSString.create` e não um cast: no Kotlin/Native `String` não é `NSString`, e
        // o cast direto compila mas estoura em runtime.
        NSString.create(string = content).writeToFile(
            path = path(name),
            // Evita meio arquivo no disco se o app morrer durante a escrita — JSON
            // truncado seria lido depois como cache corrompido.
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )
    }

    private fun path(name: String) = "$directory/$name"
}
