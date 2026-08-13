package com.mach.apps.repostinho.data.remote

import com.mach.apps.repostinho.data.model.MovementType

/**
 * O lançamento como o morador o preencheu na tela, antes de virar URL.
 *
 * [weights] tem só quem entrou no rateio; quem ficou de fora não vira parâmetro, e o campo
 * correspondente no Forms fica vazio — que é como um não-participante é representado lá.
 */
data class LancamentoDraft(
    val description: String,
    val type: MovementType,
    val payer: String,
    val valueCents: Long,
    val weights: Map<String, Double> = emptyMap()
)

/**
 * Monta o link do formulário do banco já preenchido.
 *
 * O app não envia o lançamento: ele preenche o Forms e deixa o morador conferir e apertar
 * Enviar. Isso mantém a planilha como única fonte da verdade — o lançamento entra pelo
 * mesmo caminho de sempre, com as macros e as fórmulas dela — e evita duas coisas caras:
 * credencial de escrita no OneDrive dentro do app, e um endpoint de submit não documentado
 * que a Microsoft muda sem avisar.
 *
 * Os ids das perguntas vieram do "obter link pré-preenchido" do próprio Forms. Eles são
 * fixos por pergunta: mexer na ordem das perguntas não os muda, mas **apagar e recriar uma
 * pergunta muda**, e aí o campo dela passa a chegar vazio, em silêncio.
 */
object LancamentoForm {

    private const val BASE =
        "https://forms.cloud.microsoft/Pages/ResponsePage.aspx?id=" +
            "M9Ks01QtHUCMw9hthhS3xZNzZpgflGFMvfqZZV8r9FJURUI1MTJWU1UySUdEOUJaNFg1SE5KUVIxWC4u"

    private const val ID_DESCRICAO = "r89fbc3d2957040c09229b5c51cea0355"
    private const val ID_TIPO = "r8bc71ccda77e4fe0a465845ab840ed4c"
    private const val ID_PAGADOR = "rafb04a89e3504863b63637dba9c629be"
    private const val ID_VALOR = "r9b2227b288ee4255a06b1a10b19c5aa1"

    /**
     * O campo de peso de cada pessoa.
     *
     * A chave é o nome como ele aparece na planilha, porque é assim que o rateio chega no
     * app. A ordem aqui é a das perguntas no Forms e não bate com a das colunas da
     * planilha — por isso o casamento é por nome, nunca por posição.
     */
    private val PESO_POR_NOME = mapOf(
        "Lameu" to "ra0f41ddbb5a74481b1a524c194f3aae8",
        "Leozin" to "rdd840dc1b7aa42f19f3c484b79bfe943",
        "Pico" to "r29d261ed7e314031aac85a5a01181378",
        "LL" to "rc50bec08986c4db5bea65460bbafbc3f",
        "Du" to "r78d3972ffc39407bbb60f323197c9969",
        "Michel" to "r74492f58b1544071b463ce987b06c344",
        "Peter" to "ra3f070d688ad473fb38e702dee2fbf0e",
        "Gab" to "r46c1faa3e4f74cd0b53646e01ce2d505",
        "Gu" to "r99dd0d7c33494ef8ad3e2139789d8c2b",
        "Benê" to "ref8dacdb9112407d9e85bbc3998d3a16",
        "Calopsita" to "r2f393c832a6a44da8f34bae1c436d4d3",
        "Mixirica" to "r9643d401de0f49b3b53fc7b7bcfefc82",
        "Massa" to "rc638baa5e0474fbd8ade6c35de39e6d3",
        "VK" to "rb2813f7ffd7246d68e7c71db42a4bde8",
        "Cansado" to "rccb75234036545a99359a4446abdde3d",
        "Prazer" to "r954fa3eedbbe4516bc34c62846aa6d4f",
        "Lucas" to "r3d63b2fbc2bb4065a5f2b3589310bc59",
        "Anhê" to "r37259d34d41d41a2935f5155feeac09a",
        "Nicole" to "rca96fb22cf2045229934c128a6e1a406",
        "Anaju" to "r5be26284dba14def951b13e40144c206",
        "Gui" to "rea6bd1d0440345f2bdf22e709ab2ad72",
        "Key" to "r6a8126ab54aa4fbf9b96a07fdea0d915",
        "Raquel" to "rbb0a674fb1a84c29917f1cd605b9de90",
        "Picasso" to "rf1c2abce70304ebfb8c01d9361abb17b",
        "Carlos" to "r4040d640c1f4423a9ef60011ec15986c",
        "Helena" to "rf6a2b38c2f1d49c0a604e6c65b42de00"
    )

    /** Quem o Forms conhece. A tela usa isto para não oferecer alguém sem campo lá. */
    val nomesConhecidos: Set<String> get() = PESO_POR_NOME.keys

    /**
     * Os caixas da rep, que aparecem como pagador mas não entram em rateio.
     *
     * Não são pessoas e não têm campo de peso: quando a caixinha paga, o dinheiro sai dela
     * e é rateado entre quem participou. Os rótulos precisam bater exatamente com as
     * opções do Forms, senão a escolha chega em branco.
     */
    val caixas = listOf(
        "Caix. Déb/PIX",
        "Caix. Crédito",
        "Caix. Dinheiro",
        "Ext. (PIX)",
        "Ext. (Dinheiro)"
    )

    /** Todas as opções de pagador, na ordem em que o Forms as lista. */
    val pagadores: List<String>
        get() = caixas.take(3) + PESO_POR_NOME.keys + caixas.drop(3)

    /**
     * Nomes que a planilha tem e o Forms não.
     *
     * Um peso para alguém daqui não teria onde ser preenchido, e o lançamento sairia
     * errado sem avisar ninguém. A tela precisa barrar antes, não depois.
     */
    fun semCampoNoForms(participantes: List<String>): List<String> =
        participantes.filterNot { it in PESO_POR_NOME }

    fun urlFor(draft: LancamentoDraft): String {
        val params = mutableListOf<String>()

        params += "$ID_DESCRICAO=${encode(draft.description)}"
        // Escolha vai entre aspas; texto e número, não. É o formato que o próprio Forms
        // gera no link pré-preenchido.
        params += "$ID_TIPO=${encode("\"${label(draft.type)}\"")}"
        params += "$ID_PAGADOR=${encode("\"${draft.payer}\"")}"
        params += "$ID_VALOR=${encode(money(draft.valueCents))}"

        // Peso zero não vira parâmetro: no Forms, não participar é o campo vazio.
        draft.weights.forEach { (name, weight) ->
            val id = PESO_POR_NOME[name]
            if (id != null && weight != 0.0) params += "$id=${encode(number(weight))}"
        }

        return "$BASE&${params.joinToString("&")}"
    }

    /** O rótulo exato da opção no Forms; um texto diferente chega como escolha em branco. */
    private fun label(type: MovementType): String = when (type) {
        MovementType.PRIVADO -> "Privado"
        MovementType.COLETIVO -> "Coletivo"
        MovementType.SAIDA -> "Saída"
        MovementType.ENTRADA -> "Entrada"
    }

    /** O formulário pede ponto como separador decimal, e o app guarda centavos. */
    private fun money(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val abs = if (cents < 0) -cents else cents
        return "$sign${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
    }

    /** Peso inteiro sai sem casas: "1" em vez de "1.0", que é o que se digita à mão. */
    private fun number(value: Double): String {
        val rounded = (value * 100).toLong()
        if (rounded % 100L == 0L) return (rounded / 100L).toString()
        return "${rounded / 100L}.${(rounded % 100L).toString().padStart(2, '0').trimEnd('0')}"
    }

    /**
     * Percent-encoding feito à mão: o Kotlin/Native não tem `URLEncoder`.
     *
     * Codifica byte a byte em UTF-8 porque os nomes têm acento (Benê, Anhê) e o Forms
     * espera UTF-8 percent-encoded — mandar o byte cru quebra a escolha.
     */
    private fun encode(value: String): String = buildString {
        for (byte in value.encodeToByteArray()) {
            val b = byte.toInt() and 0xFF
            val c = b.toChar()
            if (c.isLetterOrDigit() && b < 0x80 || c in "-_.~") {
                append(c)
            } else {
                append('%').append(HEX[b shr 4]).append(HEX[b and 0x0F])
            }
        }
    }

    private const val HEX = "0123456789ABCDEF"
}
