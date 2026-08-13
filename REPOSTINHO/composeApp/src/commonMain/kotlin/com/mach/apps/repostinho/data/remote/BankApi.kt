package com.mach.apps.repostinho.data.remote

import com.mach.apps.repostinho.data.model.CaixinhaLine
import com.mach.apps.repostinho.data.model.MeetingNotes
import com.mach.apps.repostinho.data.model.MemberBalance
import com.mach.apps.repostinho.data.model.Movement
import com.mach.apps.repostinho.data.model.RepEvent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
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

/**
 * As tarefas que a rep já marcou como feitas nesta semana.
 *
 * Quem conta a semana é o app: a regra do rodízio mora no Kotlin, e ter o Worker
 * recalculando o mesmo índice criaria duas versões da mesma conta para discordarem.
 */
@Serializable
data class ChoreDonePayload(
    val week: Int,
    val doneChoreIds: List<String> = emptyList()
)

@Serializable
private data class ChoreDoneRequest(
    val week: Int,
    val choreId: String,
    val done: Boolean
)

/** Os eventos que a rep cadastrou pelo app — a agenda fixa não passa por aqui. */
@Serializable
data class EventsPayload(
    val events: List<RepEvent> = emptyList()
)

@Serializable
private data class AddEventRequest(val event: RepEvent)

@Serializable
private data class RemoveEventRequest(val remove: String)

class BankApi(private val client: HttpClient) {

    /**
     * Lança em caso de falha; quem chama decide o que fazer sem rede.
     *
     * [fresh] pede ao banco-api que ignore o cache de borda. É o que o "puxar para
     * atualizar" usa: sem isso, puxar dentro da janela de cache devolvia o payload
     * anterior, com o mesmo horário, e parecia não ter feito nada.
     */
    suspend fun fetchSheet(fresh: Boolean = false): BankSheetPayload =
        get("banco", if (fresh) "fresh=1" else "")

    /** As últimas atas na pasta do Drive. Lança em caso de falha. */
    suspend fun fetchMeetingNotes(fresh: Boolean = false): MeetingNotes =
        get("atas", if (fresh) "fresh=1" else "")

    /** O que já está marcado nesta semana. Lança em caso de falha. */
    suspend fun fetchChoreDone(week: Int): ChoreDonePayload = get("tarefas", "semana=$week")

    /**
     * Marca ou desmarca uma tarefa para a rep inteira.
     *
     * Devolve a lista já atualizada pelo Worker, e não só um "ok": se alguém marcou outra
     * tarefa entre a leitura e este toque, a resposta já traz as duas.
     */
    suspend fun setChoreDone(week: Int, choreId: String, done: Boolean): ChoreDonePayload =
        post("tarefas", ChoreDoneRequest(week = week, choreId = choreId, done = done))

    /** A agenda cadastrada pela rep. Lança em caso de falha. */
    suspend fun fetchEvents(): EventsPayload = get("eventos")

    /** Cadastra (ou corrige, se o id já existir) um evento para todos. */
    suspend fun addEvent(event: RepEvent): EventsPayload =
        post("eventos", AddEventRequest(event))

    /** Apaga um evento da agenda de todos. Só vale para os cadastrados pela tela. */
    suspend fun removeEvent(eventId: String): EventsPayload =
        post("eventos", RemoveEventRequest(eventId))

    private suspend inline fun <reified B, reified T> post(path: String, body: B): T {
        check(BankApiConfig.isConfigured) {
            "bancoApi.baseUrl ausente no local.properties"
        }

        val response = client.post("${BankApiConfig.BASE_URL.trimEnd('/')}/$path") {
            header("x-rep-token", BankApiConfig.TOKEN)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        if (!response.status.isSuccess()) {
            error("banco-api respondeu ${response.status.value} em /$path")
        }
        return response.body()
    }

    private suspend inline fun <reified T> get(path: String, query: String = ""): T {
        check(BankApiConfig.isConfigured) {
            "bancoApi.baseUrl ausente no local.properties"
        }

        val suffix = if (query.isBlank()) "" else "?$query"
        val response = client.get("${BankApiConfig.BASE_URL.trimEnd('/')}/$path$suffix") {
            header("x-rep-token", BankApiConfig.TOKEN)
        }
        if (!response.status.isSuccess()) {
            error("banco-api respondeu ${response.status.value} em /$path")
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
