package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.model.Resident
import com.mach.apps.repostinho.data.model.RoomType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementação temporária em memória, enquanto o projeto no Firebase não existe.
 * Os dados somem quando o app é fechado.
 */
class InMemoryResidentRepository : ResidentRepository {

    private val residents = MutableStateFlow(SEED)
    private var nextId = SEED.size + 1

    override fun getResidents(): Flow<List<Resident>> = residents.asStateFlow()

    override suspend fun saveResident(resident: Resident) {
        residents.value = if (resident.id.isBlank()) {
            residents.value + resident.copy(id = "morador-${nextId++}")
        } else {
            residents.value.map { if (it.id == resident.id) resident else it }
        }
    }

    /**
     * Remover um morador não apaga o histórico: ele só sai dos rateios futuros, senão os
     * lançamentos antigos em que ele aparece ficariam com o saldo torto.
     */
    override suspend fun removeResident(residentId: String) {
        residents.value = residents.value.map {
            if (it.id == residentId) it.copy(isActive = false) else it
        }
    }

    companion object {
        /** Quem está usando o app enquanto não existe login. */
        const val CURRENT_USER_ID = "vk"

        val SEED = listOf(
            // Individual
            Resident("leozinho", "Leozinho", RoomType.INDIVIDUAL),
            Resident("pico", "Pico", RoomType.INDIVIDUAL),
            Resident("ll", "LL", RoomType.INDIVIDUAL),
            // Duplo maior
            Resident("du", "Du", RoomType.DUPLO_MAIOR),
            Resident("peter", "Peter", RoomType.DUPLO_MAIOR),
            Resident(CURRENT_USER_ID, "VK", RoomType.DUPLO_MAIOR,
                birthDate = "20/02/2001", joinedAt = "28/03/2026"),
            Resident("mais-novo", "Mais Novo", RoomType.DUPLO_MAIOR),
            // Duplo menor
            Resident("gab", "Gab", RoomType.DUPLO_MENOR),
            Resident("gustavo", "Gustavo", RoomType.DUPLO_MENOR),
            // Triplo maior
            Resident("cansado", "Cansado", RoomType.TRIPLO_MAIOR),
            Resident("mixas", "Mixas", RoomType.TRIPLO_MAIOR),
            Resident("tz", "TZ", RoomType.TRIPLO_MAIOR),
            // Triplo menor
            Resident("lameu", "Lameu", RoomType.TRIPLO_MENOR),
            // Saiu da rep: fica no cadastro como inativo porque os lançamentos antigos do
            // banco têm o nome dele, e sumir com ele deixaria aqueles rateios sem dono.
            Resident("picasso", "Picasso", RoomType.TRIPLO_MENOR, isActive = false),
            Resident("prazer", "Prazer", RoomType.TRIPLO_MENOR)
        )
    }
}
