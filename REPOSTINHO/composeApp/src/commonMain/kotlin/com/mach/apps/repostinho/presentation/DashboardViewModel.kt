package com.mach.apps.repostinho.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mach.apps.repostinho.data.model.BankSettings
import com.mach.apps.repostinho.data.model.ChoreTask
import com.mach.apps.repostinho.data.model.RepEvent
import com.mach.apps.repostinho.data.model.Resident
import com.mach.apps.repostinho.data.model.Transaction
import com.mach.apps.repostinho.data.model.TransactionType
import com.mach.apps.repostinho.data.repository.BankSettingsRepository
import com.mach.apps.repostinho.data.repository.ChoreRepository
import com.mach.apps.repostinho.data.repository.EventRepository
import com.mach.apps.repostinho.data.repository.InMemoryResidentRepository
import com.mach.apps.repostinho.data.repository.ResidentRepository
import com.mach.apps.repostinho.data.repository.TransactionRepository
import com.mach.apps.repostinho.domain.BalanceCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ResidentBalance(
    val resident: Resident,
    /** Em centavos. Negativo = deve ao banco. */
    val balanceCents: Long
)

data class BankUiState(
    val residents: List<Resident> = emptyList(),
    val balances: List<ResidentBalance> = emptyList(),
    val settings: BankSettings = BankSettings(),
    val transactionCount: Int = 0,
    /** Quem está usando o app. Fixo no VK enquanto não existe login. */
    val currentResidentId: String = InMemoryResidentRepository.CURRENT_USER_ID
) {
    val activeResidents: List<Resident> get() = residents.filter { it.isActive }

    val currentResident: Resident?
        get() = residents.firstOrNull { it.id == currentResidentId }

    val currentBalanceCents: Long?
        get() = balances.firstOrNull { it.resident.id == currentResidentId }?.balanceCents

    /** Soma do que a rep tem a receber dos moradores que estão devendo, em centavos. */
    val totalOwedCents: Long
        get() = balances.filter { it.balanceCents < 0 }.sumOf { -it.balanceCents }
}

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val residentRepository: ResidentRepository,
    private val settingsRepository: BankSettingsRepository,
    private val choreRepository: ChoreRepository,
    private val eventRepository: EventRepository,
    private val calculator: BalanceCalculator
) : ViewModel() {

    private val currentResidentId =
        MutableStateFlow(InMemoryResidentRepository.CURRENT_USER_ID)

    val uiState: StateFlow<BankUiState> = combine(
        transactionRepository.getTransactions(),
        residentRepository.getResidents(),
        settingsRepository.getSettings(),
        currentResidentId
    ) { transactions, residents, settings, currentId ->
        BankUiState(
            residents = residents,
            balances = residents.filter { it.isActive }.map { resident ->
                ResidentBalance(
                    resident = resident,
                    balanceCents = calculator.balanceOf(
                        transactions, settings, residents, resident.id
                    )
                )
            },
            settings = settings,
            transactionCount = transactions.size,
            currentResidentId = currentId
        )
    }
        .catch { emit(BankUiState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BankUiState())

    val tasks: StateFlow<List<ChoreTask>> = choreRepository.getTasks()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val events: StateFlow<List<RepEvent>> = eventRepository.getEvents()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Lança uma despesa: [payerId] pagou [totalValue] e o valor é rateado entre os moradores
     * de [weights], proporcionalmente ao peso de cada um.
     */
    fun saveExpense(
        description: String,
        payerId: String,
        totalCents: Long,
        weights: Map<String, Double>
    ) {
        if (description.isBlank() || totalCents <= 0L || payerId.isBlank()) return

        val effectiveWeights = weights.filterValues { it > 0.0 }
        if (effectiveWeights.isEmpty()) return

        viewModelScope.launch {
            transactionRepository.saveTransaction(
                Transaction(
                    description = description,
                    type = if (effectiveWeights.size == 1) TransactionType.PRIVADO
                    else TransactionType.COLETIVO,
                    payerId = payerId,
                    totalValueCents = totalCents,
                    weights = effectiveWeights
                )
            )
        }
    }

    /**
     * Lança um pagamento do morador para o banco. É uma transação sem rateio: o morador
     * entra como pagador e ninguém divide o valor, então a dívida dele cai no mesmo tanto.
     */
    fun savePayment(residentId: String, amountCents: Long, description: String = "Pagamento") {
        if (residentId.isBlank() || amountCents <= 0L) return

        viewModelScope.launch {
            transactionRepository.saveTransaction(
                Transaction(
                    description = description,
                    type = TransactionType.ENTRADA,
                    payerId = residentId,
                    totalValueCents = amountCents,
                    weights = emptyMap()
                )
            )
        }
    }

    fun saveResident(resident: Resident) {
        if (resident.name.isBlank()) return
        viewModelScope.launch { residentRepository.saveResident(resident) }
    }

    fun removeResident(residentId: String) {
        viewModelScope.launch { residentRepository.removeResident(residentId) }
    }

    /** Só a tarefa do próprio morador pode ser marcada; as outras são consulta. */
    fun setTaskDone(taskId: String, done: Boolean) {
        val task = tasks.value.firstOrNull { it.id == taskId } ?: return
        if (task.isRest || currentResidentId.value !in task.assigneeIds) return

        viewModelScope.launch { choreRepository.setDone(taskId, done) }
    }
}
