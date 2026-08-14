package com.mach.apps.repostinho.data.repository

import com.mach.apps.repostinho.data.local.SessionStore
import com.mach.apps.repostinho.data.model.Resident
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/** Por que a entrada foi recusada. A tela precisa dizer coisas diferentes para cada uma. */
sealed interface SignInError {
    /** Email e senha não conferem no provedor de autenticação. */
    data object InvalidCredentials : SignInError

    /**
     * A conta existe, mas o email não é de nenhum morador.
     *
     * É este o caso que faz o app ser só da rep: autenticar prova que o email é seu, não
     * que você mora aqui.
     */
    data object NotAResident : SignInError

    /** Rede, provedor fora do ar, qualquer coisa que tentar de novo pode resolver. */
    data class Unavailable(val reason: String) : SignInError
}

interface AuthRepository {
    /** O morador logado, ou `null`. Emite na abertura, antes de qualquer rede. */
    fun currentResidentId(): Flow<String?>

    suspend fun signIn(email: String, password: String): Result<Resident>

    suspend fun signOut()
}

/**
 * Autenticação em duas etapas, de propósito.
 *
 * A primeira prova que o email é de quem está digitando — é o Firebase Auth. A segunda
 * pergunta se aquele email pertence a um morador, e essa é nossa: um provedor de
 * autenticação diz que você é você, nunca que você mora na rep.
 *
 * Por isso a lista de moradores é a lista de permitidos, e o `email` vazio é o que impede
 * alguém de entrar sem ter sido cadastrado.
 */
class ResidentAuthRepository(
    private val residents: ResidentRepository,
    private val session: SessionStore,
    private val provider: AuthProvider
) : AuthRepository {

    private val current = MutableStateFlow(session.read())

    override fun currentResidentId(): Flow<String?> = current.asStateFlow()

    override suspend fun signIn(email: String, password: String): Result<Resident> {
        val normalized = email.trim().lowercase()

        val authenticated = provider.authenticate(normalized, password)
        authenticated.exceptionOrNull()?.let { return Result.failure(it) }

        // A lista pode estar velha se o app abriu sem rede; rebuscar aqui evita barrar
        // quem foi cadastrado depois da última abertura.
        residents.refresh()
        val resident = residents.getResidents().first()
            .firstOrNull { it.isActive && it.email?.lowercase() == normalized }
            ?: return Result.failure(SignInException(SignInError.NotAResident))

        session.write(resident.id)
        current.value = resident.id
        return Result.success(resident)
    }

    override suspend fun signOut() {
        provider.signOut()
        session.clear()
        current.value = null
    }
}

/** Erro de entrada com a causa preservada, para a tela escolher a frase. */
class SignInException(val error: SignInError) : Exception()

/**
 * Quem prova que o email é de quem está digitando.
 *
 * É interface porque é a única peça presa a um fornecedor: o resto do login — casar com
 * o morador, guardar a sessão, abrir o app — não muda se o Firebase sair um dia.
 */
interface AuthProvider {
    suspend fun authenticate(email: String, password: String): Result<Unit>
    suspend fun signOut()
}

