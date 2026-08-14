package com.mach.apps.repostinho.data.local

import com.mach.apps.repostinho.data.model.Resident
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Guarda a última lista de moradores em disco.
 *
 * Sem ela, abrir sem rede cairia na lista embutida no app — que pode estar uma troca de
 * morador atrasada, e é justamente ela que define quem aparece na escala e no rateio.
 */
class ResidentsCache(private val store: TextFileStore) {

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(Resident.serializer())

    fun read(): List<Resident>? {
        val raw = store.read(FILE_NAME) ?: return null
        return runCatching { json.decodeFromString(serializer, raw) }.getOrNull()
    }

    fun write(residents: List<Resident>) {
        runCatching { store.write(FILE_NAME, json.encodeToString(serializer, residents)) }
    }

    private companion object {
        const val FILE_NAME = "moradores.json"
    }
}
