package com.mach.apps.repostinho.data.local

/**
 * Guarda a última escala conhecida em disco.
 *
 * A escala não é mais calculada da data: ela é um número que mora no `banco-api` e só
 * anda quando alguém roda. Isso significa que, sem rede, o app não tem como descobri-la
 * sozinho — e abrir a tela de tarefas sem escala nenhuma seria pior do que mostrar a
 * última que se viu, dizendo de onde ela veio.
 *
 * Duas linhas de texto, no estilo das outras preferências: a semana e quando ela foi
 * virada, ou `-` quando isso nunca aconteceu. Legível a olho nu — quando a escala
 * aparecer errada, este arquivo é a primeira coisa a olhar.
 *
 * ```
 * 8
 * 2026-10-16T03:40:00.000Z
 * ```
 */
class RotationCache(private val store: TextFileStore) {

    fun read(): CachedRotation? {
        val lines = store.read(FILE_NAME)?.trim()?.lines() ?: return null
        val week = lines.getOrNull(0)?.trim()?.toIntOrNull() ?: return null
        val rotatedAt = lines.getOrNull(1)?.trim()?.takeIf { it != NUNCA }
        return CachedRotation(week = week, rotatedAt = rotatedAt)
    }

    fun write(rotation: CachedRotation) {
        store.write(FILE_NAME, "${rotation.week}\n${rotation.rotatedAt ?: NUNCA}")
    }

    private companion object {
        const val FILE_NAME = "rodizio.txt"
        const val NUNCA = "-"
    }
}

/** A escala como ela foi vista pela última vez. */
data class CachedRotation(val week: Int, val rotatedAt: String? = null)
