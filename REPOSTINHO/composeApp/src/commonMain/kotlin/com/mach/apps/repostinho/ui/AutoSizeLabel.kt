package com.mach.apps.repostinho.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Rótulo de item de navegação que encolhe em vez de quebrar a linha.
 *
 * Abas e itens do menu dividem a largura da tela em partes iguais, então a palavra mais
 * longa manda. Num Galaxy A34 (360dp) com a fonte do sistema ampliada, "Lançamentos" e
 * "Calendário" quebravam e jogavam as últimas letras para uma segunda linha.
 *
 * `softWrap = false` impede a quebra, e o [TextAutoSize] reduz o corpo até caber — melhor
 * do que reticências, que esconderiam justamente o fim da palavra.
 *
 * Passe um [modifier] que limite a largura (`fillMaxWidth`) quando o pai medir o rótulo
 * sem restrição: sem um teto, não há o que caber, e o texto sai cortado em vez de menor.
 */
@Composable
fun AutoSizeLabel(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleSmall,
    minFontSize: TextUnit = 9.sp
) {
    BasicText(
        text = text,
        modifier = modifier,
        maxLines = 1,
        softWrap = false,
        style = style.copy(color = LocalContentColor.current, textAlign = TextAlign.Center),
        autoSize = TextAutoSize.StepBased(
            minFontSize = minFontSize,
            maxFontSize = style.fontSize
        )
    )
}
