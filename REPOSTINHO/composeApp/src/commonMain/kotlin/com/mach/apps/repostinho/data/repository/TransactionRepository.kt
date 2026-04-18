package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getTransactions(): Flow<List<Transaction>>
    suspend fun saveTransaction(transaction: Transaction)
}