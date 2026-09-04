package com.mach.apps.repostinho.data.model

import kotlinx.serialization.Serializable

/** Quem avisar. O parentesco entra porque "ligar para a mãe" é mais rápido que um nome. */
@Serializable
data class EmergencyContact(
    val name: String,
    val relationship: String? = null,
    val phone: String? = null
)

/**
 * A ficha médica de um morador.
 *
 * Ela existe para a emergência, e é isso que decide o resto do desenho: quem está do lado
 * precisa do tipo sanguíneo, da alergia e do telefone de quem avisar **naquele minuto**,
 * sem ligar para ninguém perguntando. Por isso a ficha é visível para a rep inteira e não
 * só para o dono — uma ficha que só o dono enxerga é inútil justamente quando serve.
 *
 * Os campos espelham as perguntas do formulário que a rep já respondeu, um a um. Texto
 * livre onde a pergunta é aberta: "Camarão, gato, cachorro, ácaros e poeira" e "Rinite
 * alérgica e dermatite atópica" são respostas de gente, e quebrá-las em lista por vírgula
 * separaria "Rinite alérgica e dermatite atópica" em lugar nenhum.
 *
 * O que o formulário tem e aqui não: CPF, RG, telefone e email pessoal. Documento não
 * salva ninguém numa emergência, e esta ficha fica visível para catorze pessoas e em
 * cache nos aparelhos delas.
 */
@Serializable
data class MedicalRecord(
    /** "O+", "AB-". Texto, e não enum, porque é o que está escrito na carteirinha. */
    val bloodType: String? = null,
    /** Alergia a medicamentos — a que muda o que o hospital pode dar. */
    val drugAllergies: String? = null,
    /** Alimentar ou a picadas. */
    val otherAllergies: String? = null,
    val conditions: String? = null,
    /** De uso contínuo. */
    val medications: String? = null,
    /** Onde ele guarda os remédios: quem for buscar precisa saber para onde ir. */
    val medicationLocation: String? = null,
    /** Na ordem em que a pessoa listou — o primeiro é quem ela quer que chamem antes. */
    val contacts: List<EmergencyContact> = emptyList(),
    val notes: String? = null
) {
    /**
     * Ficha sem nada dentro.
     *
     * "Não preencheu" e "preencheu dizendo que não tem nada" são coisas diferentes, e a
     * segunda continua sendo ficha: quem respondeu "nenhuma" para alergia respondeu.
     */
    val isEmpty: Boolean
        get() = bloodType.isNullOrBlank() &&
            drugAllergies.isNullOrBlank() &&
            otherAllergies.isNullOrBlank() &&
            conditions.isNullOrBlank() &&
            medications.isNullOrBlank() &&
            medicationLocation.isNullOrBlank() &&
            contacts.isEmpty() &&
            notes.isNullOrBlank()
}
