package com.mach.apps.repostinho.data.local

import com.mach.apps.repostinho.data.remote.BankSheetPayload
import kotlinx.serialization.json.Json

/**
 * Guarda a última resposta boa do `banco-api` em disco.
 *
 * É o que a tela mostra quando a busca falha. Antes existia um retrato do banco embutido
 * no código, que envelhecia para sempre: passada uma semana, ele exibia saldos que já não
 * eram de ninguém. O último dado que o app realmente viu é sempre mais honesto.
 */
class BankSheetCache(private val store: TextFileStore) {

    private val json = Json { ignoreUnknownKeys = true }

    fun read(): BankSheetPayload? {
        val raw = store.read(FILE_NAME) ?: return null
        // Cache corrompido ou de um formato antigo é o mesmo que não ter cache.
        return runCatching { json.decodeFromString<BankSheetPayload>(raw) }.getOrNull()
    }

    fun write(payload: BankSheetPayload) {
        runCatching {
            store.write(FILE_NAME, json.encodeToString(BankSheetPayload.serializer(), payload))
        }
    }

    private companion object {
        const val FILE_NAME = "banco.json"
    }
}
