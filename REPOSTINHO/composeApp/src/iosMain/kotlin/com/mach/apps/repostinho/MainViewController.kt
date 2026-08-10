package com.mach.apps.repostinho

import androidx.compose.ui.window.ComposeUIViewController
import com.mach.apps.repostinho.di.initKoin

fun MainViewController() = ComposeUIViewController {
    initKoinOnce()
    App()
}

private var koinStarted = false

private fun initKoinOnce() {
    if (!koinStarted) {
        koinStarted = true
        initKoin()
    }
}