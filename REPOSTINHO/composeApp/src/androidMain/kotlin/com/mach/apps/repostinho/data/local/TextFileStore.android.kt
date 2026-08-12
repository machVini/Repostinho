package com.mach.apps.repostinho.data.local

import java.io.File

actual fun textFileStore(directory: String): TextFileStore = AndroidTextFileStore(directory)

private class AndroidTextFileStore(private val directory: String) : TextFileStore {

    override fun read(name: String): String? = runCatching {
        File(directory, name).takeIf { it.exists() }?.readText()
    }.getOrNull()

    override fun write(name: String, content: String) {
        runCatching { File(directory, name).writeText(content) }
    }
}
