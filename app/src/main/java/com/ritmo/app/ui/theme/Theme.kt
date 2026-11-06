package com.ritmo.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.ritmo.app.R
import com.ritmo.app.domain.ThemeMode

// Paleta da marca.
val Indigo = Color(0xFF211951)
val Purple = Color(0xFF836FFF)
val Mint = Color(0xFF15F5BA)
val Frost = Color(0xFFF0F3FF)
val Blue = Color(0xFF2F7DFF)

// Variações usadas para relevo e para manter o contraste do texto.
private val DeepIndigo = Color(0xFF140E38)
private val DeepPurple = Color(0xFF6A55F0)
private val SoftPurple = Color(0xFFA99BFF)
private val DeepMint = Color(0xFF0DBE90)
private val InkMint = Color(0xFF00835F)
private val DeepBlue = Color(0xFF1C5FD1)
private val NightBlue = Color(0xFF0B2545)
private val Sky = Color(0xFF7CC8FF)
private val Coral = Color(0xFFFF6B8A)
private val DeepCoral = Color(0xFFD93A63)
private val Ink = Color(0xFF111114)
private val ChartPurple = Color(0xFF8E7DFF)
private val ChartCoral = Color(0xFFE0446C)
private val ChartCoralDark = Color(0xFFEE4F78)

/** Cores dos objetivos e das listas; o índice fica salvo, então novas cores entram no fim. */
val ObjectiveColors = listOf(
    Purple,
    Blue,
    Color(0xFF12C99A),
    Color(0xFF38B6FF),
    Color(0xFFB26BFF),
    Color(0xFFE45CC8),
    Color(0xFF4A3BC7),
    Color(0xFF7C86A8),
)

/** Cores de marca que o Material não modela: faixas, relevos e os números do cabeçalho. */
@Immutable
data class BrandColors(
    val header: Color,
    val onHeader: Color,
    val banner: Color,
    val bannerLip: Color,
    val onBanner: Color,
    val bannerSide: Color,
    val bannerFill: Color,
    val action: Color,
    val actionLip: Color,
    val onAction: Color,
    val streak: Color,
    val freeze: Color,
    val coin: Color,
    val level: Color,
    val done: Color,
    val onDone: Color,
    val doneText: Color,
    val missed: Color,
    /** Par validado para daltonismo em cada tema: não usa verde e vermelho juntos. */
    val chartDone: Color,
    val chartMissed: Color,
)

private data class Look(val scheme: ColorScheme, val brand: BrandColors)

private fun scheme(
    dark: Boolean,
    background: Color,
    card: Color,
    raised: Color,
    text: Color,
    muted: Color,
    line: Color,
    accent: Color,
    onAccent: Color,
    accentTint: Color,
    error: Color,
): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentTint,
        onPrimaryContainer = text,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = accentTint,
        onSecondaryContainer = text,
        tertiary = accent,
        onTertiary = onAccent,
        tertiaryContainer = accentTint,
        onTertiaryContainer = text,
        background = background,
        onBackground = text,
        surface = card,
        onSurface = text,
        surfaceVariant = raised,
        onSurfaceVariant = muted,
        surfaceDim = card,
        surfaceBright = card,
        surfaceContainerLowest = card,
        surfaceContainerLow = card,
        surfaceContainer = card,
        surfaceContainerHigh = raised,
        surfaceContainerHighest = line,
        outline = line,
        outlineVariant = line,
        error = error,
        inverseSurface = text,
        inverseOnSurface = background,
    )
}

private val RitmoLight = Look(
    scheme(
        dark = false,
        background = Frost,
        card = Color.White,
        raised = Color(0xFFE6E9FC),
        text = Indigo,
        muted = Color(0xFF5F5A8C),
        line = Color(0xFFD5D8F5),
        accent = DeepPurple,
        onAccent = Color.White,
        accentTint = Color(0xFFE4DFFF),
        error = DeepCoral,
    ),
    BrandColors(
        header = Indigo,
        onHeader = Color.White,
        banner = Purple,
        bannerLip = DeepPurple,
        onBanner = Color.White,
        bannerSide = Indigo,
        bannerFill = Mint,
        action = Mint,
        actionLip = DeepMint,
        onAction = Indigo,
        streak = SoftPurple,
        freeze = Sky,
        coin = Mint,
        level = Color.White,
        done = Mint,
        onDone = Indigo,
        doneText = InkMint,
        missed = DeepCoral,
        chartDone = DeepPurple,
        chartMissed = ChartCoral,
    ),
)

private val RitmoDark = Look(
    scheme(
        dark = true,
        background = DeepIndigo,
        card = Indigo,
        raised = Color(0xFF2E2570),
        text = Frost,
        muted = Color(0xFFB7B4DE),
        line = Color(0xFF3B3186),
        accent = SoftPurple,
        onAccent = Indigo,
        accentTint = Color(0xFF3A2F8F),
        error = Coral,
    ),
    RitmoLight.brand.copy(
        header = DeepIndigo,
        doneText = Mint,
        missed = Coral,
        chartDone = ChartPurple,
        chartMissed = ChartCoralDark,
    ),
)

private val BlueLight = Look(
    scheme(
        dark = false,
        background = Color(0xFFEDF4FF),
        card = Color.White,
        raised = Color(0xFFDFEBFF),
        text = NightBlue,
        muted = Color(0xFF4F6486),
        line = Color(0xFFCFDFF7),
        accent = DeepBlue,
        onAccent = Color.White,
        accentTint = Color(0xFFD9E7FF),
        error = DeepCoral,
    ),
    BrandColors(
        header = NightBlue,
        onHeader = Color.White,
        banner = Blue,
        bannerLip = DeepBlue,
        onBanner = Color.White,
        bannerSide = NightBlue,
        bannerFill = Color.White,
        action = Blue,
        actionLip = DeepBlue,
        onAction = Color.White,
        streak = Color(0xFF8DBDFF),
        freeze = Sky,
        coin = Color.White,
        level = Color.White,
        done = Blue,
        onDone = Color.White,
        doneText = DeepBlue,
        missed = DeepCoral,
        chartDone = Color(0xFF1F6FEB),
        chartMissed = ChartCoral,
    ),
)

private val BlueDark = Look(
    scheme(
        dark = true,
        background = Color(0xFF06142B),
        card = Color(0xFF0E2444),
        raised = Color(0xFF17345F),
        text = Color(0xFFEAF2FF),
        muted = Color(0xFFA3B8D8),
        line = Color(0xFF234777),
        accent = Color(0xFF7DB0FF),
        onAccent = NightBlue,
        accentTint = Color(0xFF17345F),
        error = Coral,
    ),
    BlueLight.brand.copy(
        header = Color(0xFF06142B),
        doneText = Color(0xFF7DB0FF),
        missed = Coral,
        chartDone = Color(0xFF448CFF),
        chartMissed = ChartCoralDark,
    ),
)

private val White = Look(
    scheme(
        dark = false,
        background = Color.White,
        card = Color(0xFFF5F6FB),
        raised = Color(0xFFEBEDF5),
        text = Ink,
        muted = Color(0xFF5C5F6B),
        line = Color(0xFFE0E2EB),
        accent = DeepPurple,
        onAccent = Color.White,
        accentTint = Color(0xFFECE9FF),
        error = DeepCoral,
    ),
    BrandColors(
        header = Color.White,
        onHeader = Ink,
        banner = Purple,
        bannerLip = DeepPurple,
        onBanner = Color.White,
        bannerSide = Ink,
        bannerFill = Mint,
        action = Ink,
        actionLip = Color(0xFF4A4A52),
        onAction = Color.White,
        streak = DeepPurple,
        freeze = Blue,
        coin = InkMint,
        level = Ink,
        done = Ink,
        onDone = Color.White,
        doneText = Ink,
        missed = DeepCoral,
        chartDone = DeepPurple,
        chartMissed = ChartCoral,
    ),
)

private val Black = Look(
    scheme(
        dark = true,
        background = Color.Black,
        card = Color(0xFF121214),
        raised = Color(0xFF1F1F23),
        text = Color.White,
        muted = Color(0xFFA8A8B3),
        line = Color(0xFF2B2B31),
        accent = Mint,
        onAccent = Color.Black,
        accentTint = Color(0xFF0B3A2E),
        error = Coral,
    ),
    RitmoLight.brand.copy(
        header = Color.Black,
        bannerSide = Color(0xFF1B1B20),
        onAction = Color.Black,
        onDone = Color.Black,
        doneText = Mint,
        missed = Coral,
        chartDone = ChartPurple,
        chartMissed = ChartCoralDark,
    ),
)

private fun look(themeId: String?, dark: Boolean): Look = when (themeId) {
    "theme_ocean" -> if (dark) BlueDark else BlueLight
    "theme_white" -> White
    "theme_black" -> Black
    else -> if (dark) RitmoDark else RitmoLight
}

/** Amostras mostradas na loja para o usuário ver o tema antes de comprar. */
val ThemeSwatches = mapOf(
    "theme_default" to listOf(Indigo, Purple, Frost),
    "theme_ocean" to listOf(NightBlue, Blue, Color.White),
    "theme_white" to listOf(Color.White, Purple, Ink),
    "theme_black" to listOf(Color.Black, Mint, Color.White),
)

@OptIn(ExperimentalTextApi::class)
private fun nunito(weight: Int) = Font(
    R.font.nunito,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val Nunito = FontFamily(nunito(500), nunito(600), nunito(700), nunito(800), nunito(900))

private fun TextStyle.rounded(weight: FontWeight) = copy(fontFamily = Nunito, fontWeight = weight)

private val typography = Typography().run {
    copy(
        displayLarge = displayLarge.rounded(FontWeight.Black),
        displayMedium = displayMedium.rounded(FontWeight.Black),
        displaySmall = displaySmall.rounded(FontWeight.Black),
        headlineLarge = TextStyle(
            fontFamily = Nunito,
            fontWeight = FontWeight.Black,
            fontSize = 30.sp,
            lineHeight = 36.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = Nunito,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            lineHeight = 30.sp,
        ),
        headlineSmall = headlineSmall.rounded(FontWeight.Black),
        titleLarge = TextStyle(
            fontFamily = Nunito,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = Nunito,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        ),
        titleSmall = titleSmall.rounded(FontWeight.ExtraBold),
        bodyLarge = bodyLarge.rounded(FontWeight.SemiBold),
        bodyMedium = bodyMedium.rounded(FontWeight.SemiBold),
        bodySmall = bodySmall.rounded(FontWeight.SemiBold).copy(fontSize = 13.sp),
        labelLarge = labelLarge.rounded(FontWeight.ExtraBold),
        labelMedium = labelMedium.rounded(FontWeight.ExtraBold),
        labelSmall = labelSmall.rounded(FontWeight.ExtraBold),
    )
}

private val shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

private val LocalBrand = staticCompositionLocalOf<BrandColors> { error("Fora do RitmoTheme") }

/** Acesso às cores de marca do tema atual, como `Ritmo.brand.action`. */
object Ritmo {
    val brand: BrandColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBrand.current
}

@Composable
fun RitmoTheme(themeId: String?, mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val look = look(themeId, dark)
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = look.brand.header.luminance() > 0.5f
            isAppearanceLightNavigationBars = look.scheme.surface.luminance() > 0.5f
        }
    }
    CompositionLocalProvider(LocalBrand provides look.brand) {
        MaterialTheme(
            colorScheme = look.scheme,
            typography = typography,
            shapes = shapes,
            content = content,
        )
    }
}
