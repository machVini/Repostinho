package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.model.Transaction
import com.mach.apps.repostinho.domain.BalanceCalculator
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class TransactionRepositoryImpl(private val calculator: BalanceCalculator) : TransactionRepository {
    private val firestore = Firebase.firestore
    private val collection = firestore.collection("republica_transactions")

    override fun getTransactions(): Flow<List<Transaction>> {
        // Retorna um Flow em tempo real para o app atualizar o saldo instantaneamente
        return collection.snapshots.map { snapshot ->
            snapshot.documents.map { it.data<Transaction>() }
        }
    }

    override suspend fun saveTransaction(transaction: Transaction) {
        collection.add(transaction)
    }
}