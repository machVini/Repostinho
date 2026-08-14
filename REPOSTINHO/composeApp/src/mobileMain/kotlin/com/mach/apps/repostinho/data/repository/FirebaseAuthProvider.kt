package com.mach.apps.repostinho.data.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.auth

/**
 * Prova a identidade pelo Firebase Auth, com email e senha.
 *
 * Email e senha, e não link mágico: o link precisa voltar para o app, no iOS isso exige
 * universal links, e universal links exigem o entitlement Associated Domains — que times
 * pessoais (conta Apple gratuita) não suportam. Com senha, nada disso entra no caminho.
 *
 * As contas são criadas no console pelo pessoal do banco; o app nunca cadastra ninguém.
 * Isso faz "só morador entra" valer já no provedor, antes mesmo da checagem da lista.
 */
class FirebaseAuthProvider : AuthProvider {

    override suspend fun authenticate(email: String, password: String): Result<Unit> = try {
        Firebase.auth.signInWithEmailAndPassword(email, password)
        Result.success(Unit)
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        Result.failure(SignInException(SignInError.InvalidCredentials))
    } catch (e: FirebaseAuthInvalidUserException) {
        // Conta inexistente ou desativada. Para quem está digitando é o mesmo caso de
        // senha errada, e distinguir na tela contaria quem tem conta aqui.
        Result.failure(SignInException(SignInError.InvalidCredentials))
    } catch (e: Exception) {
        // Rede fora, projeto não configurado, provedor desligado no console. Nenhum deles
        // é culpa de quem digitou, e todos podem passar sozinhos.
        Result.failure(SignInException(SignInError.Unavailable(e.message.orEmpty())))
    }

    override suspend fun signOut() {
        // Falhar aqui não pode prender ninguém no app: a sessão local é limpa de todo
        // jeito por quem chamou, e o pior caso é o Firebase seguir com a sessão dele até
        // o próximo login.
        runCatching { Firebase.auth.signOut() }
    }
}
