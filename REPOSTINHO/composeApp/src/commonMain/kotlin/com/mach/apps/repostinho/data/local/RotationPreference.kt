package com.mach.apps.repostinho.data.local

import com.mach.apps.repostinho.data.model.ChoreRotation
import com.mach.apps.repostinho.data.model.RotationState
import kotlinx.datetime.LocalDate

/**
 * Guarda o estado do rodízio em disco.
 *
 * Duas linhas de texto, no estilo do [ThemePreferenceStore]: a âncora em ISO e a semana
 * congelada, ou `-` quando o rodízio está andando. Legível a olho nu — quando a escala
 * aparecer errada, este arquivo é a primeira coisa a olhar.
 *
 * ```
 * 2026-08-12
 * -
 * ```
 */
class RotationPreferenceStore(private val store: TextFileStore) {

    fun read(): RotationState {
        val lines = store.read(FILE_NAME)?.trim()?.lines() ?: return DEFAULT

        // Arquivo truncado ou gravado por uma versão futura volta ao padrão: uma âncora
        // ilegível reembaralharia a escala inteira, o que é pior do que ignorar o arquivo.
        val anchor = lines.getOrNull(0)?.trim()
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return DEFAULT

        val paused = lines.getOrNull(1)?.trim()
            ?.takeIf { it != RUNNING }
            ?.toIntOrNull()

        return RotationState(anchor = anchor, pausedAtWeek = paused)
    }

    fun write(state: RotationState) {
        store.write(
            FILE_NAME,
            "${state.anchor}\n${state.pausedAtWeek?.toString() ?: RUNNING}"
        )
    }

    private companion object {
        const val FILE_NAME = "rodizio.txt"
        const val RUNNING = "-"
        val DEFAULT = RotationState(anchor = ChoreRotation.DEFAULT_ANCHOR)
    }
}
