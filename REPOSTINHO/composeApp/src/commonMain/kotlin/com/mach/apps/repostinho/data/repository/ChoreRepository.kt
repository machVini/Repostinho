package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.local.CachedRotation
import com.mach.apps.repostinho.data.local.RotationCache
import com.mach.apps.repostinho.data.model.Chore
import com.mach.apps.repostinho.data.model.ChoreGroup
import com.mach.apps.repostinho.data.model.ChoreRotation
import com.mach.apps.repostinho.data.model.ChoreTask
import com.mach.apps.repostinho.data.remote.BankApi
import com.mach.apps.repostinho.data.remote.BankApiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.TimeZone

/** Em que pé está o rodízio, para a tela poder dizer. */
data class RotationStatus(
    val week: Int,
    /**
     * Quando a escala foi virada pela última vez, em ISO-8601, ou `null` se nunca foi.
     *
     * Vai crua para a tela, que é quem sabe escrever data em português. O repositório
     * guardar o texto pronto deixaria o formato longe de onde ele é lido.
     */
    val rotatedAt: String? = null,
    /**
     * As marcações desta tela vieram da rep, e não só deste aparelho.
     *
     * Sem rede, marcar continua funcionando na tela mas não chega em ninguém — e isso
     * precisa ficar visível, senão a pessoa acha que avisou a casa e não avisou.
     */
    val isShared: Boolean = false
)

interface ChoreRepository {
    fun getTasks(): Flow<List<ChoreTask>>
    fun getRotationStatus(): Flow<RotationStatus>
    suspend fun setDone(taskId: String, done: Boolean)

    /**
     * Vira a escala para a rep inteira — `-1` desfaz um toque errado.
     *
     * Devolve o resultado em vez de engolir a falha: virar a escala é um gesto de uma
     * pessoa só, e se não chegou no servidor ela precisa saber na hora. Marcar tarefa
     * pode ficar local e sincronizar depois; isto não.
     */
    suspend fun rotate(delta: Int): Result<Unit>

    /** Rebusca a escala que está valendo e o que a rep já marcou. */
    suspend fun refresh()
}

/**
 * A escala da semana, buscada do `banco-api`.
 *
 * As tarefas e os grupos são fixos no código: o app roda o rodízio, mas ainda não edita
 * quem está em cada dupla. O que mudou foi de onde sai o **número** da semana — antes da
 * data, agora do servidor, porque a virada passou a ser no dedo de alguém e precisa valer
 * para os quinze aparelhos ao mesmo tempo.
 *
 * O último número visto fica em disco: sem rede, a tela mostra a escala que se viu por
 * último em vez de ficar vazia.
 */
class RotatingChoreRepository(
    private val cache: RotationCache,
    private val api: BankApi
) : ChoreRepository {

    private val tasks = MutableStateFlow<List<ChoreTask>>(emptyList())
    private val status = MutableStateFlow(RotationStatus(week = 0))

    private var week = cache.read()?.week ?: 0
    private var rotatedAt = cache.read()?.rotatedAt

    /**
     * O que está marcado na semana corrente.
     *
     * Espelha o que o Worker devolveu por último. Sem rede ele fica com o que o morador
     * marcou nesta sessão, para a caixinha não desmarcar sozinha na frente dele.
     */
    private var doneIds = emptySet<String>()
    private var shared = false

    init {
        // A escala aparece antes de qualquer chamada: a do disco, enquanto a de verdade
        // não chega.
        publish()
    }

    override fun getTasks(): Flow<List<ChoreTask>> = tasks.asStateFlow()

    override fun getRotationStatus(): Flow<RotationStatus> = status.asStateFlow()

    override suspend fun setDone(taskId: String, done: Boolean) {
        // A caixa responde ao toque na hora; a rede vem depois. Esperar a resposta para
        // pintar o check deixaria o gesto travado por segundos numa rede ruim.
        doneIds = if (done) doneIds + taskId else doneIds - taskId
        publish()

        if (!BankApiConfig.isConfigured) return
        try {
            val payload = api.setChoreDone(week, taskId, done)
            adoptDone(payload.week, payload.doneChoreIds, shared = true)
        } catch (e: Exception) {
            // Marcação perdida na rede não pode sumir da tela: ela fica local e a próxima
            // sincronização resolve. O rótulo avisa que ninguém mais está vendo isto.
            shared = false
            publish()
        }
    }

    override suspend fun rotate(delta: Int): Result<Unit> {
        if (!BankApiConfig.isConfigured) {
            return Result.failure(IllegalStateException("sem banco-api configurado"))
        }
        return try {
            val payload = api.rotate(delta)
            adoptRotation(payload.week, payload.rotatedAt)
            // Escala nova começa sem nada marcado; o que a rep marcar vem na próxima
            // sincronização.
            doneIds = emptySet()
            publish()
            refreshDone()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refresh() {
        if (!BankApiConfig.isConfigured) return

        try {
            val payload = api.fetchRotation()
            adoptRotation(payload.week, payload.rotatedAt)
        } catch (e: Exception) {
            // Sem rede a escala não é recalculável — ela mora no servidor. Fica a última
            // conhecida, que é melhor do que tela vazia.
            shared = false
            publish()
            return
        }
        refreshDone()
    }

    private suspend fun refreshDone() {
        try {
            val payload = api.fetchChoreDone(week)
            adoptDone(payload.week, payload.doneChoreIds, shared = true)
        } catch (e: Exception) {
            shared = false
            publish()
        }
    }

    /**
     * Adota a escala que o servidor manda.
     *
     * Virou a escala: o que estava marcado era da anterior, e levá-lo junto marcaria a
     * louça de quem ainda nem a pegou.
     */
    private fun adoptRotation(week: Int, rotatedAt: String?) {
        if (week != this.week) doneIds = emptySet()
        this.week = week
        this.rotatedAt = rotatedAt
        cache.write(CachedRotation(week = week, rotatedAt = rotatedAt))
        publish()
    }

    private fun adoptDone(week: Int, ids: List<String>, shared: Boolean) {
        // Resposta de outra semana significa que alguém rodou no meio do caminho;
        // adotá-la marcaria a escala nova com o que foi feito na antiga.
        if (week != this.week) return
        doneIds = ids.toSet()
        this.shared = shared
        publish()
    }

    private fun publish() {
        tasks.value = ChoreRotation.assign(CHORES, GROUPS, week, doneIds)
        status.value = RotationStatus(
            week = week,
            rotatedAt = rotatedAt,
            isShared = shared
        )
    }

    companion object {
        /** O fuso da rep. O Calendário também lê daqui. */
        val CAMPINAS = TimeZone.of("America/Sao_Paulo")

        /** A ordem importa: é sobre ela que os grupos deslizam a cada semana. */
        val CHORES = listOf(
            Chore("cozinha", "Cozinha"),
            Chore("sala", "Sala e Copa"),
            Chore("lixo", "Lixo"),
            Chore("area-externa", "Área Externa"),
            Chore("louca", "Louça"),
            Chore("panos", "Panos"),
            Chore("folga", "Folga", isRest = true)
        )

        /**
         * Os grupos na posição da semana da âncora (07/08/2026) — é o que faz a escala
         * combinada na rep ser a que aparece nesta semana.
         */
        val GROUPS = listOf(
            ChoreGroup("g1", listOf("tz", "prazer")),
            ChoreGroup("g2", listOf("cansado", "peter")),
            ChoreGroup("g3", listOf("mixas", "ll")),
            ChoreGroup("g4", listOf("mais-novo", "vk")),
            ChoreGroup("g5", listOf("du", "pico")),
            ChoreGroup("g6", listOf("gab", "gustavo")),
            ChoreGroup("g7", listOf("lameu", "leozinho"))
        )
    }
}
