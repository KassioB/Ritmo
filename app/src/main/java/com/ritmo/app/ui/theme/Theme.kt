package com.ritmo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Flame = Color(0xFFFF6B2C)
val Gold = Color(0xFFFFC24B)
val Ice = Color(0xFF4FB9EC)
val Success = Color(0xFF2FB463)
val Missed = Color(0xFFE5484D)

/** Paleta dos objetivos. */
val ObjectiveColors = listOf(
    Color(0xFFFF6B2C),
    Color(0xFFF2A516),
    Color(0xFF2FB463),
    Color(0xFF1FA8A0),
    Color(0xFF3D84F5),
    Color(0xFF8B5CF6),
    Color(0xFFE84393),
)

private data class Seed(val light: Color, val dark: Color, val tintLight: Color, val tintDark: Color)

private val seeds = mapOf(
    null to Seed(Color(0xFFC2410C), Color(0xFFFF9A63), Color(0xFFFFE8DB), Color(0xFF4A2412)),
    "theme_ocean" to Seed(Color(0xFF0B63B5), Color(0xFF7CC4FA), Color(0xFFDCEEFC), Color(0xFF0E3350)),
    "theme_forest" to Seed(Color(0xFF1B7A43), Color(0xFF7EDBA3), Color(0xFFDDF4E5), Color(0xFF123C25)),
    "theme_plum" to Seed(Color(0xFF7A2FD0), Color(0xFFC8A2FA), Color(0xFFEFE4FD), Color(0xFF34195C)),
)

private fun scheme(themeId: String?, dark: Boolean): ColorScheme {
    val seed = seeds[themeId] ?: seeds.getValue(null)
    return if (dark) {
        darkColorScheme(
            primary = seed.dark,
            onPrimary = Color(0xFF1C1208),
            primaryContainer = seed.tintDark,
            onPrimaryContainer = Color(0xFFFFF4EC),
            background = Color(0xFF14130F),
            surface = Color(0xFF14130F),
            surfaceContainer = Color(0xFF211F1A),
            surfaceContainerHigh = Color(0xFF2B2922),
        )
    } else {
        lightColorScheme(
            primary = seed.light,
            onPrimary = Color.White,
            primaryContainer = seed.tintLight,
            onPrimaryContainer = Color(0xFF2B1608),
            background = Color(0xFFFFFBF7),
            surface = Color(0xFFFFFBF7),
            surfaceContainer = Color(0xFFF6F0E9),
            surfaceContainerHigh = Color(0xFFEFE8DF),
        )
    }
}

private val typography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
)

private val shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun RitmoTheme(themeId: String?, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme(themeId, isSystemInDarkTheme()),
        typography = typography,
        shapes = shapes,
        content = content,
    )
}
