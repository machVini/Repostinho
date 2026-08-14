package com.mach.apps.repostinho.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mach.apps.repostinho.ui.accentColor
import com.mach.apps.repostinho.ui.dismissKeyboardOnTap

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .dismissKeyboardOnTap()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "República Postinho",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor(),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Entre com o email cadastrado na rep.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmail,
            label = { Text("Email") },
            singleLine = true,
            enabled = !isBusy,
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
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
        }

        Button(
            onClick = onSignIn,
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                    strokeWidth = 2.dp
                )
            }
            Text("Entrar")
        }

        Text(
            text = "Só quem está cadastrado como morador consegue entrar. Para incluir " +
                "alguém, fale com quem cuida do banco.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 32.dp)
        )
    }
}
