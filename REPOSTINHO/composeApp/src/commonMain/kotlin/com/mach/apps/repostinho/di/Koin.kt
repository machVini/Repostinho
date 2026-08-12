package com.mach.apps.repostinho.di

import com.mach.apps.repostinho.data.repository.BankSettingsRepository
import com.mach.apps.repostinho.data.repository.BankSheetRepository
import com.mach.apps.repostinho.data.repository.ChoreRepository
import com.mach.apps.repostinho.data.repository.EventRepository
import com.mach.apps.repostinho.data.repository.InMemoryBankSettingsRepository
import com.mach.apps.repostinho.data.repository.InMemoryBankSheetRepository
import com.mach.apps.repostinho.data.repository.InMemoryChoreRepository
import com.mach.apps.repostinho.data.repository.InMemoryEventRepository
import com.mach.apps.repostinho.data.repository.InMemoryResidentRepository
import com.mach.apps.repostinho.data.repository.ResidentRepository
import com.mach.apps.repostinho.presentation.DashboardViewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

val appModule = module {
    // Repositórios em memória: rodam sem projeto no Firebase, mas perdem os dados ao fechar o app.
    // Quando o projeto no Firebase existir (e o google-services.json estiver em composeApp/),
    // troque estas linhas pelas implementações do Firestore.
    single<ResidentRepository> { InMemoryResidentRepository() }
    single<BankSettingsRepository> { InMemoryBankSettingsRepository() }
    single<ChoreRepository> { InMemoryChoreRepository() }
    single<EventRepository> { InMemoryEventRepository() }

    // O banco vem da planilha da rep. Hoje é um retrato embutido no app; quando a planilha
    // for publicada em JSON, só esta linha muda.
    single<BankSheetRepository> { InMemoryBankSheetRepository() }

    // ViewModel (no KMP usamos o Compose ViewModel ou bibliotecas como Voyager/Decompose)
    factory { DashboardViewModel(get(), get(), get(), get(), get()) }
}

// Função para inicializar o Koin (chamada no Android e iOS)
fun initKoin() = startKoin {
    modules(appModule)
}
