package com.mach.apps.repostinho.data.remote

import com.mach.apps.repostinho.data.model.CaixinhaLine
import com.mach.apps.repostinho.data.model.MemberBalance
import com.mach.apps.repostinho.data.model.Movement
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * O que o `banco-api` devolve.
 *
 * Os nomes dos campos são os das `data class` do app de propósito: o JSON desserializa
 * direto nos modelos, sem camada de DTO no meio. Renomear um campo aqui exige mexer no
 * Worker junto.
 */
@Serializable
data class BankSheetPayload(
    val generatedAt: String,
    /**
     * "12/08, 01:21" no fuso de Campinas, já pronto para a tela.
     *
     * Quem formata é o Worker: converter UTC para horário local no Kotlin/Native custaria
     * uma biblioteca de datas inteira. Tem default porque cache gravado antes deste campo
     * existir precisa continuar legível.
     */
    val generatedAtLabel: String = "",
    val balances: List<MemberBalance>,
    val movements: List<Movement>,
    val caixinha: List<CaixinhaLine>
)

class BankApi(private val client: HttpClient) {

    /** Lança em caso de falha; quem chama decide o que fazer sem rede. */
    suspend fun fetchSheet(): BankSheetPayload {
        check(BankApiConfig.isConfigured) {
            "bancoApi.baseUrl ausente no local.properties"
        }

        val response = client.get("${BankApiConfig.BASE_URL.trimEnd('/')}/banco") {
            header("x-rep-token", BankApiConfig.TOKEN)
        }
        if (!response.status.isSuccess()) {
            error("banco-api respondeu ${response.status.value}")
        }
        return response.body()
    }

    companion object {
        fun defaultClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                // A planilha ganha colunas com alguma frequência; campo novo no JSON não
                // pode derrubar o app.
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
}
