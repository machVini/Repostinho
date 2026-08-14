package com.mach.apps.repostinho.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * Toque fora de um campo fecha o teclado.
 *
 * Sem isto, num formulário que rola o teclado tapa metade da tela e não há para onde
 * tocar: o botão de confirmar fica escondido atrás dele.
 *
 * Vai no container, e não em cada campo: o gesto só chega aqui quando nenhum filho o
 * consumiu, então tocar num `TextField` ou num botão continua fazendo o que deveria.
 *
 * Fecha de dois jeitos porque eles não são a mesma coisa: `clearFocus` tira o cursor do
 * campo, e `hide` baixa o teclado. Em geral um puxa o outro, mas quando não puxa o
 * teclado fica aberto sobre um campo sem foco.
 */
fun Modifier.dismissKeyboardOnTap(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    pointerInput(Unit) {
        detectTapGestures(
            onTap = {
                focusManager.clearFocus()
                keyboard?.hide()
            }
        )
    }
}
