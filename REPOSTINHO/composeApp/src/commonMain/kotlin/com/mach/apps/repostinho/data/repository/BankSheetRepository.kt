package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.local.BankSheetCache
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

    /** Buscados da planilha agora. */
    data class Live(val generatedAtLabel: String) : SyncState

    /** A busca falhou; a tela está com a última resposta boa, gravada em disco. */
    data class Cached(val generatedAtLabel: String) : SyncState

    /** Falhou e não há nada em cache — primeira abertura sem rede. */
    data class Failed(val reason: String) : SyncState
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

    /**
     * Busca a versão atual. Não lança: falha vira [SyncState.Cached] ou [SyncState.Failed].
     *
     * [fresh] vem do gesto do morador e ignora o cache de borda.
     */
    suspend fun refresh(fresh: Boolean = false)
}

/**
 * Busca a planilha convertida no `banco-api` e guarda a última resposta boa em disco.
 *
 * Sem rede, a tela mostra o cache com a data de quando ele foi buscado. Se nem cache
 * houver — primeira abertura offline — as listas ficam vazias e a tela diz isso, em vez de
 * inventar números.
 */
class RemoteBankSheetRepository(
    private val api: BankApi,
    private val cache: BankSheetCache
) : BankSheetRepository {

    private val balances = MutableStateFlow<List<MemberBalance>>(emptyList())
    private val movements = MutableStateFlow<List<Movement>>(emptyList())
    private val caixinha = MutableStateFlow<List<CaixinhaLine>>(emptyList())
    private val syncState = MutableStateFlow<SyncState>(SyncState.Loading)

    init {
        // O cache entra antes da rede: a tela abre com números na hora, e a resposta os
        // substitui quando chega.
        cache.read()?.let { cached ->
            publish(cached.balances, cached.movements, cached.caixinha)
            syncState.value = SyncState.Cached(cached.generatedAtLabel)
        }
    }

    override fun getBalances(): Flow<List<MemberBalance>> = balances.asStateFlow()
    override fun getMovements(): Flow<List<Movement>> = movements.asStateFlow()
    override fun getCaixinha(): Flow<List<CaixinhaLine>> = caixinha.asStateFlow()
    override fun getSyncState(): Flow<SyncState> = syncState.asStateFlow()

    override suspend fun refresh(fresh: Boolean) {
        if (!BankApiConfig.isConfigured) {
            fallback("banco-api não configurado")
            return
        }

        syncState.value = SyncState.Loading
        try {
            val payload = api.fetchSheet(fresh)
            // Resposta vazia é sintoma de aba renomeada na planilha. Guardar isso no cache
            // apagaria os últimos números bons que o app tinha.
            if (payload.balances.isEmpty()) {
                fallback("a planilha voltou sem saldos")
                return
            }
            publish(payload.balances, payload.movements, payload.caixinha)
            cache.write(payload)
            syncState.value = SyncState.Live(payload.generatedAtLabel)
        } catch (e: Exception) {
            fallback(e.message ?: "falha ao buscar")
        }
    }

    private fun publish(
        newBalances: List<MemberBalance>,
        newMovements: List<Movement>,
        newCaixinha: List<CaixinhaLine>
    ) {
        balances.value = newBalances
        movements.value = newMovements
        caixinha.value = newCaixinha
    }

    private fun fallback(reason: String) {
        val cached = cache.read()
        syncState.value = if (cached == null) {
            SyncState.Failed(reason)
        } else {
            publish(cached.balances, cached.movements, cached.caixinha)
            SyncState.Cached(cached.generatedAtLabel)
        }
    }
}
