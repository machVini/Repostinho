package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementação temporária que guarda tudo em memória, para rodar o app enquanto o projeto
 * no Firebase não existe. Os dados somem quando o app é fechado.
 *
 * Para voltar ao Firestore, troque a linha correspondente em [com.mach.apps.repostinho.di.appModule].
 */
class InMemoryTransactionRepository : TransactionRepository {

    private val transactions = MutableStateFlow<List<Transaction>>(emptyList())
    private var nextId = 1

    override fun getTransactions(): Flow<List<Transaction>> = transactions.asStateFlow()

    override suspend fun saveTransaction(transaction: Transaction) {
        transactions.value = transactions.value + transaction.copy(id = "local-${nextId++}")
    }
}
