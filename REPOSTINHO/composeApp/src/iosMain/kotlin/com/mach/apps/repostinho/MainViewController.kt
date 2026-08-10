package com.mach.apps.repostinho

import androidx.compose.ui.window.ComposeUIViewController
import com.mach.apps.repostinho.di.initKoin
import platform.UIKit.UIViewController

/**
 * Entrada do app no iOS, chamada pelo `ComposeView` do SwiftUI.
 *
 * O Koin sobe aqui, fora da composição: no Android isso acontece na `Application`, e o
 * iOS não tem equivalente. O guarda existe porque o SwiftUI pode recriar o
 * `UIViewControllerRepresentable`, e `startKoin` duas vezes lança exceção.
 */
fun MainViewController(): UIViewController {
    startKoinOnce()
    return ComposeUIViewController { App() }
}

private var koinStarted = false

private fun startKoinOnce() {
    if (!koinStarted) {
        koinStarted = true
        initKoin()
    }
}
