package com.mach.apps.repostinho.di

import com.mach.apps.repostinho.data.local.BankSheetCache
import com.mach.apps.repostinho.data.local.textFileStore
import com.mach.apps.repostinho.data.repository.BankSheetRepository
import com.mach.apps.repostinho.data.repository.ChoreRepository
import com.mach.apps.repostinho.data.repository.EventRepository
import com.mach.apps.repostinho.data.remote.BankApi
import com.mach.apps.repostinho.data.repository.InMemoryChoreRepository
import com.mach.apps.repostinho.data.repository.InMemoryEventRepository
import com.mach.apps.repostinho.data.repository.InMemoryResidentRepository
import com.mach.apps.repostinho.data.repository.RemoteBankSheetRepository
import com.mach.apps.repostinho.data.repository.ResidentRepository
import com.mach.apps.repostinho.presentation.DashboardViewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * [cacheDirectory] é onde a última resposta do banco-api fica gravada. Vem de fora porque
 * no Android depende do `Context`, que só existe na `Application`.
 */
fun appModule(cacheDirectory: String) = module {
    // Moradores, tarefas e eventos ainda são fixos no código e se perdem ao fechar o app.
    // Só o banco tem fonte de verdade fora dele (a planilha).
    single<ResidentRepository> { InMemoryResidentRepository() }
    single<ChoreRepository> { InMemoryChoreRepository() }
    single<EventRepository> { InMemoryEventRepository() }

    // O banco vem da planilha da rep, convertida em JSON pelo banco-api, e a última
    // resposta boa fica em disco para as aberturas sem rede.
    single { BankApi.defaultClient() }
    single { BankApi(get()) }
    single { BankSheetCache(textFileStore(cacheDirectory)) }
    single<BankSheetRepository> { RemoteBankSheetRepository(get(), get()) }

    // ViewModel (no KMP usamos o Compose ViewModel ou bibliotecas como Voyager/Decompose)
    factory { DashboardViewModel(get(), get(), get(), get()) }
}

// Função para inicializar o Koin (chamada no Android e iOS)
fun initKoin(cacheDirectory: String) = startKoin {
    modules(appModule(cacheDirectory))
}
