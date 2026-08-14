package com.mach.apps.repostinho.data.local

import kotlinx.browser.localStorage
import org.w3c.dom.get

/**
 * No navegador não há sistema de arquivos: o `localStorage` faz o papel.
 *
 * Cuidado conhecido: o Safari apaga o `localStorage` de sites pouco usados depois de uns
 * dias. Como tudo aqui é cache do `banco-api`, o custo é uma releitura — mas a sessão
 * também mora aqui, então o morador vai ter que entrar de novo.
 */
private class LocalStorageTextFileStore(
    private val prefix: String
) : TextFileStore {

    override fun read(name: String): String? = localStorage["$prefix$name"]

    override fun write(name: String, content: String) {
        localStorage.setItem("$prefix$name", content)
    }
}

actual fun textFileStore(directory: String): TextFileStore =
    LocalStorageTextFileStore(prefix = "$directory/")
