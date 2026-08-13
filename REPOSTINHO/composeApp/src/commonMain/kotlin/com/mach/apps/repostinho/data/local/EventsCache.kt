package com.mach.apps.repostinho.data.local

import com.mach.apps.repostinho.data.model.RepEvent
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Guarda a última agenda cadastrada pela rep em disco.
 *
 * Só os eventos adicionados pela tela passam por aqui — a agenda fixa vem no binário e não
 * precisa de cache. Sem isto, abrir o app sem rede esconderia justamente os eventos que
 * alguém cadastrou, que são os que ninguém mais sabe de cor.
 */
class EventsCache(private val store: TextFileStore) {

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(RepEvent.serializer())

    fun read(): List<RepEvent>? {
        val raw = store.read(FILE_NAME) ?: return null
        return runCatching { json.decodeFromString(serializer, raw) }.getOrNull()
    }

    fun write(events: List<RepEvent>) {
        runCatching { store.write(FILE_NAME, json.encodeToString(serializer, events)) }
    }

    private companion object {
        const val FILE_NAME = "eventos.json"
    }
}
