package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.model.RepDate
import com.mach.apps.repostinho.data.model.RepEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface EventRepository {
    fun getEvents(): Flow<List<RepEvent>>
}

/**
 * Agenda fixa da rep. Ainda não há cadastro de evento pela tela, então as datas ficam aqui,
 * na mesma ideia dos outros repositórios em memória.
 *
 * O [YEAR] é fixo: quando virar o ano, as datas precisam ser atualizadas à mão.
 */
class InMemoryEventRepository : EventRepository {

    private val events = MutableStateFlow(SEED)

    override fun getEvents(): Flow<List<RepEvent>> = events.asStateFlow()

    private companion object {
        const val YEAR = 2026

        val SEED = listOf(
            RepEvent("lei-do-retorno", "Lei do Retorno", RepDate(15, 8, YEAR)),
            RepEvent("alcorridas", "Alcorridas", RepDate(29, 8, YEAR)),
            RepEvent(
                id = "interreps",
                name = "InterReps",
                start = RepDate(19, 11, YEAR),
                end = RepDate(22, 11, YEAR)
            )
        )
    }
}
