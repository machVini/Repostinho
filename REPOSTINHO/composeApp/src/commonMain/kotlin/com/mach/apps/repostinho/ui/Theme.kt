package com.mach.apps.repostinho.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.mach.apps.repostinho.data.model.EventCategory

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

    // Amarelos dessaturados: no escuro, o tom do brasão vira néon e cansa a vista. Estes
    // puxam para o ouro velho e continuam lendo como amarelo ao lado do azul.
    secondary = Color(0xFFD8BC70),
    onSecondary = Color(0xFF3A2E08),
    secondaryContainer = Color(0xFF4A3D16),
    onSecondaryContainer = Color(0xFFEFDCA8),

    tertiary = Color(0xFFC9B078),
    onTertiary = Color(0xFF362B10),
    tertiaryContainer = Color(0xFF4C3F1C),
    onTertiaryContainer = Color(0xFFEBDCBC),

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
    // azuis sobre ele.
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
 * Toolbar e menu inferior: azul da rep no claro, azul-noite no escuro. Como as duas
 * barras leem daqui, elas não têm como divergir uma da outra.
 *
 * No escuro as barras já foram amarelas — o azul do brasão inteiro virava um bloco cítrico
 * em cima e embaixo da tela. O amarelo continua presente, mas como acento: na pílula do
 * item selecionado e nos cards de tarefa e evento.
 */

/** Um degrau acima do fundo, para a barra se destacar sem virar outro bloco de cor. */
private val NightBar = Color(0xFF16263F)
private val OnNightBar = Color(0xFFE3EAF5)

@Composable
fun barContainerColor(): Color =
    if (LocalDarkTheme.current) NightBar else MaterialTheme.colorScheme.primary

@Composable
fun onBarColor(): Color =
    if (LocalDarkTheme.current) OnNightBar else MaterialTheme.colorScheme.onPrimary

/**
 * A pílula do item selecionado.
 *
 * Amarela nos dois temas — é ela que dá o acento sobre o azul, e no escuro passou a ser o
 * lugar principal onde o amarelo aparece. Não precisa mais inverter por tema.
 */
@Composable
fun barIndicatorColor(): Color = MaterialTheme.colorScheme.secondaryContainer

/**
 * Acento de destaque: ouro no escuro, azul no claro.
 *
 * A regra do app é "ouro marca o que está selecionado ou é seu; azul carrega estrutura e
 * navegação". No claro o azul já é o destaque natural sobre o creme; no escuro tudo é
 * azul, então o ouro é o que separa. É esta função que dá amarelo a todas as telas no
 * modo noturno sem espalhar cor sem critério.
 */
@Composable
fun accentColor(): Color =
    if (LocalDarkTheme.current) MaterialTheme.colorScheme.secondary
    else MaterialTheme.colorScheme.primary

@Composable
fun onBarIndicatorColor(): Color = MaterialTheme.colorScheme.onSecondaryContainer

/*
 * A tela de login é a única com fundo colorido de ponta a ponta.
 *
 * Ela não tem barra, nem card, nem conteúdo da rep — é só a marca e dois campos. Por isso
 * as cores dela ficam aqui e não saem do `colorScheme`: o azul do app é o das barras, e
 * ocupando a tela inteira ele fica pesado demais atrás de um formulário.
 */

/** Azul mais suave que o das barras, para o fundo não competir com os campos. */
private val LoginBlue = Color(0xFF3D82DE)

@Composable
fun loginBackgroundColor(): Color =
    if (LocalDarkTheme.current) MaterialTheme.colorScheme.background else LoginBlue

/**
 * O botão de entrar: amarelo no claro, ouro velho no escuro.
 *
 * Segue a mesma regra do resto do app — o ouro do brasão vira néon sobre fundo escuro, e
 * o tom dessaturado do tema escuro é o que continua lendo como amarelo sem cansar.
 */
@Composable
fun loginButtonColor(): Color =
    if (LocalDarkTheme.current) MaterialTheme.colorScheme.secondary else RepYellow

/** Texto sobre o botão: escuro nos dois temas, porque os dois fundos são claros. */
@Composable
fun onLoginButtonColor(): Color =
    if (LocalDarkTheme.current) MaterialTheme.colorScheme.onSecondary else RepBronze

/** Sobre o azul e sobre o azul-noite, o branco é o que lê nos dois. */
@Composable
fun onLoginBackgroundColor(): Color = Color.White

/*
 * Cores das categorias da agenda.
 *
 * São dois tons por categoria porque os cards mudam de fundo com o tema: no claro eles são
 * creme, e a cor precisa ser escura e saturada; no escuro são azuis, e a mesma cor escura
 * sumiria. Nenhuma delas é o ouro nem o azul do app — esses dois já têm significado
 * ("é seu" e "estrutura"), e reaproveitá-los para categoria embaralharia a leitura.
 */
private val EventPink = Color(0xFFB3236B) to Color(0xFFFFA8CE)
private val EventAmber = Color(0xFF9A5B00) to Color(0xFFFFC078)
private val EventGreen = Color(0xFF2E7D32) to Color(0xFF7DD87F)
private val EventViolet = Color(0xFF6A3FB5) to Color(0xFFCBB2FF)

/**
 * A cor de uma categoria da agenda no tema atual.
 *
 * O aniversário da rep não usa isto: ele se destaca pelo card inteiro pintado, e não pela
 * cor do texto. Ouro sobre o creme do tema claro seria ilegível, que é a mesma razão de
 * [accentColor] virar azul no claro.
 */
@Composable
fun eventCategoryColor(category: EventCategory): Color {
    val (light, dark) = when (category) {
        EventCategory.ANIVERSARIO -> EventPink
        EventCategory.REP -> EventAmber
        EventCategory.ROLE -> EventGreen
        EventCategory.ARU -> EventViolet
    }
    return if (LocalDarkTheme.current) dark else light
}

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
