package com.mach.apps.repostinho.data.model

import kotlinx.serialization.Serializable

/**
 * Configuração do banco da rep, editável pelos responsáveis pelo financeiro.
 *
 * [monthlyFixedTotalCents] é o total de aluguel + contas fixas do mês, em centavos. Ele é
 * rateado entre os moradores ativos proporcionalmente ao peso do quarto de cada um
 * ([roomWeights]) — pesos são razões, então seguem em `Double`.
 */
@Serializable
data class BankSettings(
    val monthlyFixedTotalCents: Long = 0L,
    val roomWeights: Map<RoomType, Double> = DEFAULT_ROOM_WEIGHTS
) {
    fun weightOf(roomType: RoomType): Double =
        roomWeights[roomType] ?: DEFAULT_ROOM_WEIGHTS.getValue(roomType)

    companion object {
        /** Pesos reais da rep. */
        val DEFAULT_ROOM_WEIGHTS = mapOf(
            RoomType.INDIVIDUAL to 1.10,
            RoomType.DUPLO_MAIOR to 1.00,
            RoomType.DUPLO_MENOR to 0.95,
            RoomType.TRIPLO_MAIOR to 0.88,
            RoomType.TRIPLO_MENOR to 0.80
        )
    }
}
