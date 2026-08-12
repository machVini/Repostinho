package com.mach.apps.repostinho.di

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

val appModule = module {
    // Moradores, tarefas e eventos ainda são fixos no código e se perdem ao fechar o app.
    // Só o banco tem fonte de verdade fora dele (a planilha).
    single<ResidentRepository> { InMemoryResidentRepository() }
    single<ChoreRepository> { InMemoryChoreRepository() }
    single<EventRepository> { InMemoryEventRepository() }

    // O banco vem da planilha da rep, convertida em JSON pelo banco-api. Sem a URL
    // configurada no local.properties, cai no retrato embutido sozinho.
    single { BankApi.defaultClient() }
    single { BankApi(get()) }
    single<BankSheetRepository> { RemoteBankSheetRepository(get()) }

    // ViewModel (no KMP usamos o Compose ViewModel ou bibliotecas como Voyager/Decompose)
    factory { DashboardViewModel(get(), get(), get(), get()) }
}

// Função para inicializar o Koin (chamada no Android e iOS)
fun initKoin() = startKoin {
    modules(appModule)
}
