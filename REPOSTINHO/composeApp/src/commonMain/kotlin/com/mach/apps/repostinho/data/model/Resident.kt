package com.mach.apps.repostinho.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Resident(
    val id: String = "",
    val name: String,
    val roomType: RoomType = RoomType.INDIVIDUAL,
    val isModerator: Boolean = false, // Para os dois que controlam o financeiro
    val isActive: Boolean = true, // Ex-morador continua no histórico, mas sai dos rateios
    // TODO: virar data de verdade quando entrar kotlinx-datetime no projeto.
    val birthDate: String? = null,
    val joinedAt: String? = null
)

/** O quarto define o peso do morador no rateio do aluguel + contas fixas. */
@Serializable
enum class RoomType(val label: String) {
    INDIVIDUAL("Individual"),
    DUPLO_MAIOR("Duplo maior"),
    DUPLO_MENOR("Duplo menor"),
    TRIPLO_MAIOR("Triplo maior"),
    TRIPLO_MENOR("Triplo menor")
}
