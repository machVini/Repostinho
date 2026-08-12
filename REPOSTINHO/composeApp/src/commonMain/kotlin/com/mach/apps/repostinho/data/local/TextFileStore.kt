package com.mach.apps.repostinho.data.local

/**
 * Lê e grava texto num diretório da plataforma.
 *
 * É interface com fábrica `expect`, e não `expect class`, porque classes expect/actual
 * ainda são Beta no Kotlin e o compilador avisa em cada build.
 */
interface TextFileStore {
    fun read(name: String): String?

    /** Não lança: falhar ao gravar o cache não pode derrubar a tela. */
    fun write(name: String, content: String)
}

/**
 * O diretório vem de fora porque no Android ele depende do `Context`, que só existe na
 * `Application`. É a única coisa que as duas plataformas não resolvem sozinhas.
 */
expect fun textFileStore(directory: String): TextFileStore
