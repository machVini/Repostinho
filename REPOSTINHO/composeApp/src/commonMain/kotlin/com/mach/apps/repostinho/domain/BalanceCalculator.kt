package com.mach.apps.repostinho.domain

import com.mach.apps.repostinho.data.model.Transaction

class BalanceCalculator {
    fun calculateDebt(transaction: Transaction, residentId: String): Double {
        val totalWeights = transaction.weights.values.sum()
        if (totalWeights == 0.0) return 0.0

        val residentWeight = transaction.weights[residentId] ?: 0.0
        return (residentWeight / totalWeights) * transaction.totalValue
    }

    fun calculateUserBalance(transactions: List<Transaction>, userId: String): Double {
        var balance = 0.0
        transactions.forEach { tx ->
            // Se o usuário foi o pagador, ele recebe um CRÉDITO do valor total
            if (tx.payerId == userId) {
                balance += tx.totalValue
            }
            // Independente de ser pagador, ele subtrai a parte que DEVE (DÉBITO)
            balance -= calculateDebt(tx, userId)
        }
        return balance
    }
}