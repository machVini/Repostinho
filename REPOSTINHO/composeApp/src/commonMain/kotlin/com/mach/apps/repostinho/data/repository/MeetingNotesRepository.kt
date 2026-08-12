package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.local.MeetingNotesCache
import com.mach.apps.repostinho.data.model.MeetingNotes
import com.mach.apps.repostinho.data.remote.BankApi
import com.mach.apps.repostinho.data.remote.BankApiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface MeetingNotesRepository {
    fun getNotes(): Flow<MeetingNotes>

    /** Não lança: sem rede, fica com o que estiver em cache. */
    suspend fun refresh(fresh: Boolean = false)
}

/**
 * Busca a lista de atas no `banco-api` e guarda a última resposta em disco.
 *
 * Mesma ideia do banco: a pasta muda pouco, e o cache evita que o card apareça vazio
 * numa abertura sem rede. Aqui o cache é ainda mais confortável — os links do Drive não
 * expiram, então uma lista velha continua funcionando.
 */
class RemoteMeetingNotesRepository(
    private val api: BankApi,
    private val cache: MeetingNotesCache
) : MeetingNotesRepository {

    private val notes = MutableStateFlow(MeetingNotes())

    init {
        cache.read()?.let { notes.value = it }
    }

    override fun getNotes(): Flow<MeetingNotes> = notes.asStateFlow()

    override suspend fun refresh(fresh: Boolean) {
        if (!BankApiConfig.isConfigured) return

        try {
            val fetched = api.fetchMeetingNotes(fresh)
            // Pasta vazia é resposta legítima, mas apagar o cache por causa dela deixaria
            // o card em branco se o Drive respondesse errado por um momento.
            if (fetched.files.isEmpty() && notes.value.files.isNotEmpty()) return

            notes.value = fetched
            cache.write(fetched)
        } catch (_: Exception) {
            // O card já mostra o cache; um erro de rede aqui não muda a tela.
        }
    }
}
