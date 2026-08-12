package com.mach.apps.repostinho

import android.app.Application
import com.mach.apps.repostinho.di.initKoin

class RepostinhoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // `filesDir` sobrevive a atualizações do app; o cache do banco mora aqui.
        initKoin(filesDir.absolutePath)
    }
}
