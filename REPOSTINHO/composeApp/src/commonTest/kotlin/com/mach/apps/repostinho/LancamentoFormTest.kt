package com.mach.apps.repostinho

import com.mach.apps.repostinho.data.model.MovementType
import com.mach.apps.repostinho.data.remote.LancamentoDraft
import com.mach.apps.repostinho.data.remote.LancamentoForm
import com.mach.apps.repostinho.presentation.parseBrlToCents
import com.mach.apps.repostinho.presentation.parseWeight
import com.mach.apps.repostinho.presentation.sortedByNome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun urlOf(
    description: String = "Pizza",
    type: MovementType = MovementType.PRIVADO,
    payer: String = "VK",
    valueCents: Long = 8990,
    weights: Map<String, Double> = emptyMap()
): String = LancamentoForm.urlFor(
    LancamentoDraft(description, type, payer, valueCents, weights)
)

class LancamentoFormTest {

    @Test
    fun oValorSaiComPontoDecimal() {
        // O formulário pede ponto, e o app guarda centavos.
        assertTrue(urlOf(valueCents = 8990).contains("=89.90"))
        assertTrue(urlOf(valueCents = 500).contains("=5.00"))
        assertTrue(urlOf(valueCents = 123456).contains("=1234.56"))
    }

    @Test
    fun aEscolhaVaiEntreAspas() {
        // Formato do próprio Forms: escolha entre aspas, texto e número sem.
        assertTrue(urlOf(type = MovementType.PRIVADO).contains("%22Privado%22"))
        assertTrue(urlOf(payer = "VK").contains("%22VK%22"))
    }

    @Test
    fun oAcentoViraUtf8PercentEncoded() {
        // "Saída" e os nomes com acento quebrariam a escolha se fossem mandados crus.
        assertTrue(urlOf(type = MovementType.SAIDA).contains("Sa%C3%ADda"))
        assertTrue(urlOf(weights = mapOf("Benê" to 1.0)).contains("=1"))
    }

    @Test
    fun aDescricaoEscapaEspacoEAcento() {
        val url = urlOf(description = "Racha da pizzação")
        assertTrue(url.contains("Racha%20da%20pizza%C3%A7%C3%A3o"))
    }

    @Test
    fun quemNaoEntrouNoRateioNaoViraParametro() {
        // No Forms, não participar é o campo vazio — mandar zero marcaria a pessoa.
        val url = urlOf(weights = mapOf("VK" to 1.0, "Lameu" to 0.0))

        assertTrue(url.contains("rb2813f7ffd7246d68e7c71db42a4bde8=1"))
        assertFalse(url.contains("ra0f41ddbb5a74481b1a524c194f3aae8"))
    }

    @Test
    fun pesoInteiroSaiSemCasas() {
        assertTrue(urlOf(weights = mapOf("VK" to 1.0)).endsWith("=1"))
        assertTrue(urlOf(weights = mapOf("VK" to 0.5)).endsWith("=0.5"))
    }

    @Test
    fun cadaUmVaiComOSeuPeso() {
        // O caso que motivou o campo por pessoa: rateio desigual no mesmo lançamento.
        val url = urlOf(weights = mapOf("VK" to 2.0, "Lameu" to 1.0, "Pico" to 0.5))

        assertTrue(url.contains("rb2813f7ffd7246d68e7c71db42a4bde8=2"), "VK com peso 2")
        assertTrue(url.contains("ra0f41ddbb5a74481b1a524c194f3aae8=1"), "Lameu com peso 1")
        assertTrue(url.contains("r29d261ed7e314031aac85a5a01181378=0.5"), "Pico com peso 0,5")
    }

    @Test
    fun pesoQuebradoNaoPerdeUmCentesimo() {
        // 0.29 * 100 dá 28,999999999999996 em ponto flutuante: truncar virava 0.28, e peso
        // errado é rateio errado.
        assertTrue(urlOf(weights = mapOf("VK" to 0.29)).endsWith("=0.29"))
        assertTrue(urlOf(weights = mapOf("VK" to 0.88)).endsWith("=0.88"))
    }

    @Test
    fun oValorAceitaVirgulaEPonto() {
        // O teclado brasileiro oferece vírgula; o formulário só entende ponto.
        assertEquals(2250L, parseBrlToCents("22,50"))
        assertEquals(2250L, parseBrlToCents("22.50"))
        assertEquals(123456L, parseBrlToCents("1.234,56"))

        // E os três chegam ao formulário no mesmo formato.
        assertEquals("22.50", LancamentoForm.formatValor(parseBrlToCents("22,50")!!))
        assertEquals("1234.56", LancamentoForm.formatValor(parseBrlToCents("1.234,56")!!))
    }

    @Test
    fun oPesoAceitaVirgulaEPonto() {
        assertEquals(0.5, parseWeight("0,5"))
        assertEquals(0.5, parseWeight("0.5"))
        assertEquals(2.0, parseWeight("2"))
    }

    @Test
    fun pesoInvalidoOuZeradoNaoPassa() {
        // Meio de digitação ("0,") e zero não podem virar participante.
        assertEquals(null, parseWeight("0,"))
        assertEquals(null, parseWeight(""))
        assertEquals(null, parseWeight("abc"))
        assertEquals(null, parseWeight("0"))
    }

    @Test
    fun aDicaDaTelaEhOMesmoNumeroDaUrl() {
        // A tela mostra "Vai como X"; se X não for o que entra na URL, a pessoa confere um
        // número e a rep recebe outro.
        val cents = parseBrlToCents("89,9")!!
        assertTrue(urlOf(valueCents = cents).contains("=${LancamentoForm.formatValor(cents)}"))
    }

    @Test
    fun nomeDesconhecidoNaoViraParametroSolto() {
        // Alguém que a planilha tem e o Forms não: não pode virar um id inventado.
        val url = urlOf(weights = mapOf("Fulano" to 1.0))
        assertFalse(url.contains("Fulano"))
    }

    @Test
    fun aCalopsitaEhOCasoConhecidoDeDivergencia() {
        // Ela tem campo no Forms mas não tem coluna na planilha; o inverso é o que
        // machucaria, e por isso a tela consulta esta lista antes de oferecer o nome.
        val daPlanilha = listOf("VK", "Lameu", "Fulano de Tal")

        assertEquals(listOf("Fulano de Tal"), LancamentoForm.semCampoNoForms(daPlanilha))
        assertTrue("Calopsita" in LancamentoForm.nomesConhecidos)
    }

    @Test
    fun aOrdemAlfabeticaIgnoraCaixaEAcento() {
        // "LL" é maiúsculo e viria antes de "Lameu" numa comparação por code point;
        // "Anhê" tem acento e cairia depois de "Zé" se ele estivesse na primeira letra.
        assertEquals(
            listOf("Anaju", "Anhê", "Ávila", "Lameu", "LL", "Zé"),
            listOf("LL", "Zé", "Anhê", "Lameu", "Ávila", "Anaju").sortedByNome()
        )
    }

    @Test
    fun aUrlComeçaNoFormularioCerto() {
        assertTrue(urlOf().startsWith("https://forms.cloud.microsoft/Pages/ResponsePage.aspx?id="))
    }

    @Test
    fun umColetivoComTodosCabeNaUrl() {
        // 26 pesos mais os quatro campos fixos: precisa continuar abaixo do limite prático
        // de ~2000 caracteres que navegadores e o iOS aceitam.
        val todos = LancamentoForm.nomesConhecidos.associateWith { 1.0 }
        val url = urlOf(description = "Mercado do mês", weights = todos)

        assertTrue(url.length < 2000, "URL ficou com ${url.length} caracteres")
    }
}
