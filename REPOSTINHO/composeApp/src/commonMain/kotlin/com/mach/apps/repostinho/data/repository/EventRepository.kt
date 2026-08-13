package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.local.EventsCache
import com.mach.apps.repostinho.data.model.EventCategory
import com.mach.apps.repostinho.data.model.Recurrence
import com.mach.apps.repostinho.data.model.RepDate
import com.mach.apps.repostinho.data.model.RepEvent
import com.mach.apps.repostinho.data.remote.BankApi
import com.mach.apps.repostinho.data.remote.BankApiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface EventRepository {
    fun getEvents(): Flow<List<RepEvent>>

    /** Os eventos cadastrados pela rep chegaram do servidor, e não só do cache. */
    fun isShared(): Flow<Boolean>

    /** Cadastra um evento para todos. Devolve false se a rede não deixou. */
    suspend fun addEvent(event: RepEvent): Boolean

    /** Apaga um evento da agenda de todos. Devolve false se a rede não deixou. */
    suspend fun removeEvent(eventId: String): Boolean

    suspend fun refresh()
}

/**
 * A agenda da rep: a parte fixa do app somada à que os moradores cadastram.
 *
 * A divisão é de propósito. As datas fixas (aniversários, InterReps) valem mesmo
 * na primeira abertura sem rede e não podem ser apagadas por um toque errado; o que é
 * cadastrado pela tela mora no `banco-api` para aparecer no celular de todo mundo.
 */
class RemoteEventRepository(
    private val api: BankApi,
    private val cache: EventsCache
) : EventRepository {

    private val custom = MutableStateFlow(cache.read().orEmpty())
    private val events = MutableStateFlow(FIXED + custom.value)
    private val shared = MutableStateFlow(false)

    override fun getEvents(): Flow<List<RepEvent>> = events.asStateFlow()

    override fun isShared(): Flow<Boolean> = shared.asStateFlow()

    override suspend fun refresh() {
        if (!BankApiConfig.isConfigured) return
        try {
            publish(api.fetchEvents().events, fromServer = true)
        } catch (e: Exception) {
            // A agenda fixa continua de pé — ela não vem da rede. Só o que a rep cadastrou
            // é que fica com a última cópia conhecida.
            shared.value = false
        }
    }

    override suspend fun addEvent(event: RepEvent): Boolean = push {
        api.addEvent(event.copy(isCustom = true)).events
    }

    override suspend fun removeEvent(eventId: String): Boolean = push {
        api.removeEvent(eventId).events
    }

    /**
     * Escrita sem eco otimista: a lista só muda quando o servidor confirma.
     *
     * Ao contrário da caixinha de tarefa feita, um evento que aparecesse na agenda e
     * sumisse na sincronização seguinte faria alguém contar com uma data que a rep não
     * tem. Aqui é melhor o botão falhar visivelmente do que a lista mentir.
     */
    private suspend fun push(block: suspend () -> List<RepEvent>): Boolean {
        if (!BankApiConfig.isConfigured) return false
        return try {
            publish(block(), fromServer = true)
            true
        } catch (e: Exception) {
            shared.value = false
            false
        }
    }

    private fun publish(remote: List<RepEvent>, fromServer: Boolean) {
        custom.value = remote
        events.value = FIXED + remote
        shared.value = fromServer
        cache.write(remote)
    }

    companion object {
        /**
         * A agenda que o app já traz.
         *
         * Recorrentes não têm ano de validade: o ano em [RepDate] é só o da primeira
         * ocorrência. Era isso que obrigava a atualizar as datas à mão na virada do ano.
         */
        val FIXED = listOf(
            RepEvent(
                id = "niver-rep",
                name = "Aniversário da Rep",
                start = RepDate(3, 8, 2023),
                category = EventCategory.ANIVERSARIO,
                recurrence = Recurrence.ANUAL,
                isHighlight = true
            ),
            RepEvent(
                id = "niver-vk",
                name = "Aniversário do VK",
                start = RepDate(20, 2, 2001),
                category = EventCategory.ANIVERSARIO,
                recurrence = Recurrence.ANUAL
            ),
            RepEvent(
                id = "lei-do-retorno",
                name = "Lei do Retorno",
                start = RepDate(15, 8, 2026),
                category = EventCategory.ROLE
            ),
            RepEvent(
                id = "alcorridas",
                name = "Alcorridas",
                start = RepDate(29, 8, 2026),
                category = EventCategory.ARU
            ),
            RepEvent(
                id = "arrecadaru",
                name = "ArrecadARU",
                start = RepDate(12, 9, 2026),
                category = EventCategory.ARU
            ),
            RepEvent(
                id = "interreps",
                name = "InterReps",
                start = RepDate(19, 11, 2026),
                end = RepDate(22, 11, 2026),
                category = EventCategory.ARU
            )
        )
    }
}
