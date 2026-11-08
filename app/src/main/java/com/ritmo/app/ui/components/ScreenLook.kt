package com.ritmo.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ritmo.app.domain.Screen
import com.ritmo.app.ui.theme.ObjectiveColors
import com.ritmo.app.ui.theme.Ritmo

/** O ícone de cada tela, igual no rodapé e nas configurações. */
val Screen.icon: ImageVector
    get() = when (this) {
        Screen.TODAY -> Icons.Rounded.Home
        Screen.LISTS -> Icons.Rounded.Checklist
        Screen.FOCUS -> Icons.Rounded.Timer
        Screen.MATRIX -> Icons.Rounded.GridView
        Screen.DASHBOARD -> Icons.Rounded.BarChart
        Screen.PROFILE -> Icons.Rounded.EmojiEvents
        Screen.SHOP -> Icons.Rounded.Storefront
    }

@Composable
fun Screen.tint(): Color = when (this) {
    Screen.TODAY -> MaterialTheme.colorScheme.primary
    Screen.LISTS -> Ritmo.brand.doneText
    Screen.FOCUS -> Ritmo.brand.streak
    Screen.MATRIX -> Ritmo.brand.coin
    Screen.DASHBOARD -> ObjectiveColors[1]
    Screen.PROFILE -> ObjectiveColors[4]
    Screen.SHOP -> ObjectiveColors[5]
}
