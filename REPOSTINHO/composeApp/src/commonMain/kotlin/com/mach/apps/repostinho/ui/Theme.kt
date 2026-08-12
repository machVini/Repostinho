package com.mach.apps.repostinho.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Cores tiradas do brasão da rep: o azul do anel e da camisa, o amarelo das letras
// e o dourado do fundo.
val RepBlue = Color(0xFF0B63D6)
val RepYellow = Color(0xFFFFC400)
val RepGold = Color(0xFFC9A227)
val RepBronze = Color(0xFF6E4E0E)

/**
 * Os tons de surface precisam ser declarados um a um: o que fica de fora do
 * [lightColorScheme]/[darkColorScheme] cai no default roxo do Material 3, e é justamente
 * `surfaceContainer*` que pinta os Cards e a barra de navegação.
 */
private val LightColors = lightColorScheme(
    primary = RepBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E3FF),
    onPrimaryContainer = Color(0xFF001A41),
    inversePrimary = Color(0xFFAEC6FF),

    secondary = RepYellow,
    onSecondary = Color(0xFF3D2E00),
    secondaryContainer = Color(0xFFFFE08C),
    onSecondaryContainer = Color(0xFF271900),

    tertiary = RepGold,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB3),
    onTertiaryContainer = Color(0xFF2C1600),

    background = Color(0xFFFFFDF7),
    onBackground = Color(0xFF1D1B16),
    surface = Color(0xFFFFFDF7),
    onSurface = Color(0xFF1D1B16),
    surfaceVariant = Color(0xFFEDE4CB),
    onSurfaceVariant = Color(0xFF4C4636),
    surfaceTint = RepBlue,
    inverseSurface = Color(0xFF32302A),
    inverseOnSurface = Color(0xFFF6F0E6),

    // A rampa puxa para o amarelo bebê: `surfaceContainerHighest` é o que o Card usa.
    surfaceDim = Color(0xFFE6DFC9),
    surfaceBright = Color(0xFFFFFDF7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFAE8),
    surfaceContainer = Color(0xFFFFF7DC),
    surfaceContainerHigh = Color(0xFFFFF4D0),
    surfaceContainerHighest = Color(0xFFFFF0C2),

    outline = Color(0xFF7B776B),
    outlineVariant = Color(0xFFCBC6B8),
    scrim = Color(0xFF000000),

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAEC6FF),
    onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF00458F),
    onPrimaryContainer = Color(0xFFD7E3FF),
    inversePrimary = RepBlue,

    secondary = Color(0xFFF5CB4C),
    onSecondary = Color(0xFF3D2E00),
    secondaryContainer = Color(0xFF5B4300),
    onSecondaryContainer = Color(0xFFFFE08C),

    tertiary = Color(0xFFE0C165),
    onTertiary = Color(0xFF3D2E00),
    tertiaryContainer = Color(0xFF684E0B),
    onTertiaryContainer = Color(0xFFFFDDB3),

    background = Color(0xFF131A23),
    onBackground = Color(0xFFDEE3EA),
    surface = Color(0xFF131A23),
    onSurface = Color(0xFFDEE3EA),
    surfaceVariant = Color(0xFF3F4A57),
    onSurfaceVariant = Color(0xFFBFC8D4),
    surfaceTint = Color(0xFFAEC6FF),
    inverseSurface = Color(0xFFDEE3EA),
    inverseOnSurface = Color(0xFF1A2230),

    // Fundo azul escuro acinzentado e a rampa subindo em azul: os cards ficam nitidamente
    // azuis sobre ele. O amarelo, no escuro, vai para as barras.
    surfaceDim = Color(0xFF0D141D),
    surfaceBright = Color(0xFF33455E),
    surfaceContainerLowest = Color(0xFF0A0F16),
    surfaceContainerLow = Color(0xFF16202E),
    surfaceContainer = Color(0xFF1A2739),
    surfaceContainerHigh = Color(0xFF1F3149),
    surfaceContainerHighest = Color(0xFF24395A),

    outline = Color(0xFF8A94A1),
    outlineVariant = Color(0xFF3F4A57),
    scrim = Color(0xFF000000),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

/**
 * Qual dos dois esquemas está no ar.
 *
 * As funções abaixo liam `isSystemInDarkTheme()` direto, o que ignorava o parâmetro
 * `darkTheme` do [RepostinhoTheme] — com o botão de modo noturno na toolbar, as cores
 * precisam seguir a escolha do morador, não a do sistema.
 */
val LocalDarkTheme = staticCompositionLocalOf { false }

/** Verde de saldo positivo. O Material 3 não tem um slot de "sucesso", então fica aqui. */
@Composable
fun positiveColor(): Color =
    if (LocalDarkTheme.current) Color(0xFF7DD87F) else Color(0xFF2E7D32)

/*
 * Toolbar e menu inferior trocam de cor entre os temas: azul no claro, amarelo no escuro.
 * Como as duas barras leem daqui, elas não têm como divergir uma da outra.
 */

@Composable
fun barContainerColor(): Color =
    if (LocalDarkTheme.current) MaterialTheme.colorScheme.secondary
    else MaterialTheme.colorScheme.primary

@Composable
fun onBarColor(): Color =
    if (LocalDarkTheme.current) MaterialTheme.colorScheme.onSecondary
    else MaterialTheme.colorScheme.onPrimary

/** A pílula do item selecionado inverte junto, senão some dentro da barra. */
@Composable
fun barIndicatorColor(): Color =
    if (LocalDarkTheme.current) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.secondaryContainer

@Composable
fun onBarIndicatorColor(): Color =
    if (LocalDarkTheme.current) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSecondaryContainer

@Composable
fun RepostinhoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content
        )
    }
}
