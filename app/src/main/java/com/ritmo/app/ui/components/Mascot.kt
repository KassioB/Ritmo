package com.ritmo.app.ui.components

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ritmo.app.ui.theme.Blue
import com.ritmo.app.ui.theme.Frost
import com.ritmo.app.ui.theme.Mint
import com.ritmo.app.ui.theme.Purple
import kotlin.math.PI
import kotlin.math.sin

enum class Mood { HAPPY, EXCITED, WORRIED, SAD, SLEEPY }

private const val LOOP_MS = 4800
private val Ink = Color(0xFF16103D)
private val Base = Offset(50f, 96f)

private class Flames(
    val outer: List<Color>,
    val middle: List<Color>,
    val core: List<Color>,
    val glow: Color,
    val ember: Color,
)

private val Lively = Flames(
    outer = listOf(Color(0xFFC3B8FF), Purple, Color(0xFF4F3BD6)),
    middle = listOf(Color(0xFF7CB2FF), Blue),
    core = listOf(Color(0xFFD6FFF3), Mint),
    glow = Purple,
    ember = Mint,
)

private val Dim = Flames(
    outer = listOf(Color(0xFF8F8BB8), Color(0xFF5E5A94), Color(0xFF3C3873)),
    middle = listOf(Color(0xFF7B86B5), Color(0xFF4E5A94)),
    core = listOf(Color(0xFFCDD6EA), Color(0xFF8FA0C4)),
    glow = Color(0xFF5E5A94),
    ember = Color(0xFF8FA0C4),
)

/** Onda periódica de [t] (0 a 1) que fecha o ciclo sem emendas, com [cycles] voltas. */
private fun wave(t: Float, cycles: Int, phase: Float = 0f) =
    sin(2f * PI.toFloat() * (cycles * t + phase))

/**
 * Faísca, a mascote do Ritmo: uma chama viva com rosto, em azul e roxo, com o coração menta.
 * Desenhada em uma grade de 100 x 100 unidades, em três camadas que tremulam fora de fase.
 */
@Composable
fun Faisca(
    mood: Mood,
    accessory: String?,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 96.dp,
) {
    val transition = rememberInfiniteTransition(label = "faisca")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(LOOP_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "tempo",
    )
    Canvas(modifier.size(sizeDp).semantics { contentDescription = "Faísca, a mascote" }) {
        val unit = size.width / 100f
        scale(unit, unit, pivot = Offset.Zero) {
            faisca(time, mood, accessory)
        }
    }
}

private fun DrawScope.faisca(t: Float, mood: Mood, accessory: String?) {
    val low = mood == Mood.SAD || mood == Mood.SLEEPY
    val colors = if (low) Dim else Lively
    val energy = when {
        mood == Mood.EXCITED -> 1.8f
        low -> 0.45f
        else -> 1f
    }
    val beat = if (mood == Mood.EXCITED) 6 else 3
    val bob = wave(t, beat) * if (mood == Mood.EXCITED) 2.2f else 1.2f
    val breath = wave(t, beat, 0.15f) * 0.018f

    drawCircle(
        Brush.radialGradient(
            listOf(colors.glow.copy(alpha = 0.34f), Color.Transparent),
            center = Offset(50f, 60f),
            radius = 54f,
        ),
        radius = 54f,
        center = Offset(50f, 60f),
    )
    if (!low) embers(t, colors.ember)

    withTransform({
        translate(top = bob)
        scale(1f - breath, (if (low) 0.84f else 1f) + breath, pivot = Base)
    }) {
        layer(outerFlame(t, energy), colors.outer, 1f, 1f)
        layer(middleFlame(t, energy), colors.middle, 0.8f, 0.8f)
        layer(core(t, energy), colors.core, 0.52f, 0.5f)
        eyes(t, mood)
        mouth(t, mood)
        accessory(accessory)
    }
}

private fun DrawScope.layer(path: Path, colors: List<Color>, scaleX: Float, scaleY: Float) {
    withTransform({ scale(scaleX, scaleY, pivot = Base) }) {
        drawPath(path, Brush.verticalGradient(colors, startY = 2f, endY = 96f))
    }
}

private fun tipX(t: Float, tip: Int, shift: Float, energy: Float) =
    wave(t, 4, shift + tip * 0.17f) * 2.8f * energy

private fun tipY(t: Float, tip: Int, shift: Float, energy: Float) =
    wave(t, 3, shift + tip * 0.29f) * 2.2f * energy

/** Silhueta: base redonda e cinco línguas de fogo, com as pontas soltas para tremular. */
private fun outerFlame(t: Float, energy: Float): Path {
    fun dx(tip: Int) = tipX(t, tip, 0f, energy)
    fun dy(tip: Int) = tipY(t, tip, 0f, energy)
    return Path().apply {
        moveTo(50f, 96f)
        cubicTo(72f, 96f, 88f, 80f, 86f, 60f)
        cubicTo(85f, 53f, 88f, 47f, 94f + dx(0), 38f + dy(0))
        cubicTo(85f, 41f, 79f, 44f, 76f, 47f)
        cubicTo(78f, 37f, 75f, 27f, 80f + dx(1), 16f + dy(1))
        cubicTo(69f, 22f, 63f, 30f, 60f, 39f)
        cubicTo(62f, 27f, 55f, 14f, 52f + dx(2), 1f + dy(2))
        cubicTo(46f, 12f, 40f, 25f, 40f, 37f)
        cubicTo(36f, 31f, 30f, 24f, 25f + dx(3), 14f + dy(3))
        cubicTo(23f, 27f, 25f, 38f, 26f, 47f)
        cubicTo(22f, 43f, 14f, 40f, 6f + dx(4), 36f + dy(4))
        cubicTo(11f, 45f, 15f, 52f, 14f, 60f)
        cubicTo(12f, 80f, 28f, 96f, 50f, 96f)
        close()
    }
}

private fun middleFlame(t: Float, energy: Float): Path {
    fun dx(tip: Int) = tipX(t, tip, 0.33f, energy)
    fun dy(tip: Int) = tipY(t, tip, 0.33f, energy)
    return Path().apply {
        moveTo(50f, 96f)
        cubicTo(72f, 96f, 86f, 82f, 84f, 62f)
        cubicTo(83f, 50f, 76f, 42f, 78f + dx(0), 22f + dy(0))
        cubicTo(70f, 30f, 62f, 36f, 60f, 44f)
        cubicTo(60f, 30f, 54f, 16f, 50f + dx(1), 0f + dy(1))
        cubicTo(46f, 16f, 40f, 30f, 40f, 44f)
        cubicTo(38f, 36f, 30f, 30f, 22f + dx(2), 22f + dy(2))
        cubicTo(24f, 42f, 17f, 50f, 16f, 62f)
        cubicTo(14f, 82f, 28f, 96f, 50f, 96f)
        close()
    }
}

/** O coração da chama é uma gota lisa, para não competir com o rosto. */
private fun core(t: Float, energy: Float): Path {
    val dx = tipX(t, 0, 0.66f, energy)
    val dy = tipY(t, 0, 0.66f, energy)
    return Path().apply {
        moveTo(50f, 96f)
        cubicTo(72f, 96f, 84f, 84f, 82f, 66f)
        cubicTo(80f, 48f, 58f, 34f, 50f + dx, 4f + dy)
        cubicTo(42f, 34f, 20f, 48f, 18f, 66f)
        cubicTo(16f, 84f, 28f, 96f, 50f, 96f)
        close()
    }
}

/** Fagulhas que sobem pelas laterais e somem. */
private fun DrawScope.embers(t: Float, color: Color) {
    listOf(0f to 13f, 0.31f to 87f, 0.58f to 30f, 0.83f to 72f).forEach { (phase, x) ->
        val life = (t * 2f + phase) % 1f
        drawCircle(
            color.copy(alpha = (1f - life) * 0.9f),
            radius = 2.1f - life * 1.2f,
            center = Offset(x + wave(t, 3, phase) * 3f, 30f - life * 30f),
        )
    }
}

private fun DrawScope.eyes(t: Float, mood: Mood) {
    val blinking = t in 0.44f..0.47f || t in 0.91f..0.94f
    val look = wave(t, 1) * 1.1f
    listOf(36f, 64f).forEachIndexed { side, x ->
        if (mood == Mood.SLEEPY || blinking) {
            drawArc(
                color = Ink,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(x - 7f, 55f),
                size = Size(14f, 8f),
                style = Stroke(2.8f, cap = StrokeCap.Round),
            )
        } else {
            val width = if (mood == Mood.EXCITED) 10.5f else 9.5f
            val height = width * 1.18f
            drawOval(Color.White, Offset(x - width, 58f - height), Size(width * 2f, height * 2f))
            val inward = if (side == 0) 1.2f else -1.2f
            val pupil = when (mood) {
                Mood.WORRIED -> 3.2f
                Mood.EXCITED -> 5.2f
                else -> 4.4f
            }
            val center = Offset(x + inward + look, 59.5f)
            drawCircle(Ink, pupil, center)
            drawCircle(Color.White, pupil * 0.38f, center + Offset(1.4f, -1.6f))
        }
    }
    if (mood == Mood.WORRIED || mood == Mood.SAD) {
        drawLine(Ink, Offset(27f, 45f), Offset(41f, 41f), 3f, StrokeCap.Round)
        drawLine(Ink, Offset(73f, 45f), Offset(59f, 41f), 3f, StrokeCap.Round)
    }
}

private fun DrawScope.mouth(t: Float, mood: Mood) {
    val line = Stroke(3f, cap = StrokeCap.Round)
    when (mood) {
        Mood.HAPPY -> openMouth(Offset(38f, 72f), Size(24f, 16f + wave(t, 3) * 1.2f))
        Mood.EXCITED -> openMouth(Offset(33f, 71f), Size(34f, 21f + wave(t, 6) * 2f))
        Mood.WORRIED -> {
            val wobble = Path().apply {
                moveTo(41f, 79f)
                quadraticTo(45f, 74f, 50f, 79f)
                quadraticTo(55f, 84f, 59f, 79f)
            }
            drawPath(wobble, Ink, style = line)
        }
        Mood.SAD -> drawArc(Ink, 200f, 140f, false, Offset(40f, 76f), Size(20f, 13f), style = line)
        Mood.SLEEPY -> drawCircle(Ink, 2.6f, Offset(50f, 78f))
    }
}

private fun DrawScope.openMouth(topLeft: Offset, size: Size) {
    val bowl = Size(size.width, size.height * 2f)
    drawArc(Ink, 0f, 180f, true, Offset(topLeft.x, topLeft.y - size.height), bowl)
    drawOval(
        Mint,
        Offset(topLeft.x + size.width * 0.28f, topLeft.y + size.height * 0.46f),
        Size(size.width * 0.44f, size.height * 0.42f),
    )
}

private fun DrawScope.accessory(id: String?) {
    when (id) {
        "acc_glasses" -> {
            val dark = Ink
            listOf(24f, 52f).forEach { x ->
                drawRoundRect(dark, Offset(x, 49f), Size(24f, 17f), CornerRadius(6f))
                drawRoundRect(Frost.copy(alpha = 0.3f), Offset(x + 3f, 52f), Size(9f, 3f), CornerRadius(1.5f))
            }
            drawLine(dark, Offset(46f, 54f), Offset(54f, 54f), 2.6f)
        }
        "acc_party" -> {
            val hat = Path().apply {
                moveTo(50f, -6f)
                lineTo(66f, 26f)
                lineTo(34f, 26f)
                close()
            }
            drawPath(hat, Brush.verticalGradient(listOf(Mint, Blue), startY = -6f, endY = 26f))
            drawCircle(Frost, 4.5f, Offset(50f, -6f))
        }
        "acc_crown" -> {
            val crown = Path().apply {
                moveTo(32f, 32f)
                lineTo(29f, 10f)
                lineTo(41f, 20f)
                lineTo(50f, 4f)
                lineTo(59f, 20f)
                lineTo(71f, 10f)
                lineTo(68f, 32f)
                close()
            }
            drawPath(crown, Mint)
            drawCircle(Frost, 3f, Offset(50f, 24f))
        }
    }
}
