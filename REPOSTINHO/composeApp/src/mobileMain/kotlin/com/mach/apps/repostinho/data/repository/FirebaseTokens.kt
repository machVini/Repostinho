package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.remote.AuthTokenProvider
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

/**
 * O token do morador logado, para as chamadas ao `banco-api`.
 *
 * `getIdToken(false)` devolve o token em cache e só renova quando ele está perto de
 * vencer — pedir renovação forçada a cada chamada seria uma ida ao Google por request.
 *
 * Devolve `null` quando ninguém está logado, e quem chama trata isso como "não dá para
 * falar com o servidor": nenhuma tela do app existe antes do login, então isso só
 * acontece na janela entre sair e a tela trocar.
 */
class FirebaseTokens : AuthTokenProvider {
    override suspend fun idToken(): String? =
        runCatching { Firebase.auth.currentUser?.getIdToken(false) }.getOrNull()
}
