package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.model.CaixinhaLine
import com.mach.apps.repostinho.data.model.MemberBalance
import com.mach.apps.repostinho.data.model.Movement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
}

/**
 * Serve o retrato da planilha embutido em [BankSheetSeed].
 *
 * É a implementação provisória: quando existir a publicação da planilha em JSON, entra no
 * lugar dela uma que busca por HTTP, sem que as telas precisem mudar.
 */
class InMemoryBankSheetRepository : BankSheetRepository {

    private val balances = MutableStateFlow(BankSheetSeed.BALANCES)
    private val movements = MutableStateFlow(BankSheetSeed.MOVEMENTS)
    private val caixinha = MutableStateFlow(BankSheetSeed.CAIXINHA)

    override fun getBalances(): Flow<List<MemberBalance>> = balances.asStateFlow()
    override fun getMovements(): Flow<List<Movement>> = movements.asStateFlow()
    override fun getCaixinha(): Flow<List<CaixinhaLine>> = caixinha.asStateFlow()
}
