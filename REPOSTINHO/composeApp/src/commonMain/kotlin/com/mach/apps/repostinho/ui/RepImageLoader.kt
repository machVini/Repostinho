package com.mach.apps.repostinho.ui

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.ktor2.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.mach.apps.repostinho.data.remote.BankApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.header

/**
 * O carregador de imagens do app.
 *
 * Existe por um motivo só: a foto do morador é servida pelo `banco-api`, atrás do mesmo
 * token do resto. O Coil, sozinho, faria um GET sem cabeçalho nenhum e toda foto voltaria
 * 401 — com o monograma no lugar, que é indistinguível de "morador sem foto".
 *
 * O token só é anexado em chamadas para o próprio banco-api: um dia em que a foto de
 * alguém aponte para outro lugar, não é para o segredo da rep viajar junto.
 */
fun repImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
    .components {
        add(KtorNetworkFetcherFactory(httpClient = { authorizedClient() }))
    }
    .crossfade(true)
    .build()

/** O host do banco-api, tirado da URL base gerada em build. */
private val apiHost: String = BankApiConfig.BASE_URL
    .substringAfter("://", "")
    .substringBefore("/")
    .substringBefore(":")

private fun authorizedClient(): HttpClient = HttpClient {
    install(DefaultRequest) {
        if (apiHost.isNotBlank() && url.host == apiHost) {
            header("x-rep-token", BankApiConfig.TOKEN)
        }
    }
}
