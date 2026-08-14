package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.remote.AuthTokenProvider

/**
 * Quem fala com o Firebase, por plataforma.
 *
 * No Android e no iOS é o SDK do GitLive. Na web não pode ser: o GitLive não publica
 * artefato Wasm, e é justamente por isso que estas duas fábricas existem — sem elas, o
 * `commonMain` não compilaria para o navegador.
 */
expect fun platformAuthProvider(): AuthProvider

expect fun platformAuthTokenProvider(): AuthTokenProvider
