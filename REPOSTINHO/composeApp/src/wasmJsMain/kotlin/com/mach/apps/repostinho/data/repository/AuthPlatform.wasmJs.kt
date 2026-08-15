package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.remote.AuthTokenProvider
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlinx.coroutines.await

/*
 * Ponte para o SDK JavaScript do Firebase.
 *
 * O `index.html` carrega o SDK e expõe três funções em `window`. É interop mínima de
 * propósito: só as três operações que a costura de auth do app pede.
 */

private external interface FirebaseBridge : JsAny {
    fun signIn(email: String, password: String): Promise<JsAny?>
    fun signOut(): Promise<JsAny?>
    fun idToken(): Promise<JsAny?>
}

private fun bridge(): FirebaseBridge? = js("globalThis.repostinhoAuth || null")

class WebFirebaseAuthProvider : AuthProvider {

    override suspend fun authenticate(email: String, password: String): Result<Unit> {
        val api = bridge() ?: return Result.failure(
            IllegalStateException("SDK do Firebase não carregou")
        )
        return runCatching {
            api.signIn(email, password).await()
            Unit
        }
    }

    override suspend fun signOut() {
        runCatching { bridge()?.signOut()?.await() }
    }
}

class WebFirebaseTokens : AuthTokenProvider {
    override suspend fun idToken(): String? =
        runCatching { bridge()?.idToken()?.await()?.toString() }.getOrNull()
}

actual fun platformAuthProvider(): AuthProvider = WebFirebaseAuthProvider()

actual fun platformAuthTokenProvider(): AuthTokenProvider = WebFirebaseTokens()
