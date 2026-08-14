package com.mach.apps.repostinho.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mach.apps.repostinho.data.model.CaixinhaLine
import com.mach.apps.repostinho.data.model.ChoreTask
import com.mach.apps.repostinho.data.model.EventOccurrence
import com.mach.apps.repostinho.data.model.EventSchedule
import com.mach.apps.repostinho.data.model.MeetingNotes
import com.mach.apps.repostinho.data.model.MemberBalance
import com.mach.apps.repostinho.data.model.Movement
import com.mach.apps.repostinho.data.model.RepDate
import com.mach.apps.repostinho.data.model.RepEvent
import com.mach.apps.repostinho.data.model.Resident
import com.mach.apps.repostinho.data.repository.AuthRepository
import com.mach.apps.repostinho.data.repository.BankSheetRepository
import com.mach.apps.repostinho.data.repository.ChoreRepository
import com.mach.apps.repostinho.data.repository.EventRepository
import com.mach.apps.repostinho.data.repository.MeetingNotesRepository
import com.mach.apps.repostinho.data.repository.ResidentRepository
import com.mach.apps.repostinho.data.repository.RotatingChoreRepository
import com.mach.apps.repostinho.data.repository.RotationStatus
import com.mach.apps.repostinho.data.repository.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.todayIn

data class BankUiState(
    val residents: List<Resident> = emptyList(),
    /** Quem entrou no app. Vazio só entre a abertura e a leitura da sessão. */
    val currentResidentId: String = ""
) {
    val currentResident: Resident?
        get() = residents.firstOrNull { it.id == currentResidentId }
}

/**
 * O banco como a planilha o fecha.
 *
 * Nada aqui é recalculado: os saldos vêm prontos da aba `Saldos_pessoas`. O app e a planilha
 * discordarem em centavos seria pior do que não ter o app.
 */
data class SheetUiState(
    val balances: List<MemberBalance> = emptyList(),
    val movements: List<Movement> = emptyList(),
    val caixinha: List<CaixinhaLine> = emptyList(),
    val syncState: SyncState = SyncState.Loading,
    /**
     * O nome de quem está logado **como ele aparece na planilha**.
     *
     * A planilha é a fonte dos saldos e ela usa os apelidos dela ("Gu", "Leozin"). Se o
     * nome do morador no app não bater com a coluna, o saldo não é encontrado e a pessoa
     * vê a tela como se não devesse nada — por isso o casamento é por este campo, e não
     * pelo id.
     */
    val currentMemberName: String = ""
) {
    /** Move o indicador do "puxar para atualizar". */
    val isRefreshing: Boolean get() = syncState is SyncState.Loading

    val activeMembers: List<MemberBalance> get() = balances.filter { !it.isFormer }

    val myBalanceCents: Long?
        get() = balances.firstOrNull { it.name == currentMemberName }?.finalCents

    /** Soma do que a rep tem a receber de quem está devendo, em centavos. */
    val totalOwedCents: Long
        get() = balances.filter { it.finalCents < 0 }.sumOf { -it.finalCents }

    val caixinhaTotalCents: Long?
        get() = caixinha.firstOrNull { it.isTotal }?.finalCents

}

@OptIn(ExperimentalTime::class)
class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val residentRepository: ResidentRepository,
    private val choreRepository: ChoreRepository,
    private val eventRepository: EventRepository,
    private val bankSheetRepository: BankSheetRepository,
    private val meetingNotesRepository: MeetingNotesRepository
) : ViewModel() {

    val uiState: StateFlow<BankUiState> = combine(
        residentRepository.getResidents(),
        authRepository.currentResidentId()
    ) { residents, currentId ->
        BankUiState(residents = residents, currentResidentId = currentId.orEmpty())
    }
        .catch { emit(BankUiState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BankUiState())

    /** O nome do morador logado, para casar com a coluna da planilha. */
    private val uiStateNames = combine(
        residentRepository.getResidents(),
        authRepository.currentResidentId()
    ) { residents, id ->
        residents.firstOrNull { it.id == id }?.bankName.orEmpty()
    }

    val sheet: StateFlow<SheetUiState> = combine(
        bankSheetRepository.getBalances(),
        bankSheetRepository.getMovements(),
        bankSheetRepository.getCaixinha(),
        bankSheetRepository.getSyncState(),
        uiStateNames
    ) { balances, movements, caixinha, syncState, memberName ->
        SheetUiState(
            balances = balances,
            movements = movements,
            caixinha = caixinha,
            syncState = syncState,
            currentMemberName = memberName
        )
    }
        .catch { emit(SheetUiState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SheetUiState())

    val tasks: StateFlow<List<ChoreTask>> = choreRepository.getTasks()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val rotation: StateFlow<RotationStatus> = choreRepository.getRotationStatus()
        .catch { emit(RotationStatus(week = 0, rangeLabel = "", isPaused = false)) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            RotationStatus(week = 0, rangeLabel = "", isPaused = false)
        )

    /**
     * A agenda de hoje até 31 de dezembro, já com os recorrentes abertos em datas.
     *
     * Um aniversário é um cadastro só e vira a data deste ano; um evento que passou some
     * sozinho. Por isso a lista sai daqui pronta, em vez de a tela receber os eventos e
     * ter que expandi-los.
     */
    val events: StateFlow<List<EventOccurrence>> = combine(
        eventRepository.getEvents(),
        residentRepository.getResidents()
    ) { events, residents ->
        // Os aniversários saem dos moradores, não da agenda: assim trocar de morador
        // atualiza as duas coisas de uma vez.
        EventSchedule.occurrencesUntilEndOfYear(
            events + EventSchedule.birthdaysOf(residents),
            today()
        )
    }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val eventsRefreshingState = MutableStateFlow(false)

    /** Move o indicador do puxar-para-atualizar do Calendário. */
    val eventsRefreshing: StateFlow<Boolean> = eventsRefreshingState.asStateFlow()

    val eventsShared: StateFlow<Boolean> = eventRepository.isShared()
        .catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** O ano da janela da agenda: é nele que um evento cadastrado pela tela entra. */
    val currentYear: Int get() = today().year

    val meetingNotes: StateFlow<MeetingNotes> = meetingNotesRepository.getNotes()
        .catch { emit(MeetingNotes()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MeetingNotes())

    init {
        // Uma busca por abertura do app; durante a sessão, quem decide é o morador,
        // puxando a lista para baixo.
        refreshSheet()
    }

    /** Hoje no fuso da rep, como data de calendário. */
    private fun today(): RepDate {
        val date = Clock.System.todayIn(RotatingChoreRepository.CAMPINAS)
        return RepDate(day = date.day, month = date.month.ordinal + 1, year = date.year)
    }

    /**
     * Rebusca só a agenda, para o puxar-para-atualizar do Calendário.
     *
     * Separado do [refreshSheet] porque ali o gesto acontece sobre saldos e atas; aqui o
     * morador está olhando eventos, e buscar a planilha inteira só atrasaria o indicador.
     */
    fun refreshEvents() {
        viewModelScope.launch {
            eventsRefreshingState.value = true
            try {
                eventRepository.refresh()
                residentRepository.refresh()
            } finally {
                // `finally` porque o indicador girando para sempre é pior do que uma
                // agenda desatualizada: a tela ficaria travada sem nada explicando.
                eventsRefreshingState.value = false
            }
        }
    }

    /** Cadastra um evento para a rep inteira. */
    fun addEvent(event: RepEvent) {
        viewModelScope.launch { eventRepository.addEvent(event) }
    }

    /** Tira um evento da agenda de todos. Só vale para os cadastrados pela tela. */
    fun removeEvent(eventId: String) {
        viewModelScope.launch { eventRepository.removeEvent(eventId) }
    }

    /**
     * Rebusca a planilha e as atas.
     *
     * [fresh] separa o gesto do morador da busca automática de abertura: puxando, ele
     * quer o estado de agora, e o banco-api ignora o cache de borda.
     */
    fun refreshSheet(fresh: Boolean = false) {
        viewModelScope.launch { bankSheetRepository.refresh(fresh) }
        // As atas vêm no mesmo gesto: são duas listas da mesma tela, e ninguém espera
        // puxar duas vezes.
        viewModelScope.launch { meetingNotesRepository.refresh(fresh) }
        // A agenda cadastrada pela rep vem no mesmo gesto.
        viewModelScope.launch { eventRepository.refresh() }
        // Foto e aniversário vêm com os moradores, e mudam sem aviso.
        viewModelScope.launch { residentRepository.refresh() }
        // A escala não vem da rede, mas depende da data: sem recalcular aqui, um app
        // deixado aberto atravessa a quarta-feira mostrando a semana anterior.
        viewModelScope.launch { choreRepository.refresh() }
    }

    /**
     * Rebusca só a escala e o que a rep já marcou.
     *
     * A aba de Tarefas chama isto ao ser aberta: sem isso, uma tarefa marcada por outro
     * morador só apareceria na próxima abertura do app, e a tela prometeria um
     * compartilhamento que ela não estaria mostrando.
     */
    fun refreshChores() {
        viewModelScope.launch { choreRepository.refresh() }
    }

    /**
     * Congela ou destrava o rodízio. Pausado, ninguém troca de tarefa na quarta.
     *
     * Sem botão na tela por enquanto: pausar valeria só neste aparelho, e a escala parada
     * num celular enquanto gira nos outros confunde mais do que ajuda. A lógica fica
     * pronta e testada para quando a escala passar a ser compartilhada.
     */
    fun setRotationPaused(paused: Boolean) {
        viewModelScope.launch { choreRepository.setPaused(paused) }
    }

    /** Só a tarefa do próprio morador pode ser marcada; as outras são consulta. */
    fun setTaskDone(taskId: String, done: Boolean) {
        val task = tasks.value.firstOrNull { it.id == taskId } ?: return
        if (task.isRest || uiState.value.currentResidentId !in task.assigneeIds) return

        viewModelScope.launch { choreRepository.setDone(taskId, done) }
    }
}
