package com.mach.apps.repostinho

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.mach.apps.repostinho.di.initKoin
import kotlinx.browser.document

/** No Kotlin/Wasm o `println` sai no console do navegador. */
private fun log(message: String) {
    println("[repostinho] $message")
}

/**
 * Entrada do app na web.
 *
 * O "diretório" de cache aqui é só um prefixo de chave no `localStorage` — no navegador
 * não existe caminho de arquivo, mas a assinatura é comum às três plataformas.
 *
 * Nota de depuração: o Compose só anexa o canvas no primeiro frame. Numa aba oculta o
 * `requestAnimationFrame` não dispara, e a tela fica vazia sem erro nenhum — o que parece
 * app quebrado, mas é só a aba não estar em primeiro plano.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    log("main() iniciou")
    try {
        initKoin(cacheDirectory = "repostinho")

        val target = document.getElementById("composeTarget") ?: document.body!!
        ComposeViewport(target) {
            App()
        }
        log("montado em ${target.tagName}")
    } catch (e: Throwable) {
        log("FALHOU: ${e::class.simpleName} -> ${e.message}")
        throw e
    }
}
