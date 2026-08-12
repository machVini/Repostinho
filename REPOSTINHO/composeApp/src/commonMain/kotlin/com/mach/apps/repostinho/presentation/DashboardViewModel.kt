package com.mach.apps.repostinho.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mach.apps.repostinho.data.model.CaixinhaLine
import com.mach.apps.repostinho.data.model.ChoreTask
import com.mach.apps.repostinho.data.model.MeetingNotes
import com.mach.apps.repostinho.data.model.MemberBalance
import com.mach.apps.repostinho.data.model.Movement
import com.mach.apps.repostinho.data.model.RepEvent
import com.mach.apps.repostinho.data.model.Resident
import com.mach.apps.repostinho.data.repository.BankSheetRepository
import com.mach.apps.repostinho.data.repository.ChoreRepository
import com.mach.apps.repostinho.data.repository.EventRepository
import com.mach.apps.repostinho.data.repository.InMemoryResidentRepository
import com.mach.apps.repostinho.data.repository.MeetingNotesRepository
import com.mach.apps.repostinho.data.repository.ResidentRepository
import com.mach.apps.repostinho.data.repository.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BankUiState(
    val residents: List<Resident> = emptyList(),
    /** Quem está usando o app. Fixo no VK enquanto não existe login. */
    val currentResidentId: String = InMemoryResidentRepository.CURRENT_USER_ID
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
    val syncState: SyncState = SyncState.Loading
) {
    /** Move o indicador do "puxar para atualizar". */
    val isRefreshing: Boolean get() = syncState is SyncState.Loading

    val activeMembers: List<MemberBalance> get() = balances.filter { !it.isFormer }

    val myBalanceCents: Long?
        get() = balances.firstOrNull { it.name == CURRENT_MEMBER_NAME }?.finalCents

    /** Soma do que a rep tem a receber de quem está devendo, em centavos. */
    val totalOwedCents: Long
        get() = balances.filter { it.finalCents < 0 }.sumOf { -it.finalCents }

    val caixinhaTotalCents: Long?
        get() = caixinha.firstOrNull { it.isTotal }?.finalCents

    companion object {
        /** O nome como ele aparece nas colunas da planilha, não um id do app. */
        const val CURRENT_MEMBER_NAME = "VK"
    }
}

class DashboardViewModel(
    private val residentRepository: ResidentRepository,
    private val choreRepository: ChoreRepository,
    private val eventRepository: EventRepository,
    private val bankSheetRepository: BankSheetRepository,
    private val meetingNotesRepository: MeetingNotesRepository
) : ViewModel() {

    private val currentResidentId =
        MutableStateFlow(InMemoryResidentRepository.CURRENT_USER_ID)

    val uiState: StateFlow<BankUiState> = combine(
        residentRepository.getResidents(),
        currentResidentId
    ) { residents, currentId ->
        BankUiState(residents = residents, currentResidentId = currentId)
    }
        .catch { emit(BankUiState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BankUiState())

    val sheet: StateFlow<SheetUiState> = combine(
        bankSheetRepository.getBalances(),
        bankSheetRepository.getMovements(),
        bankSheetRepository.getCaixinha(),
        bankSheetRepository.getSyncState()
    ) { balances, movements, caixinha, syncState ->
        SheetUiState(
            balances = balances,
            movements = movements,
            caixinha = caixinha,
            syncState = syncState
        )
    }
        .catch { emit(SheetUiState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SheetUiState())

    val tasks: StateFlow<List<ChoreTask>> = choreRepository.getTasks()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val events: StateFlow<List<RepEvent>> = eventRepository.getEvents()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val meetingNotes: StateFlow<MeetingNotes> = meetingNotesRepository.getNotes()
        .catch { emit(MeetingNotes()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MeetingNotes())

    init {
        // Uma busca por abertura do app; durante a sessão, quem decide é o morador,
        // puxando a lista para baixo.
        refreshSheet()
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
    }

    /** Só a tarefa do próprio morador pode ser marcada; as outras são consulta. */
    fun setTaskDone(taskId: String, done: Boolean) {
        val task = tasks.value.firstOrNull { it.id == taskId } ?: return
        if (task.isRest || currentResidentId.value !in task.assigneeIds) return

        viewModelScope.launch { choreRepository.setDone(taskId, done) }
    }
}
