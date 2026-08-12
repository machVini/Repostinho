package com.mach.apps.repostinho.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mach.apps.repostinho.data.model.BankSettings
import com.mach.apps.repostinho.data.model.CaixinhaLine
import com.mach.apps.repostinho.data.model.ChoreTask
import com.mach.apps.repostinho.data.model.MemberBalance
import com.mach.apps.repostinho.data.model.Movement
import com.mach.apps.repostinho.data.model.RepEvent
import com.mach.apps.repostinho.data.model.Resident
import com.mach.apps.repostinho.data.repository.BankSettingsRepository
import com.mach.apps.repostinho.data.repository.BankSheetRepository
import com.mach.apps.repostinho.data.repository.ChoreRepository
import com.mach.apps.repostinho.data.repository.EventRepository
import com.mach.apps.repostinho.data.repository.InMemoryResidentRepository
import com.mach.apps.repostinho.data.repository.ResidentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BankUiState(
    val residents: List<Resident> = emptyList(),
    val settings: BankSettings = BankSettings(),
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
    val caixinha: List<CaixinhaLine> = emptyList()
) {
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
    private val settingsRepository: BankSettingsRepository,
    private val choreRepository: ChoreRepository,
    private val eventRepository: EventRepository,
    private val bankSheetRepository: BankSheetRepository
) : ViewModel() {

    private val currentResidentId =
        MutableStateFlow(InMemoryResidentRepository.CURRENT_USER_ID)

    val uiState: StateFlow<BankUiState> = combine(
        residentRepository.getResidents(),
        settingsRepository.getSettings(),
        currentResidentId
    ) { residents, settings, currentId ->
        BankUiState(
            residents = residents,
            settings = settings,
            currentResidentId = currentId
        )
    }
        .catch { emit(BankUiState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BankUiState())

    val sheet: StateFlow<SheetUiState> = combine(
        bankSheetRepository.getBalances(),
        bankSheetRepository.getMovements(),
        bankSheetRepository.getCaixinha()
    ) { balances, movements, caixinha ->
        SheetUiState(balances = balances, movements = movements, caixinha = caixinha)
    }
        .catch { emit(SheetUiState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SheetUiState())

    val tasks: StateFlow<List<ChoreTask>> = choreRepository.getTasks()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val events: StateFlow<List<RepEvent>> = eventRepository.getEvents()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Só a tarefa do próprio morador pode ser marcada; as outras são consulta. */
    fun setTaskDone(taskId: String, done: Boolean) {
        val task = tasks.value.firstOrNull { it.id == taskId } ?: return
        if (task.isRest || currentResidentId.value !in task.assigneeIds) return

        viewModelScope.launch { choreRepository.setDone(taskId, done) }
    }
}
