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
    /**
     * Mês e ano em que entrou na rep — "Março de 2026".
     *
     * Dois inteiros em vez de texto pronto pelo mesmo motivo do aniversário: texto entra
     * escrito de cinco jeitos ("03/2026", "mar/26", "Março"), e aí ordenar ou comparar
     * vira adivinhação. O dia não entra porque ninguém lembra.
     */
    val joinedMonth: Int? = null,
    val joinedYear: Int? = null,
    /**
     * O email com que ele entra no app.
     *
     * É o que liga a conta de autenticação ao morador: sem isso, saber que alguém provou
     * ter um email não diz de quem é o saldo nem qual é a tarefa da semana. Vazio quer
     * dizer que a pessoa ainda não pode entrar.
     */
    val email: String? = null,
    /**
     * O nome dele **na planilha**, quando difere do nome no app.
     *
     * A planilha usa os apelidos dela ("Gu", "Leozin", "Mixirica") e é ela que fecha os
     * saldos. Sem este vínculo explícito, o app procura a coluna pelo nome que exibe, não
     * acha, e a pessoa vê a tela como se não devesse nada — que é o pior jeito de errar
     * numa tela de dinheiro.
     */
    val sheetName: String? = null,
    /**
     * URL da foto. Vazio cai no monograma com a inicial do nome.
     *
     * URL, e não imagem embutida: foto no binário obriga a publicar versão nova do app
     * toda vez que alguém troca a sua.
     */
    val photoUrl: String? = null
) {
    /** Como procurá-lo nas colunas da planilha. */
    val bankName: String get() = sheetName?.takeIf { it.isNotBlank() } ?: name
}

/** O quarto define o peso do morador no rateio do aluguel + contas fixas. */
@Serializable
enum class RoomType(val label: String) {
    INDIVIDUAL("Individual"),
    DUPLO_MAIOR("Duplo maior"),
    DUPLO_MENOR("Duplo menor"),
    TRIPLO_MAIOR("Triplo maior"),
    TRIPLO_MENOR("Triplo menor")
}
