package com.mach.apps.repostinho.data.local

/**
 * Quem está logado, entre aberturas do app.
 *
 * Guarda o **id do morador**, e não o email nem token nenhum: o provedor de autenticação
 * já mantém a sessão dele em disco, e duplicar credencial aqui só criaria um segundo lugar
 * para vazar. Isto é a resposta para "de quem é o saldo que eu mostro", que o app precisa
 * saber antes mesmo de a rede responder.
 *
 * Um arquivo com uma palavra, no estilo do [ThemePreferenceStore] — dá para ler a olho nu
 * quando o app abrir como a pessoa errada.
 */
class SessionStore(private val store: TextFileStore) {

    fun read(): String? = store.read(FILE_NAME)?.trim()?.takeIf { it.isNotEmpty() }

    fun write(residentId: String) {
        store.write(FILE_NAME, residentId)
    }

    fun clear() {
        store.write(FILE_NAME, "")
    }

    private companion object {
        const val FILE_NAME = "sessao.txt"
    }
}
