package com.mach.apps.repostinho.ui

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.ktor2.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.mach.apps.repostinho.data.remote.BankApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.api.createClientPlugin
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
    // Sem cache nenhum: trocar a foto e continuar vendo a antiga — em memória até fechar
    // o app, em disco por dias — é pior do que rebaixar uma imagem pequena. São quinze
    // fotos de poucos KB, pedidas por uma tela só. Quando isso pesar, o caminho é
    // versionar a URL da foto, não voltar a cachear às cegas.
    .memoryCachePolicy(CachePolicy.DISABLED)
    .diskCachePolicy(CachePolicy.DISABLED)
    .crossfade(true)
    .build()

/** O host do banco-api, tirado da URL base gerada em build. */
private val apiHost: String = BankApiConfig.BASE_URL
    .substringAfter("://", "")
    .substringBefore("/")
    .substringBefore(":")

/**
 * Põe o token nas chamadas ao banco-api, e só nelas.
 *
 * É um plugin, e não `DefaultRequest`, porque aquele recebe um builder de **valores
 * padrão**: o `url` de lá não é o da requisição em curso, então comparar o host contra ele
 * nunca casava, o cabeçalho nunca ia junto e toda foto voltava 401 — aparecendo como
 * monograma, indistinguível de morador sem foto. Aqui a requisição real é inspecionada.
 */
private val RepToken = createClientPlugin("RepToken") {
    onRequest { request, _ ->
        if (apiHost.isNotBlank() && request.url.host == apiHost) {
            request.header("x-rep-token", BankApiConfig.TOKEN)
        }
    }
}

private fun authorizedClient(): HttpClient = HttpClient {
    install(RepToken)
}
