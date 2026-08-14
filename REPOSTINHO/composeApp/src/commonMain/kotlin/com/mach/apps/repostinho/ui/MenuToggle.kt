package com.mach.apps.repostinho.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Altura máxima de um menu suspenso: acima disso ele rola em vez de tomar a tela. */
val MenuMaxHeight = 280.dp

/**
 * Menu que fecha quando se toca de novo no botão que o abriu.
 *
 * Parece um `!expanded` e não é. O toque fora que fecha o popup também chega no botão,
 * então o par "fecha por fora" + "inverte no clique" reabre o menu no mesmo gesto e ele
 * parece nunca fechar. A janela curta abaixo ignora o clique que só desfez a abertura.
 */
@Stable
class MenuToggle {

    var expanded by mutableStateOf(false)
        private set

    private var closedAt: TimeMark? = null

    /** Chamado pelo botão âncora. */
    fun onAnchorClick() {
        val justClosed = closedAt?.elapsedNow()?.let { it < REOPEN_GUARD } == true
        if (!justClosed) expanded = !expanded
    }

    /** Chamado pelo `onDismissRequest` do menu. */
    fun dismiss() {
        expanded = false
        closedAt = TimeSource.Monotonic.markNow()
    }

    fun select() = dismiss()

    private companion object {
        /**
         * Tempo entre o popup se fechar e o clique chegar no botão.
         *
         * Generoso de propósito: errar para mais só custa um toque perdido em quem abre e
         * fecha muito rápido; errar para menos traz o bug de volta.
         */
        val REOPEN_GUARD = 250.milliseconds
    }
}

@Composable
fun rememberMenuToggle(): MenuToggle = remember { MenuToggle() }
