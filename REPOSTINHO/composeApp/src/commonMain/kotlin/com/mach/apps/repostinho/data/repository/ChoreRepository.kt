package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.local.RotationPreferenceStore
import com.mach.apps.repostinho.data.model.Chore
import com.mach.apps.repostinho.data.model.ChoreGroup
import com.mach.apps.repostinho.data.model.ChoreRotation
import com.mach.apps.repostinho.data.model.ChoreTask
import com.mach.apps.repostinho.data.remote.BankApi
import com.mach.apps.repostinho.data.remote.BankApiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Em que pé está o rodízio, para a tela poder dizer. */
data class RotationStatus(
    val week: Int,
    /** "12 a 18 de agosto" — o intervalo desta semana, já pronto para a tela. */
    val rangeLabel: String,
    val isPaused: Boolean,
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

    /** Congela a escala como está — férias, recesso, semana de prova. */
    suspend fun setPaused(paused: Boolean)

    /** Recalcula a semana e rebusca o que a rep já marcou. */
    suspend fun refresh()
}

/**
 * A escala da semana, calculada a partir da data.
 *
 * As tarefas e os grupos são fixos no código: o app roda o rodízio, mas ainda não edita
 * quem está em cada dupla. O que ele não guarda é a escala da semana — ela sai de
 * [ChoreRotation.assign] toda vez, então nunca fica velha em disco.
 *
 * As marcações de feito, ao contrário, são da rep: moram no `banco-api` para que marcar a
 * louça num celular apareça no dos outros.
 */
@OptIn(ExperimentalTime::class)
class RotatingChoreRepository(
    private val preferences: RotationPreferenceStore,
    private val api: BankApi,
    /** Injetável para o teste não depender do relógio da máquina. */
    private val today: () -> LocalDate = { Clock.System.todayIn(CAMPINAS) }
) : ChoreRepository {

    private val tasks = MutableStateFlow<List<ChoreTask>>(emptyList())
    private val status = MutableStateFlow(
        RotationStatus(week = 0, rangeLabel = "", isPaused = false)
    )

    /**
     * O que está marcado na semana corrente.
     *
     * Espelha o que o Worker devolveu por último. Sem rede ele fica com o que o morador
     * marcou nesta sessão, para a caixinha não desmarcar sozinha na frente dele.
     */
    private var doneWeek: Int? = null
    private var doneIds = emptySet<String>()
    private var shared = false

    init {
        // A escala não depende de rede: ela aparece inteira antes de qualquer chamada.
        recompute()
    }

    override fun getTasks(): Flow<List<ChoreTask>> = tasks.asStateFlow()

    override fun getRotationStatus(): Flow<RotationStatus> = status.asStateFlow()

    override suspend fun setDone(taskId: String, done: Boolean) {
        val week = currentWeek()

        // A caixa responde ao toque na hora; a rede vem depois. Esperar a resposta para
        // pintar o check deixaria o gesto travado por segundos numa rede ruim.
        doneIds = if (done) doneIds + taskId else doneIds - taskId
        publish(week)

        if (!BankApiConfig.isConfigured) return
        try {
            val payload = api.setChoreDone(week, taskId, done)
            adopt(payload.week, payload.doneChoreIds, shared = true)
        } catch (e: Exception) {
            // Marcação perdida na rede não pode sumir da tela: ela fica local e a próxima
            // sincronização resolve. O rótulo avisa que ninguém mais está vendo isto.
            shared = false
            publish(week)
        }
    }

    override suspend fun setPaused(paused: Boolean) {
        val now = today()
        val current = preferences.read()
        val updated = if (paused) {
            ChoreRotation.pause(current, now)
        } else {
            ChoreRotation.resume(current, now)
        }
        preferences.write(updated)
        refresh()
    }

    override suspend fun refresh() {
        recompute()
        if (!BankApiConfig.isConfigured) return

        val week = currentWeek()
        try {
            val payload = api.fetchChoreDone(week)
            adopt(payload.week, payload.doneChoreIds, shared = true)
        } catch (e: Exception) {
            // Sem rede a escala continua de pé — ela é calculada, não buscada. Só as
            // marcações dos outros é que não chegam.
            shared = false
            publish(week)
        }
    }

    private fun currentWeek(): Int =
        ChoreRotation.weekIndex(preferences.read(), today())

    private fun adopt(week: Int, ids: List<String>, shared: Boolean) {
        // Resposta de outra semana significa que a virada aconteceu no meio do caminho;
        // adotá-la marcaria a escala nova com o que foi feito na antiga.
        if (week != currentWeek()) return
        doneWeek = week
        doneIds = ids.toSet()
        this.shared = shared
        publish(week)
    }

    private fun recompute() {
        val week = currentWeek()

        // Virou a quarta: o que estava marcado era da escala anterior.
        if (doneWeek != week) {
            doneWeek = week
            doneIds = emptySet()
        }
        publish(week)
    }

    private fun publish(week: Int) {
        val now = today()
        tasks.value = ChoreRotation.assign(CHORES, GROUPS, week, doneIds)
        status.value = RotationStatus(
            week = week,
            rangeLabel = ChoreRotation.weekRangeLabel(now),
            isPaused = preferences.read().isPaused,
            isShared = shared
        )
    }

    companion object {
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
         * Os grupos na posição da semana da âncora (12/08/2026) — é o que faz a escala
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
