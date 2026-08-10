package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.model.BankSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface BankSettingsRepository {
    fun getSettings(): Flow<BankSettings>
    suspend fun saveSettings(settings: BankSettings)
}

/** Implementação temporária em memória, enquanto o projeto no Firebase não existe. */
class InMemoryBankSettingsRepository : BankSettingsRepository {

    // Aluguel + contas do mês. Não há tela para editar isso desde que o card saiu da aba
    // Saldo, então o valor mora aqui até existir uma tela de admin.
    private val settings = MutableStateFlow(BankSettings(monthlyFixedTotalCents = 750_000L))

    override fun getSettings(): Flow<BankSettings> = settings.asStateFlow()

    override suspend fun saveSettings(settings: BankSettings) {
        this.settings.value = settings
    }
}
