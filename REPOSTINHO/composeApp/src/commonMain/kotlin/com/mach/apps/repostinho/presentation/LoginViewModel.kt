package com.mach.apps.repostinho.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mach.apps.repostinho.data.repository.AuthRepository
import com.mach.apps.repostinho.data.repository.SignInError
import com.mach.apps.repostinho.data.repository.SignInException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isBusy: Boolean = false,
    val error: String? = null
)

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {

    private val state = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = state.asStateFlow()

    /**
     * `null` enquanto ninguém entrou.
     *
     * Vem do disco, não da rede: o app precisa saber de quem é o saldo antes de a primeira
     * resposta chegar, senão ele piscaria a tela de login a cada abertura.
     */
    val currentResidentId: StateFlow<String?> = auth.currentResidentId()
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun onEmail(value: String) {
        // Limpa o erro ao digitar: manter "email ou senha não conferem" embaixo de um
        // campo que a pessoa já está corrigindo só atrapalha.
        state.value = state.value.copy(email = value, error = null)
    }

    fun onPassword(value: String) {
        state.value = state.value.copy(password = value, error = null)
    }

    fun signIn() {
        val current = state.value
        if (current.isBusy) return

        viewModelScope.launch {
            state.value = current.copy(isBusy = true, error = null)
            val result = auth.signIn(current.email, current.password)
            state.value = state.value.copy(
                isBusy = false,
                // Senha some da memória na falha: se a pessoa errou, vai redigitar de
                // qualquer jeito, e não há razão para ela ficar guardada.
                password = if (result.isSuccess) "" else "",
                error = result.exceptionOrNull()?.let(::message)
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            auth.signOut()
            state.value = LoginUiState()
        }
    }

    private fun message(error: Throwable): String = when ((error as? SignInException)?.error) {
        SignInError.InvalidCredentials -> "Email ou senha não conferem."
        // Não diz "esse email não é de morador" com todas as letras para não virar um
        // jeito de descobrir quem mora aqui — mas diz o suficiente para a pessoa saber
        // que o problema não é a senha.
        SignInError.NotAResident ->
            "Este email não está cadastrado como morador. Fale com quem cuida do banco."
        is SignInError.Unavailable ->
            "Não foi possível entrar agora. Tente de novo em instantes."
        null -> "Não foi possível entrar agora. Tente de novo em instantes."
    }
}
