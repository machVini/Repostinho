package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.remote.AuthTokenProvider

actual fun platformAuthProvider(): AuthProvider = FirebaseAuthProvider()

actual fun platformAuthTokenProvider(): AuthTokenProvider = FirebaseTokens()
