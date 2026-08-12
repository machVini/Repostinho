package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.model.CaixinhaLine
import com.mach.apps.repostinho.data.model.MemberBalance
import com.mach.apps.repostinho.data.model.Movement
import com.mach.apps.repostinho.data.remote.BankApi
import com.mach.apps.repostinho.data.remote.BankApiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** De onde vieram os números que estão na tela. */
sealed interface SyncState {
    data object Loading : SyncState

    /** Vieram da planilha agora. [generatedAt] é ISO-8601, como o Worker devolve. */
    data class Live(val generatedAt: String) : SyncState

    /** A busca falhou e a tela está com o retrato embutido no app. */
    data class Fallback(val reason: String) : SyncState
}

/**
 * Os dados do banco da rep, na forma das abas da planilha.
 *
 * A interface é só de leitura de propósito: o lançamento continua sendo feito na planilha,
 * que tem as macros e o formulário. O app reflete, não escreve.
 */
interface BankSheetRepository {
    fun getBalances(): Flow<List<MemberBalance>>
    fun getMovements(): Flow<List<Movement>>
    fun getCaixinha(): Flow<List<CaixinhaLine>>
    fun getSyncState(): Flow<SyncState>

    /** Busca a versão atual. Não lança: falha vira [SyncState.Fallback]. */
    suspend fun refresh()
}

/**
 * Busca a planilha convertida no `banco-api` a cada abertura do app.
 *
 * Começa exibindo o retrato embutido em [BankSheetSeed] e o substitui quando a resposta
 * chega. Sem rede, a tela mostra o retrato com um aviso — melhor um número velho e
 * identificado como velho do que uma tela vazia.
 */
class RemoteBankSheetRepository(private val api: BankApi) : BankSheetRepository {

    private val balances = MutableStateFlow(BankSheetSeed.BALANCES)
    private val movements = MutableStateFlow(BankSheetSeed.MOVEMENTS)
    private val caixinha = MutableStateFlow(BankSheetSeed.CAIXINHA)
    private val syncState = MutableStateFlow<SyncState>(SyncState.Loading)

    override fun getBalances(): Flow<List<MemberBalance>> = balances.asStateFlow()
    override fun getMovements(): Flow<List<Movement>> = movements.asStateFlow()
    override fun getCaixinha(): Flow<List<CaixinhaLine>> = caixinha.asStateFlow()
    override fun getSyncState(): Flow<SyncState> = syncState.asStateFlow()

    override suspend fun refresh() {
        if (!BankApiConfig.isConfigured) {
            syncState.value = SyncState.Fallback("banco-api não configurado")
            return
        }

        syncState.value = SyncState.Loading
        try {
            val payload = api.fetchSheet()
            // Resposta vazia é sintoma de aba renomeada na planilha. Manter o retrato
            // anterior é menos errado do que zerar os saldos na tela.
            if (payload.balances.isEmpty()) {
                syncState.value = SyncState.Fallback("a planilha voltou sem saldos")
                return
            }
            balances.value = payload.balances
            movements.value = payload.movements
            caixinha.value = payload.caixinha
            syncState.value = SyncState.Live(payload.generatedAt)
        } catch (e: Exception) {
            syncState.value = SyncState.Fallback(e.message ?: "falha ao buscar")
        }
    }
}

/** Só o retrato embutido: usado quando não se quer rede (testes, desenvolvimento). */
class InMemoryBankSheetRepository : BankSheetRepository {

    private val state = MutableStateFlow<SyncState>(
        SyncState.Fallback("retrato embutido no app")
    )

    override fun getBalances(): Flow<List<MemberBalance>> =
        MutableStateFlow(BankSheetSeed.BALANCES).asStateFlow()

    override fun getMovements(): Flow<List<Movement>> =
        MutableStateFlow(BankSheetSeed.MOVEMENTS).asStateFlow()

    override fun getCaixinha(): Flow<List<CaixinhaLine>> =
        MutableStateFlow(BankSheetSeed.CAIXINHA).asStateFlow()

    override fun getSyncState(): Flow<SyncState> = state.asStateFlow()

    override suspend fun refresh() = Unit
}
