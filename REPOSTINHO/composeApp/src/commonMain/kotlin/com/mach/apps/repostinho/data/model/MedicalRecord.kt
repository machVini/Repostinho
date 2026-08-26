package com.mach.apps.repostinho.data.model

import kotlinx.serialization.Serializable

/**
 * A ficha médica de um morador.
 *
 * Ela existe para a emergência, e é isso que decide o resto do desenho: quem está do lado
 * precisa do tipo sanguíneo, da alergia e do telefone de quem avisar **naquele minuto**,
 * sem ligar para ninguém perguntando. Por isso a ficha é visível para a rep inteira e não
 * só para o dono — uma ficha que só o dono enxerga é inútil justamente quando serve.
 *
 * Nada aqui é obrigatório. Campo vazio a tela mostra como "—" e segue: ficha meio
 * preenchida vale mais do que ficha que ninguém preencheu por ser longa demais.
 */
@Serializable
data class MedicalRecord(
    /** "O+", "AB-". Texto, e não enum, porque é o que está escrito na carteirinha. */
    val bloodType: String? = null,
    val allergies: List<String> = emptyList(),
    /** Uso contínuo — o que a pessoa toma todo dia. */
    val medications: List<String> = emptyList(),
    /** Condições e histórico: asma, diabetes, cirurgia recente. */
    val conditions: List<String> = emptyList(),
    /** Convênio e carteirinha, como se diz na recepção do hospital. */
    val healthPlan: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    /** O que não coube nos campos acima. */
    val notes: String? = null
) {
    /**
     * Ficha sem nada dentro.
     *
     * A tela precisa distinguir "não preencheu" de "preencheu e está tudo normal": a
     * primeira pede um convite para preencher, a segunda não.
     */
    val isEmpty: Boolean
        get() = bloodType.isNullOrBlank() &&
            allergies.isEmpty() &&
            medications.isEmpty() &&
            conditions.isEmpty() &&
            healthPlan.isNullOrBlank() &&
            emergencyContactName.isNullOrBlank() &&
            emergencyContactPhone.isNullOrBlank() &&
            notes.isNullOrBlank()
}
