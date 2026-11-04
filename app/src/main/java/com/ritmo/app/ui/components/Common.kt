package com.ritmo.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ritmo.app.ui.theme.Flame
import com.ritmo.app.ui.theme.Gold
import com.ritmo.app.ui.theme.Ice
import com.ritmo.app.ui.theme.ObjectiveColors
import kotlin.random.Random

@Composable
fun StatChip(icon: ImageVector, tint: Color, value: String, label: String) {
    Row(
        Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun WalletRow(streak: Int, coins: Int, freezes: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatChip(Icons.Filled.LocalFireDepartment, Flame, "$streak", "Streak principal")
        StatChip(Icons.Filled.AcUnit, Ice, "$freezes", "Bloqueios")
        StatChip(Icons.Filled.MonetizationOn, Gold, "$coins", "Moedas")
    }
}

data class Celebration(
    val title: String,
    val message: String,
    val confetti: Boolean = true,
    val mood: Mood = Mood.EXCITED,
)

@Composable
fun CelebrationDialog(celebration: Celebration, accessory: String?, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(contentAlignment = Alignment.Center) {
            Card(shape = MaterialTheme.shapes.large) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Faisca(celebration.mood, accessory, sizeDp = 120.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        celebration.title,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(celebration.message, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Continuar", fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (celebration.confetti) Confetti(Modifier.matchParentSize())
        }
    }
}

private data class Particle(val x: Float, val delay: Float, val speed: Float, val spin: Float, val color: Color)

@Composable
private fun Confetti(modifier: Modifier = Modifier) {
    val particles = remember {
        val colors = ObjectiveColors + Gold
        List(70) {
            Particle(
                x = Random.nextFloat(),
                delay = Random.nextFloat() * 0.35f,
                speed = 0.7f + Random.nextFloat() * 0.6f,
                spin = Random.nextFloat() * 720f,
                color = colors.random(),
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(2200, easing = LinearEasing))
    }
    Canvas(modifier) {
        val t = progress.value
        particles.forEach { p ->
            val local = ((t - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (local > 0f && local < 1f) {
                val center = Offset(p.x * size.width, local * p.speed * size.height)
                rotate(p.spin * local, center) {
                    drawRect(
                        p.color.copy(alpha = 1f - local * local),
                        topLeft = center - Offset(5f, 8f),
                        size = Size(10f, 16f),
                    )
                }
            }
        }
    }
}

/** Pílula "+20 XP" mostrada ao concluir um objetivo. */
@Composable
fun RewardPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(MaterialTheme.colorScheme.inverseSurface, CircleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun EmptyMessage(title: String, message: String, accessory: String?, mood: Mood = Mood.SLEEPY) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Faisca(mood, accessory, sizeDp = 130.dp)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, textAlign = TextAlign.Center)
    }
}
