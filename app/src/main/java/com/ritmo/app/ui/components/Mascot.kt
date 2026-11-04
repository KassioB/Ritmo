package com.ritmo.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class Mood { HAPPY, EXCITED, WORRIED, SAD, SLEEPY }

private val Ink = Color(0xFF3A1D0E)

/** Faísca, a mascote do Ritmo. Desenhada em uma grade de 100 x 100 unidades. */
@Composable
fun Faisca(
    mood: Mood,
    accessory: String?,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 96.dp,
) {
    val transition = rememberInfiniteTransition(label = "faisca")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(if (mood == Mood.EXCITED) 450 else 1500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "bob",
    )
    Canvas(modifier.size(sizeDp).semantics { contentDescription = "Faísca, a mascote" }) {
        val unit = size.width / 100f
        scale(unit, unit, pivot = Offset.Zero) {
            translate(top = 3f - bob * 5f) {
                body(mood)
                eyes(mood)
                mouth(mood)
                accessory(accessory)
            }
        }
    }
}

private fun DrawScope.body(mood: Mood) {
    val outer = Path().apply {
        moveTo(50f, 10f)
        cubicTo(56f, 28f, 84f, 40f, 84f, 66f)
        cubicTo(84f, 84f, 68f, 97f, 50f, 97f)
        cubicTo(32f, 97f, 16f, 84f, 16f, 66f)
        cubicTo(16f, 50f, 26f, 44f, 32f, 32f)
        cubicTo(35f, 42f, 38f, 46f, 43f, 46f)
        cubicTo(46f, 34f, 46f, 22f, 50f, 10f)
        close()
    }
    val colors = if (mood == Mood.SAD || mood == Mood.SLEEPY) {
        listOf(Color(0xFFB9A79C), Color(0xFF8D7C74))
    } else {
        listOf(Color(0xFFFFB347), Color(0xFFFF5A1F))
    }
    drawPath(outer, Brush.verticalGradient(colors, startY = 10f, endY = 97f))
    drawOval(
        color = Color.White.copy(alpha = 0.22f),
        topLeft = Offset(30f, 58f),
        size = Size(40f, 32f),
    )
}

private fun DrawScope.eyes(mood: Mood) {
    listOf(38f, 62f).forEach { x ->
        if (mood == Mood.SLEEPY) {
            drawArc(
                color = Ink,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(x - 5f, 58f),
                size = Size(10f, 6f),
                style = Stroke(2.5f, cap = StrokeCap.Round),
            )
        } else {
            val radius = if (mood == Mood.EXCITED) 7.5f else 6.5f
            drawCircle(Color.White, radius, Offset(x, 62f))
            drawCircle(Ink, radius * 0.58f, Offset(x + 0.8f, 62.6f))
            drawCircle(Color.White, 1.3f, Offset(x + 2.2f, 60.8f))
        }
    }
    if (mood == Mood.WORRIED || mood == Mood.SAD) {
        drawLine(Ink, Offset(31f, 53f), Offset(40f, 50f), 2.4f, StrokeCap.Round)
        drawLine(Ink, Offset(69f, 53f), Offset(60f, 50f), 2.4f, StrokeCap.Round)
    }
}

private fun DrawScope.mouth(mood: Mood) {
    val line = Stroke(2.8f, cap = StrokeCap.Round)
    when (mood) {
        Mood.HAPPY -> drawArc(
            Ink, 20f, 140f, false, Offset(42f, 68f), Size(16f, 12f), style = line,
        )
        Mood.EXCITED -> {
            drawArc(Ink, 0f, 180f, true, Offset(40f, 66f), Size(20f, 20f))
            drawCircle(Color(0xFFFF8A80), 4f, Offset(50f, 81.5f))
        }
        Mood.WORRIED -> drawLine(Ink, Offset(44f, 77f), Offset(56f, 75f), 2.8f, StrokeCap.Round)
        Mood.SAD -> drawArc(
            Ink, 200f, 140f, false, Offset(42f, 74f), Size(16f, 12f), style = line,
        )
        Mood.SLEEPY -> drawCircle(Ink, 2.4f, Offset(50f, 76f))
    }
}

private fun DrawScope.accessory(id: String?) {
    when (id) {
        "acc_glasses" -> {
            val dark = Color(0xFF1A1A1F)
            listOf(26f, 53f).forEach { x ->
                drawRoundRect(dark, Offset(x, 55f), Size(21f, 14f), CornerRadius(5f))
            }
            drawLine(dark, Offset(46f, 60f), Offset(54f, 60f), 2.4f)
        }
        "acc_party" -> {
            val hat = Path().apply {
                moveTo(50f, -4f)
                lineTo(66f, 26f)
                lineTo(34f, 26f)
                close()
            }
            drawPath(hat, Brush.verticalGradient(listOf(Color(0xFF8B5CF6), Color(0xFFE84393))))
            drawCircle(Color(0xFFFFD84D), 4.5f, Offset(50f, -4f))
        }
        "acc_crown" -> {
            val crown = Path().apply {
                moveTo(32f, 30f)
                lineTo(29f, 8f)
                lineTo(41f, 18f)
                lineTo(50f, 2f)
                lineTo(59f, 18f)
                lineTo(71f, 8f)
                lineTo(68f, 30f)
                close()
            }
            drawPath(crown, Color(0xFFFFC92E))
            drawCircle(Color(0xFFE5484D), 3f, Offset(50f, 22f))
        }
    }
}
