package com.mach.apps.repostinho.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Resident(
    val id: String = "",
    val name: String,
    val roomType: RoomType = RoomType.INDIVIDUAL,
    val isModerator: Boolean = false, // Para os dois que controlam o financeiro
    val isActive: Boolean = true, // Ex-morador continua no histórico, mas sai dos rateios
    /**
     * Dia e mês do aniversário. O ano é opcional e não aparece em lugar nenhum.
     *
     * É daqui que o Calendário monta os aniversários: enquanto eles eram eventos escritos
     * à mão, a mesma data existia em dois lugares e a segunda ficaria velha na primeira
     * troca de morador.
     */
    val birthDay: Int? = null,
    val birthMonth: Int? = null,
    /** "28/03/2026" — texto porque só é exibido, nunca comparado. */
    val joinedAt: String? = null,
    /**
     * URL da foto. Vazio cai no monograma com a inicial do nome.
     *
     * URL, e não imagem embutida: foto no binário obriga a publicar versão nova do app
     * toda vez que alguém troca a sua.
     */
    val photoUrl: String? = null
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
