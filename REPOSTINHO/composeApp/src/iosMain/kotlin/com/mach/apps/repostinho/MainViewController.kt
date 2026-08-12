package com.mach.apps.repostinho

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import com.mach.apps.repostinho.di.initKoin
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
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
        initKoin(cacheDirectory())
    }
}

/**
 * Application Support, e não Documents: o cache do banco é dado derivado, e não tem por
 * que aparecer para o usuário nem ir para o backup como se fosse conteúdo dele.
 */
@OptIn(ExperimentalForeignApi::class)
private fun cacheDirectory(): String {
    val manager = NSFileManager.defaultManager
    val url = manager.URLsForDirectory(
        NSApplicationSupportDirectory,
        NSUserDomainMask
    ).firstOrNull() as? NSURL

    val path = url?.path ?: NSTemporaryDirectory()
    // O diretório existe no simulador, mas não necessariamente no primeiro boot no device.
    manager.createDirectoryAtPath(path, true, null, null)
    return path
}
