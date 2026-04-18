package com.mach.apps.repostinho.di

import com.mach.apps.repostinho.data.repository.TransactionRepository
import com.mach.apps.repostinho.data.repository.TransactionRepositoryImpl
import com.mach.apps.repostinho.domain.BalanceCalculator
import org.koin.core.context.startKoin
import org.koin.dsl.module

val appModule = module {
    // Lógica de cálculo que definimos antes
    single { BalanceCalculator() }

    // Repositório para o Firebase
    single<TransactionRepository> { TransactionRepositoryImpl(get()) }

    // ViewModel (no KMP usamos o Compose ViewModel ou bibliotecas como Voyager/Decompose)
    factory { DashboardViewModel(get(), get()) }
}

// Função para inicializar o Koin (chamada no Android e iOS)
fun initKoin() = startKoin {
    modules(appModule)
}