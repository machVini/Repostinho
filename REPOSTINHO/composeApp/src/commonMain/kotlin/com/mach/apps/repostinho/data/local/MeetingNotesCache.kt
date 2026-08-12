package com.mach.apps.repostinho.data.local

import com.mach.apps.repostinho.data.model.MeetingNotes
import kotlinx.serialization.json.Json

/** Guarda a última lista de atas em disco, para o card não abrir vazio sem rede. */
class MeetingNotesCache(private val store: TextFileStore) {

    private val json = Json { ignoreUnknownKeys = true }

    fun read(): MeetingNotes? {
        val raw = store.read(FILE_NAME) ?: return null
        return runCatching { json.decodeFromString<MeetingNotes>(raw) }.getOrNull()
    }

    fun write(notes: MeetingNotes) {
        runCatching {
            store.write(FILE_NAME, json.encodeToString(MeetingNotes.serializer(), notes))
        }
    }

    private companion object {
        const val FILE_NAME = "atas.json"
    }
}
