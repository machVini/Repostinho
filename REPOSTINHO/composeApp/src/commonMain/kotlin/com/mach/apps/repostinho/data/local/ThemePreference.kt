package com.mach.apps.repostinho.data.local

/**
 * Como o app decide entre claro e escuro.
 *
 * [SISTEMA] é o estado inicial e não é o mesmo que "claro": um app que nasce claro num
 * aparelho no escuro pisca branco a cada abertura. Só depois que o morador toca no botão
 * é que a escolha dele passa a valer sobre a do aparelho.
 */
enum class ThemeMode {
    SISTEMA, CLARO, ESCURO;

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SISTEMA -> systemDark
        CLARO -> false
        ESCURO -> true
    }
}

/**
 * Guarda a escolha de tema em disco.
 *
 * É um arquivo de texto com o nome do modo, e não JSON: é um valor só, e um formato que dá
 * para ler a olho nu quando algo estiver estranho.
 */
class ThemePreferenceStore(private val store: TextFileStore) {

    fun read(): ThemeMode {
        val raw = store.read(FILE_NAME)?.trim() ?: return ThemeMode.SISTEMA
        // Valor irreconhecível — arquivo truncado, ou gravado por uma versão futura —
        // volta a seguir o sistema, que é o padrão seguro.
        return ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SISTEMA
    }

    fun write(mode: ThemeMode) {
        store.write(FILE_NAME, mode.name)
    }

    private companion object {
        const val FILE_NAME = "tema.txt"
    }
}
