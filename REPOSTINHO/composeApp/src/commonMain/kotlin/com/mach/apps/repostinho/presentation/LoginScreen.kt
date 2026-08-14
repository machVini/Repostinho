package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mach.apps.repostinho.ui.dismissKeyboardOnTap
import com.mach.apps.repostinho.ui.loginBackgroundColor
import com.mach.apps.repostinho.ui.loginButtonColor
import com.mach.apps.repostinho.ui.onLoginBackgroundColor
import com.mach.apps.repostinho.ui.onLoginButtonColor
import org.jetbrains.compose.resources.painterResource
import repostinho.composeapp.generated.resources.Res
import repostinho.composeapp.generated.resources.logo_rep
import androidx.compose.foundation.Image

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    email: String,
    password: String,
    isBusy: Boolean,
    error: String?,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canSubmit = email.isNotBlank() && password.isNotBlank() && !isBusy
    val onBackground = onLoginBackgroundColor()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(loginBackgroundColor())
            .verticalScroll(rememberScrollState())
            .dismissKeyboardOnTap()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.logo_rep),
            contentDescription = "Brasão da República Postinho",
            modifier = Modifier.size(140.dp)
        )

        Text(
            text = "República Postinho",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Entre com o email cadastrado na rep.",
            style = MaterialTheme.typography.bodyMedium,
            // Um pouco apagado para não disputar com o título logo acima.
            color = onBackground.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmail,
            label = { Text("Email") },
            singleLine = true,
            enabled = !isBusy,
            colors = camposBrancos(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPassword,
            label = { Text("Senha") },
            singleLine = true,
            enabled = !isBusy,
            colors = camposBrancos(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )

        if (error != null) {
            // Branco, e não o vermelho de erro do tema: sobre o azul do fundo aquele
            // vermelho fica ilegível, e aqui o texto já basta para dizer que deu errado.
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = onBackground,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
        }

        Button(
            onClick = onSignIn,
            enabled = canSubmit,
            colors = ButtonDefaults.buttonColors(
                containerColor = loginButtonColor(),
                contentColor = onLoginButtonColor(),
                // Desabilitado continua amarelo, só apagado: o padrão do Material some
                // no azul e o botão parece ter desaparecido.
                disabledContainerColor = loginButtonColor().copy(alpha = 0.45f),
                disabledContentColor = onLoginButtonColor().copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(52.dp)
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    color = onLoginButtonColor(),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text("Entrar", fontWeight = FontWeight.Bold)
            }
        }

        Text(
            text = "Só quem está cadastrado como morador consegue entrar. Para incluir " +
                "alguém, fale com quem cuida do banco.",
            style = MaterialTheme.typography.bodySmall,
            color = onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 32.dp)
        )
    }
}

/**
 * Campos brancos nos dois temas.
 *
 * O fundo da tela é colorido de ponta a ponta, e um campo transparente sobre ele mal se
 * distingue. Branco fixo, e não `surface`, porque no tema escuro o `surface` é quase a
 * mesma cor do fundo daqui.
 */
@Composable
private fun camposBrancos() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color.White.copy(alpha = 0.7f),
    focusedTextColor = TextoNoCampo,
    unfocusedTextColor = TextoNoCampo,
    disabledTextColor = TextoNoCampo.copy(alpha = 0.6f),
    cursorColor = TextoNoCampo,
    focusedBorderColor = Color.White,
    unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
    focusedLabelColor = RotuloNoCampo,
    unfocusedLabelColor = RotuloNoCampo,
    disabledLabelColor = RotuloNoCampo.copy(alpha = 0.6f)
)

/** Sobre branco, o texto tem que ser escuro nos dois temas. */
private val TextoNoCampo = Color(0xFF1D1B16)
private val RotuloNoCampo = Color(0xFF5B564A)
