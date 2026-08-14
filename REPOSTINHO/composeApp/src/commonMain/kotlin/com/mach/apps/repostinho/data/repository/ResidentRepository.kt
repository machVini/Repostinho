package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.local.ResidentsCache
import com.mach.apps.repostinho.data.model.Resident
import com.mach.apps.repostinho.data.model.RoomType
import com.mach.apps.repostinho.data.remote.BankApi
import com.mach.apps.repostinho.data.remote.BankApiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ResidentRepository {
    fun getResidents(): Flow<List<Resident>>
    suspend fun refresh()
}

/**
 * Os moradores da rep, vindos do `banco-api`.
 *
 * A lista embutida abaixo continua existindo para a primeira abertura sem rede — sem
 * morador nenhum o app não teria nome, nem escala, nem saldo próprio para mostrar. Mas
 * quem manda é o servidor: foto, aniversário e data de entrada mudam sem que ninguém
 * queira publicar versão nova do app por causa disso.
 */
class RemoteResidentRepository(
    private val api: BankApi,
    private val cache: ResidentsCache
) : ResidentRepository {

    private val residents = MutableStateFlow(cache.read() ?: SEED)

    override fun getResidents(): Flow<List<Resident>> = residents.asStateFlow()

    override suspend fun refresh() {
        if (!BankApiConfig.isConfigured) return
        try {
            val fetched = api.fetchResidents().residents
            // Lista vazia é sintoma de KV ainda não semeado. Adotá-la deixaria o app sem
            // moradores, o que é pior do que uma lista velha.
            if (fetched.isEmpty()) return
            residents.value = fetched
            cache.write(fetched)
        } catch (e: Exception) {
            // Fica com o cache, ou com a lista embutida. A tela não depende de rede para
            // saber quem mora na rep.
        }
    }

    companion object {
        /** Quem está usando o app enquanto não existe login. */
        const val CURRENT_USER_ID = "vk"

        /**
         * O retrato que vai no binário.
         *
         * Os aniversários vêm daqui e não de eventos escritos à mão: enquanto eram as duas
         * coisas, a mesma data existia em dois lugares para divergir na primeira troca.
         */
        val SEED = listOf(
            Resident("leozinho", "Leozinho", RoomType.INDIVIDUAL, sheetName = "Leozin", birthDay = 11, birthMonth = 1),
            Resident("pico", "Pico", RoomType.INDIVIDUAL, birthDay = 28, birthMonth = 10),
            Resident("ll", "LL", RoomType.INDIVIDUAL, birthDay = 29, birthMonth = 12),
            Resident("du", "Du", RoomType.DUPLO_MAIOR),
            Resident("peter", "Peter", RoomType.DUPLO_MAIOR, birthDay = 24, birthMonth = 2),
            Resident(
                id = CURRENT_USER_ID,
                name = "VK",
                roomType = RoomType.DUPLO_MAIOR,
                birthDay = 20,
                birthMonth = 2,
                joinedMonth = 3,
                joinedYear = 2026
            ),
            Resident("mais-novo", "Mais Novo", RoomType.DUPLO_MAIOR, sheetName = "Michel", birthDay = 17, birthMonth = 9),
            Resident("gab", "Gab", RoomType.DUPLO_MENOR, birthDay = 11, birthMonth = 1),
            Resident("gustavo", "Gustavo", RoomType.DUPLO_MENOR, sheetName = "Gu", birthDay = 22, birthMonth = 10),
            Resident("cansado", "Cansado", RoomType.TRIPLO_MAIOR, birthDay = 31, birthMonth = 8),
            Resident("mixas", "Mixas", RoomType.TRIPLO_MAIOR, sheetName = "Mixirica", birthDay = 10, birthMonth = 11),
            Resident("tz", "TZ", RoomType.TRIPLO_MAIOR, sheetName = "Massa", birthDay = 2, birthMonth = 8),
            Resident("lameu", "Lameu", RoomType.TRIPLO_MENOR, birthDay = 20, birthMonth = 11),
            // Saiu da rep: fica no cadastro porque os lançamentos antigos têm o nome dele,
            // e sumir com ele deixaria aqueles rateios sem dono.
            Resident("picasso", "Picasso", RoomType.TRIPLO_MENOR, isActive = false),
            Resident("prazer", "Prazer", RoomType.TRIPLO_MENOR, birthDay = 6, birthMonth = 3)
        )
    }
}
