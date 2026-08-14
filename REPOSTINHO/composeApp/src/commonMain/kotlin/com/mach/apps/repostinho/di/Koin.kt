package com.mach.apps.repostinho.di

import com.mach.apps.repostinho.data.local.BankSheetCache
import com.mach.apps.repostinho.data.local.EventsCache
import com.mach.apps.repostinho.data.local.MeetingNotesCache
import com.mach.apps.repostinho.data.local.ResidentsCache
import com.mach.apps.repostinho.data.local.SessionStore
import com.mach.apps.repostinho.data.local.RotationPreferenceStore
import com.mach.apps.repostinho.data.local.ThemePreferenceStore
import com.mach.apps.repostinho.data.local.textFileStore
import com.mach.apps.repostinho.data.repository.AuthProvider
import com.mach.apps.repostinho.data.repository.AuthRepository
import com.mach.apps.repostinho.data.repository.FirebaseAuthProvider
import com.mach.apps.repostinho.data.repository.BankSheetRepository
import com.mach.apps.repostinho.data.repository.ResidentAuthRepository
import com.mach.apps.repostinho.data.repository.ChoreRepository
import com.mach.apps.repostinho.data.repository.EventRepository
import com.mach.apps.repostinho.data.remote.BankApi

import com.mach.apps.repostinho.data.repository.MeetingNotesRepository
import com.mach.apps.repostinho.data.repository.RemoteBankSheetRepository
import com.mach.apps.repostinho.data.repository.RemoteEventRepository
import com.mach.apps.repostinho.data.repository.RemoteResidentRepository
import com.mach.apps.repostinho.data.repository.RotatingChoreRepository
import com.mach.apps.repostinho.data.repository.RemoteMeetingNotesRepository
import com.mach.apps.repostinho.data.repository.ResidentRepository
import com.mach.apps.repostinho.presentation.DashboardViewModel
import com.mach.apps.repostinho.presentation.LoginViewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * [cacheDirectory] é onde a última resposta do banco-api fica gravada. Vem de fora porque
 * no Android depende do `Context`, que só existe na `Application`.
 */
fun appModule(cacheDirectory: String) = module {
    // Os moradores vêm do banco-api; a lista embutida no app é só o retrato inicial,
    // para a primeira abertura sem rede não ficar sem ninguém.
    single { ResidentsCache(textFileStore(cacheDirectory)) }
    single<ResidentRepository> { RemoteResidentRepository(get(), get()) }

    // A agenda fixa vem no binário; o que a rep cadastra pela tela mora no banco-api,
    // para aparecer no celular de todos.
    single { EventsCache(textFileStore(cacheDirectory)) }
    single<EventRepository> { RemoteEventRepository(get(), get()) }

    // As tarefas também são fixas, mas quem faz cada uma sai da data: o rodízio é
    // calculado, e só a pausa precisa ficar gravada.
    single { RotationPreferenceStore(textFileStore(cacheDirectory)) }
    single<ChoreRepository> { RotatingChoreRepository(get(), get()) }

    // O banco vem da planilha da rep, convertida em JSON pelo banco-api, e a última
    // resposta boa fica em disco para as aberturas sem rede.
    single { BankApi.defaultClient() }
    single { BankApi(get()) }
    single { BankSheetCache(textFileStore(cacheDirectory)) }
    single<BankSheetRepository> { RemoteBankSheetRepository(get(), get()) }

    // As atas moram numa pasta do Drive; o Worker lista, o app só abre os links.
    single { MeetingNotesCache(textFileStore(cacheDirectory)) }

    // A escolha de tema mora no mesmo diretório do cache do banco.
    single { ThemePreferenceStore(textFileStore(cacheDirectory)) }
    single<MeetingNotesRepository> { RemoteMeetingNotesRepository(get(), get()) }

    // Quem entra no app. O provedor é a única peça que o Firebase substitui — o resto
    // do login (casar com o morador, guardar a sessão) não muda com ele.
    single { SessionStore(textFileStore(cacheDirectory)) }
    single<AuthProvider> { FirebaseAuthProvider() }
    single<AuthRepository> { ResidentAuthRepository(get(), get(), get()) }

    // ViewModel (no KMP usamos o Compose ViewModel ou bibliotecas como Voyager/Decompose)
    factory { DashboardViewModel(get(), get(), get(), get(), get(), get()) }
    factory { LoginViewModel(get()) }
}

// Função para inicializar o Koin (chamada no Android e iOS)
fun initKoin(cacheDirectory: String) = startKoin {
    modules(appModule(cacheDirectory))
}
