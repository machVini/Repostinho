package com.mach.apps.repostinho.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.browser.document

/**
 * Publica a cor em `--bar-color`, no elemento raiz.
 *
 * Vai por variável, e não pintando o fundo direto, para o CSS continuar dono do palpite
 * inicial: até o Wasm terminar de baixar não existe tema escolhido, e a variável ainda não
 * está definida — aí valem os valores por `prefers-color-scheme` do index.html.
 *
 * A `meta[theme-color]` vem junto porque é dela que o sistema tira a cor da barra de status
 * no PWA instalado; deixá-la fixa faria a barra divergir do app depois de trocar o tema.
 */
actual fun paintBrowserChrome(color: Color) {
    val css = color.toCssHex()
    // `setAttribute` em vez de `style.backgroundColor`: o tipo de `documentElement` é
    // `Element`, e converter para `HTMLElement` aqui não vale o casting.
    document.documentElement?.setAttribute("style", "--bar-color: $css")
    document.querySelector("meta[name=\"theme-color\"]")?.setAttribute("content", css)
}

/** O alfa é descartado: as faixas são chapadas, e as cores das barras são opacas. */
private fun Color.toCssHex(): String =
    "#" + (toArgb() and 0xFFFFFF).toString(16).padStart(6, '0')
