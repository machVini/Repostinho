package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.model.ChoreTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ChoreRepository {
    fun getTasks(): Flow<List<ChoreTask>>
    suspend fun setDone(taskId: String, done: Boolean)
}

/**
 * Escala mockada da semana. O rodízio automático ainda não existe: as duplas estão fixas
 * aqui e não giram sozinhas.
 */
class InMemoryChoreRepository : ChoreRepository {

    private val tasks = MutableStateFlow(SEED)

    override fun getTasks(): Flow<List<ChoreTask>> = tasks.asStateFlow()

    override suspend fun setDone(taskId: String, done: Boolean) {
        tasks.value = tasks.value.map {
            if (it.id == taskId) it.copy(done = done) else it
        }
    }

    private companion object {
        val SEED = listOf(
            ChoreTask("panos", "Panos", listOf("cansado", "peter")),
            ChoreTask("louca", "Louça", listOf("mixas", "ll")),
            ChoreTask("sala", "Sala", listOf("vk", "mais-novo")),
            ChoreTask("cozinha", "Cozinha", listOf("pico", "du")),
            ChoreTask("area-externa", "Área Externa", listOf("gab", "gustavo")),
            ChoreTask("lixo", "Lixo", listOf("lameu", "leozinho")),
            ChoreTask("folga", "Folga", listOf("tz", "prazer", "picasso"), isRest = true)
        )
    }
}
