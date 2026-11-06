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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ritmo.app.domain.Evolution
import com.ritmo.app.domain.Mood
import com.ritmo.app.domain.Pets
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val LOOP_MS = 4800
private val Ink = Color(0xFF1B123F)
private val Spark = Color(0xFF7FB4FF)
private val Tear = Color(0xFF7CC8FF)
private val Base = Offset(50f, 96f)

/** Onda periódica de [t] (0 a 1) que fecha o ciclo sem emendas, com [cycles] voltas. */
private fun wave(t: Float, cycles: Int, phase: Float = 0f) =
    sin(2f * PI.toFloat() * (cycles * t + phase))

/**
 * O personagem do usuário, desenhado em uma grade de 100 x 100 unidades. Todo pet muda de forma e
 * de cor a cada fase de evolução. Ao mudar um desenho, atualize tools/generate_pet_icons.py junto.
 */
@Composable
fun Mascot(
    pet: String?,
    stage: Int,
    mood: Mood,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 96.dp,
) {
    val transition = rememberInfiniteTransition(label = "mascote")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(LOOP_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "tempo",
    )
    val phase = stage.coerceIn(Evolution.stages.indices)
    val description = "${Pets.byId(pet).name}, fase ${Evolution.stages[phase].name}"
    Canvas(modifier.size(sizeDp).semantics { contentDescription = description }) {
        val unit = size.width / 100f
        scale(unit, unit, pivot = Offset.Zero) {
            when (pet) {
                Pets.RAIO -> raio(time, phase, mood)
                Pets.LUZ -> luz(time, phase, mood)
                Pets.MAROMBA -> maromba(time, phase, mood)
                else -> faisca(time, phase, mood)
            }
        }
    }
}

/** Tudo que o rosto precisa saber; [rest] desenha a boca da expressão normal. */
private class Face(
    val left: Offset,
    val right: Offset,
    val width: Float,
    val height: Float,
    val iris: Float,
    val lookLeft: Float,
    val lookRight: Float,
    val lid: Float,
    val lidColor: Color,
    val irisColor: Color,
    val mouth: Offset,
    val mouthWidth: Float,
    val rest: DrawScope.() -> Unit,
)

private class Look(
    val light: Color,
    val mid: Color,
    val edge: Color,
    val limb: Color,
    val iris: Color,
) {
    val fill get() = Brush.radialGradient(
        0f to light,
        0.55f to mid,
        1f to edge,
        center = Offset(50f, 58f),
        radius = 56f,
    )
}

private val Looks = listOf(
    Look(Color(0xFFC9CBE0), Color(0xFF8C8FB5), Color(0xFF5F6290), Color(0xFF5F6290), Color(0xFF3B3D63)),
    Look(Color(0xFFFFF2A0), Color(0xFFFFC928), Color(0xFFF79A0B), Color(0xFFE88A08), Color(0xFF7A3E12)),
    Look(Color(0xFFFFC27A), Color(0xFFFF6F2C), Color(0xFFE0340F), Color(0xFFD93A0C), Color(0xFF6E2A10)),
    Look(Color(0xFFEDB2FF), Color(0xFFB63BE6), Color(0xFF7A1DC4), Color(0xFF8E25D0), Color(0xFF4E1A86)),
    Look(Color(0xFF9CD0FF), Color(0xFF2F7DFF), Color(0xFF1546C9), Color(0xFF1A4FD4), Color(0xFF123A9E)),
    Look(Color(0xFFC9BEFF), Color(0xFF6F5BFF), Color(0xFF2C46DC), Color(0xFF3A3BD6), Color(0xFF2A2190)),
)

private val StageScales = listOf(0.60f, 0.68f, 0.76f, 0.87f, 0.95f, 1f)

private fun energyOf(mood: Mood) = when (mood) {
    Mood.EXCITED -> 1.7f
    Mood.SAD, Mood.SLEEPY -> 0.45f
    else -> 1f
}

/** Desloca as pontas da chama em ritmos diferentes, para ela tremular sem repetir o desenho. */
private class Flicker(val t: Float, val energy: Float) {
    fun x(tip: Int) = wave(t, 4, tip * 0.17f) * 1.9f * energy
    fun y(tip: Int) = wave(t, 3, tip * 0.29f) * 1.5f * energy
}

private fun dropBody(f: Flicker) = Path().apply {
    moveTo(50f, 90f)
    cubicTo(30f, 90f, 18f, 76f, 18f, 60f)
    cubicTo(18f, 46f, 28f, 40f, 33f + f.x(0), 29f + f.y(0))
    cubicTo(36f, 34f, 39f, 36f, 42f, 35f)
    cubicTo(43f, 27f, 49f, 20f, 57f + f.x(1), 12f + f.y(1))
    cubicTo(58f, 23f, 67f, 30f, 73f, 39f)
    cubicTo(79f, 45f, 82f, 52f, 82f, 60f)
    cubicTo(82f, 76f, 70f, 90f, 50f, 90f)
    close()
}

private fun hookBody(f: Flicker) = Path().apply {
    moveTo(50f, 91f)
    cubicTo(28f, 91f, 15f, 76f, 16f, 58f)
    cubicTo(17f, 44f, 28f, 37f, 34f + f.x(0), 24f + f.y(0))
    cubicTo(37f, 30f, 41f, 34f, 46f, 33f)
    cubicTo(48f, 23f, 58f, 14f, 74f + f.x(1), 8f + f.y(1))
    cubicTo(69f, 18f, 68f, 26f, 72f, 33f)
    cubicTo(75f, 30f, 78f, 26f, 80f + f.x(2), 20f + f.y(2))
    cubicTo(81f, 33f, 85f, 44f, 84f, 58f)
    cubicTo(84f, 77f, 72f, 91f, 50f, 91f)
    close()
}

private fun tallBody(f: Flicker) = Path().apply {
    moveTo(50f, 93f)
    cubicTo(38f, 93f, 29f, 86f, 26f, 74f)
    cubicTo(23f, 63f, 17f, 53f, 19f, 42f)
    cubicTo(16f, 39f, 13f, 35f, 12f + f.x(0), 28f + f.y(0))
    cubicTo(19f, 32f, 24f, 33f, 28f, 31f)
    cubicTo(27f, 22f, 30f, 13f, 38f + f.x(1), 7f + f.y(1))
    cubicTo(38f, 15f, 42f, 20f, 48f, 20f)
    cubicTo(50f, 13f, 55f, 6f, 64f + f.x(2), 1f + f.y(2))
    cubicTo(62f, 11f, 65f, 17f, 71f, 20f)
    cubicTo(74f, 16f, 78f, 12f, 84f + f.x(3), 10f + f.y(3))
    cubicTo(82f, 18f, 82f, 26f, 85f, 31f)
    cubicTo(87f, 29f, 89f, 26f, 90f + f.x(4), 22f + f.y(4))
    cubicTo(91f, 32f, 87f, 42f, 81f, 50f)
    cubicTo(81f, 60f, 77f, 66f, 74f, 74f)
    cubicTo(71f, 86f, 62f, 93f, 50f, 93f)
    close()
}

/** Preenche e arredonda as pontas: o traço com junção redonda suaviza cada bico. */
private fun DrawScope.soft(path: Path, brush: Brush, edge: Float = 3f) {
    drawPath(path, brush)
    drawPath(path, brush, style = Stroke(edge, join = StrokeJoin.Round))
}

private fun DrawScope.curve(
    x1: Float,
    y1: Float,
    cx: Float,
    cy: Float,
    x2: Float,
    y2: Float,
    width: Float,
    color: Color = Ink,
) {
    val path = Path().apply {
        moveTo(x1, y1)
        quadraticTo(cx, cy, x2, y2)
    }
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private val Shine = Color.White.copy(alpha = 0.55f)

private fun DrawScope.dropShine() = curve(54f, 20f, 49f, 26f, 46f, 34f, 2.2f, Shine)

private fun DrawScope.hookShine() = curve(68f, 14f, 58f, 20f, 52f, 31f, 2.2f, Shine)

private fun DrawScope.tallShine() {
    curve(36f, 14f, 33f, 22f, 34f, 29f, 2.2f, Shine)
    curve(62f, 8f, 59f, 14f, 60f, 20f, 2.2f, Shine)
    curve(16f, 31f, 19f, 36f, 23f, 38f, 2f, Color.White.copy(alpha = 0.45f))
}

private fun DrawScope.limbs(color: Color, t: Float, mood: Mood) {
    listOf(40f to -1f, 60f to 1f).forEach { (x, side) ->
        val leg = Path().apply {
            moveTo(x, 86f)
            cubicTo(x, 92f, x + side, 95f, x + side, 97f)
        }
        drawPath(leg, color, style = Stroke(3f, cap = StrokeCap.Round))
        val foot = Path().apply {
            moveTo(x - 3f * side, 98f)
            quadraticTo(x + side, 94f, x + 6f * side, 98f)
            close()
        }
        drawPath(foot, color)
        drawPath(foot, color, style = Stroke(1.5f, join = StrokeJoin.Round))
    }
    val swing = wave(t, 3) * 1.2f
    listOf(false, true).forEach { mirrored ->
        fun x(value: Float) = if (mirrored) 100f - value else value
        val arm = Path().apply {
            moveTo(x(20f), 62f)
            if (mood == Mood.EXCITED) {
                cubicTo(x(13f), 56f, x(9f), 48f, x(11f + swing), 40f)
            } else {
                cubicTo(x(13f), 66f, x(11f), 74f, x(13f + swing), 80f)
            }
        }
        drawPath(arm, color, style = Stroke(3f, cap = StrokeCap.Round))
        val handY = if (mood == Mood.EXCITED) 39f else 81f
        val handX = if (mood == Mood.EXCITED) 11f else 13f
        drawCircle(color, 2.6f, Offset(x(handX + swing), handY))
    }
}

private fun faceFor(stage: Int): Face {
    val look = Looks[stage]
    return when (stage) {
        0 -> Face(
            Offset(39f, 59f), Offset(61f, 59f), 8.4f, 8.6f, 5.6f, 0.8f, -0.8f, 0.34f, look.mid, look.iris,
            Offset(50f, 75f), 14f,
        ) {
            curve(44f, 74f, 50f, 77f, 56f, 74f, 2.4f)
        }
        1 -> Face(
            Offset(39f, 58f), Offset(61f, 58f), 10f, 11.5f, 7.6f, 0.8f, -0.8f, 0f, look.mid, look.iris,
            Offset(50f, 76f), 16f,
        ) {
            curve(44f, 75f, 50f, 80f, 57f, 73f, 2.4f)
        }
        2 -> Face(
            Offset(37f, 56f), Offset(63f, 56f), 10.5f, 12f, 7.8f, 1.2f, -1.2f, 0f, look.mid, look.iris,
            Offset(50f, 74f), 20f,
        ) {
            val mouth = Path().apply {
                moveTo(42f, 73f)
                quadraticTo(50f, 75f, 58f, 72f)
                quadraticTo(56f, 82f, 49f, 81f)
                quadraticTo(44f, 80f, 42f, 73f)
                close()
            }
            drawPath(mouth, Ink)
        }
        3 -> Face(
            Offset(36f, 55f), Offset(64f, 55f), 11f, 10f, 6.2f, 2.2f, 0.4f, 0.42f, look.mid, look.iris,
            Offset(50f, 74f), 20f,
        ) {
            curve(41f, 74f, 49f, 71f, 60f, 70f, 2.8f)
        }
        4 -> Face(
            Offset(37f, 46f), Offset(63f, 46f), 10.5f, 9.5f, 6.2f, 1.2f, -0.2f, 0.36f, look.mid, look.iris,
            Offset(50f, 64f), 34f,
        ) {
            val mouth = Path().apply {
                moveTo(34f, 62f)
                quadraticTo(50f, 68f, 68f, 60f)
                quadraticTo(65f, 74f, 48f, 73f)
                quadraticTo(37f, 72f, 34f, 62f)
                close()
            }
            drawPath(mouth, Ink)
            val teeth = Path().apply {
                moveTo(36.5f, 63.2f)
                quadraticTo(50f, 68.5f, 65.5f, 61.6f)
                quadraticTo(64f, 65.6f, 61f, 66.4f)
                quadraticTo(50f, 70f, 39.5f, 66.8f)
                close()
            }
            drawPath(teeth, Color.White)
        }
        else -> Face(
            Offset(37f, 45f), Offset(63f, 45f), 10.5f, 10f, 6.6f, 2f, 1f, 0.22f, look.mid, look.iris,
            Offset(52f, 63f), 36f,
        ) {
            sideGrin()
        }
    }
}

private fun DrawScope.sideGrin() {
    val mouth = Path().apply {
        moveTo(35f, 63f)
        quadraticTo(50f, 68f, 71f, 56f)
        quadraticTo(68f, 74f, 50f, 74f)
        quadraticTo(39f, 73f, 35f, 63f)
        close()
    }
    drawPath(mouth, Ink)
    val teeth = Path().apply {
        moveTo(37.5f, 64.2f)
        quadraticTo(50f, 68.5f, 68.5f, 58.4f)
        quadraticTo(67f, 63f, 63.5f, 65.4f)
        quadraticTo(50f, 71f, 41f, 67.5f)
        close()
    }
    drawPath(teeth, Color.White)
    drawPath(star(Offset(63f, 62.5f), 3.4f), Spark)
}

/** Estrela de quatro pontas, o brilho do dente e das faíscas. */
private fun star(center: Offset, radius: Float) = Path().apply {
    val waist = radius * 0.25f
    moveTo(center.x, center.y - radius)
    lineTo(center.x + waist, center.y - waist)
    lineTo(center.x + radius, center.y)
    lineTo(center.x + waist, center.y + waist)
    lineTo(center.x, center.y + radius)
    lineTo(center.x - waist, center.y + waist)
    lineTo(center.x - radius, center.y)
    lineTo(center.x - waist, center.y - waist)
    close()
}

private fun DrawScope.eye(center: Offset, face: Face, irisRadius: Float, look: Float, lid: Float, droop: Float) {
    val rx = face.width
    val ry = face.height
    val outline = Path().apply {
        addOval(Rect(center.x - rx, center.y - ry, center.x + rx, center.y + ry))
    }
    drawPath(outline, Color.White)
    clipPath(outline) {
        val iris = Offset(center.x + look, center.y + 0.6f)
        drawCircle(face.irisColor, irisRadius, iris)
        drawCircle(Ink, irisRadius * 0.55f, iris)
        drawCircle(Color.White, irisRadius * 0.32f, Offset(iris.x + irisRadius * 0.38f, center.y - irisRadius * 0.32f))
        drawCircle(
            Color.White.copy(alpha = 0.85f),
            irisRadius * 0.15f,
            Offset(iris.x - irisRadius * 0.4f, center.y + irisRadius * 0.42f),
        )
        if (lid > 0f) {
            val top = center.y - ry - 1f
            val cut = center.y - ry + 2f * ry * lid
            val shape = Path().apply {
                moveTo(center.x - rx - 1f, cut + droop)
                quadraticTo(center.x, cut - 1.8f, center.x + rx + 1f, cut - droop)
                lineTo(center.x + rx + 1f, top)
                lineTo(center.x - rx - 1f, top)
                close()
            }
            drawPath(shape, face.lidColor)
        }
    }
}

/** Lágrima ou gota de suor, com a ponta para cima. */
private fun DrawScope.teardrop(x: Float, y: Float) {
    val path = Path().apply {
        moveTo(x, y)
        quadraticTo(x - 3.4f, y + 6f, x, y + 7f)
        quadraticTo(x + 3.4f, y + 6f, x, y)
        close()
    }
    drawPath(path, Tear)
}

private fun DrawScope.closedEye(center: Offset, width: Float) =
    curve(center.x - width, center.y, center.x, center.y + 5f, center.x + width, center.y, 2.6f)

private fun DrawScope.expression(face: Face, t: Float, mood: Mood) {
    val blinking = t in 0.44f..0.465f || t in 0.91f..0.935f
    if (mood == Mood.SLEEPY || blinking) {
        closedEye(face.left, face.width * 0.72f)
        closedEye(face.right, face.width * 0.72f)
    } else {
        val lid = when (mood) {
            Mood.EXCITED, Mood.WORRIED -> 0f
            Mood.SAD -> maxOf(face.lid, 0.42f)
            else -> face.lid
        }
        val iris = when (mood) {
            Mood.WORRIED -> face.iris * 0.8f
            Mood.EXCITED -> face.iris * 1.08f
            else -> face.iris
        }
        val droop = if (mood == Mood.SAD) 2.8f else 0f
        eye(face.left, face, iris, face.lookLeft, lid, droop)
        eye(face.right, face, iris, face.lookRight, lid, -droop)
    }

    val m = face.mouth
    val w = face.mouthWidth
    when (mood) {
        Mood.HAPPY -> face.rest(this)
        Mood.EXCITED -> {
            val width = minOf(w * 0.5f, 12f)
            val depth = width * 0.46f
            val smile = Path().apply {
                moveTo(m.x - width / 2f, m.y)
                quadraticTo(m.x, m.y + 1.2f, m.x + width / 2f, m.y)
                quadraticTo(m.x + width * 0.34f, m.y + depth, m.x, m.y + depth)
                quadraticTo(m.x - width * 0.34f, m.y + depth, m.x - width / 2f, m.y)
                close()
            }
            drawPath(smile, Ink)
        }
        Mood.WORRIED -> {
            val wobble = Path().apply {
                moveTo(m.x - w * 0.34f, m.y + 2f)
                quadraticTo(m.x - w * 0.17f, m.y - 2f, m.x, m.y + 2f)
                quadraticTo(m.x + w * 0.17f, m.y + 6f, m.x + w * 0.34f, m.y + 2f)
            }
            drawPath(wobble, Ink, style = Stroke(2.6f, cap = StrokeCap.Round))
            teardrop(face.right.x + face.width + 1.5f, face.right.y - face.height - 2f)
        }
        Mood.SAD -> {
            curve(m.x - w * 0.3f, m.y + 5f, m.x, m.y - 2f, m.x + w * 0.3f, m.y + 5f, 2.8f)
            teardrop(face.left.x - 2f, face.left.y + face.height + 0.5f)
        }
        Mood.SLEEPY -> drawCircle(Ink, 2.2f, Offset(m.x, m.y + 2f))
    }
}

/** Faíscas que sobem, soltas do corpo. */
private fun DrawScope.embers(t: Float, color: Color, count: Int) {
    listOf(0f to 13f, 0.31f to 87f, 0.58f to 28f, 0.83f to 74f, 0.17f to 92f, 0.67f to 8f).take(count)
        .forEach { (phase, x) ->
            val life = (t * 2f + phase) % 1f
            drawCircle(
                color.copy(alpha = (1f - life) * 0.9f),
                radius = 2f - life * 1.1f,
                center = Offset(x + wave(t, 3, phase) * 3f, 34f - life * 34f),
            )
        }
}

private fun DrawScope.aura(color: Color, radius: Float, alpha: Float, center: Offset = Offset(50f, 56f)) {
    drawCircle(
        Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center, radius),
        radius,
        center,
    )
}

private fun DrawScope.stageTransform(t: Float, scale: Float, mood: Mood, block: DrawScope.() -> Unit) {
    val beat = if (mood == Mood.EXCITED) 6 else 3
    val bob = wave(t, beat) * (if (mood == Mood.EXCITED) 1.8f else 0.8f)
    val breath = wave(t, beat, 0.15f) * 0.014f
    withTransform({
        translate(top = bob)
        scale(scale * (1f - breath), scale * (1f + breath), pivot = Base)
    }, block)
}

private fun DrawScope.faisca(t: Float, stage: Int, mood: Mood) {
    val look = Looks[stage]
    val flicker = Flicker(t, energyOf(mood))
    if (stage >= 4) aura(look.mid, 56f, if (stage == 5) 0.42f else 0.3f)
    if (stage >= 2) embers(t, look.light, stage)
    stageTransform(t, StageScales[stage], mood) {
        limbs(look.limb, t, mood)
        val brush = look.fill
        when (stage) {
            0, 1 -> {
                soft(dropBody(flicker), brush)
                dropShine()
            }
            2, 3 -> {
                soft(hookBody(flicker), brush)
                hookShine()
            }
            else -> {
                soft(tallBody(flicker), brush)
                tallShine()
            }
        }
        expression(faceFor(stage), t, mood)
    }
    if (stage == 5) {
        listOf(Offset(9f, 18f) to 0f, Offset(93f, 62f) to 0.4f, Offset(6f, 66f) to 0.7f).forEach { (at, phase) ->
            val pulse = (wave(t, 3, phase) + 1f) / 2f
            drawPath(star(at, 2.5f + pulse * 2.5f), Spark.copy(alpha = 0.5f + pulse * 0.5f))
        }
    }
}

/** Os pets crescem com o streak; forma e cor mudam por fase, nas tabelas de cada um. */
private fun petScale(stage: Int) = 0.76f + stage * 0.048f

private class RaioLook(
    val light: Color,
    val mid: Color,
    val edge: Color,
    val limb: Color,
    val iris: Color,
    val spark: Color,
)

private val RaioLooks = listOf(
    RaioLook(Color(0xFFEFEBD0), Color(0xFFC4BD8C), Color(0xFF8C875C), Color(0xFF8C875C), Color(0xFF55512F), Color(0xFF9FB4D6)),
    RaioLook(Color(0xFFFFFBD6), Color(0xFFFFE45C), Color(0xFFF2B40A), Color(0xFFD99A08), Color(0xFF7A5200), Color(0xFF2F7DFF)),
    RaioLook(Color(0xFFFFF9C9), Color(0xFFFFD21F), Color(0xFFF59A00), Color(0xFFE88A08), Color(0xFF7A4A00), Color(0xFF2F7DFF)),
    RaioLook(Color(0xFFFFEDB0), Color(0xFFFFB01F), Color(0xFFEE6E00), Color(0xFFD85E04), Color(0xFF6E3400), Color(0xFF2F7DFF)),
    RaioLook(Color(0xFFE3F6FF), Color(0xFF5CC8FF), Color(0xFF1E7BF0), Color(0xFF1A63D0), Color(0xFF123A9E), Color(0xFFFFD21F)),
    RaioLook(Color(0xFFF3EBFF), Color(0xFFAE90FF), Color(0xFF5B3BE8), Color(0xFF4A2FD0), Color(0xFF2A2190), Color(0xFF7FE8FF)),
)

/** Começa curto e rechonchudo e termina afiado, com um zigue-zague a mais. */
private val RaioBodies = listOf(
    floatArrayOf(44f, 10f, 74f, 10f, 63f, 31f, 82f, 31f, 41f, 86f, 49f, 55f, 26f, 55f),
    floatArrayOf(43f, 6f, 75f, 6f, 63f, 31f, 83f, 31f, 38f, 91f, 48f, 55f, 25f, 55f),
    floatArrayOf(42f, 3f, 76f, 3f, 63f, 31f, 84f, 31f, 36f, 96f, 47f, 55f, 23f, 55f),
    floatArrayOf(42f, 3f, 76f, 3f, 63f, 31f, 84f, 31f, 36f, 96f, 47f, 55f, 23f, 55f),
    floatArrayOf(40f, 1f, 78f, 1f, 64f, 29f, 87f, 29f, 58f, 61f, 69f, 61f, 33f, 99f, 45f, 55f, 21f, 55f),
    floatArrayOf(40f, 1f, 78f, 1f, 64f, 29f, 87f, 29f, 58f, 61f, 69f, 61f, 33f, 99f, 45f, 55f, 21f, 55f),
)
private val RaioEdges = listOf(7f, 6f, 4f, 4f, 3f, 3f)
private val RaioArcs = listOf(
    listOf(86f to 12f, 80f to 18f, 87f to 20f, 82f to 27f),
    listOf(14f to 30f, 20f to 35f, 13f to 39f, 19f to 45f),
    listOf(74f to 58f, 80f to 62f, 74f to 66f, 80f to 71f),
)

private fun polygon(points: FloatArray) = Path().apply {
    moveTo(points[0], points[1])
    for (index in 2 until points.size step 2) lineTo(points[index], points[index + 1])
    close()
}

/** Raiozinho solto ao redor do corpo, na fase final. */
private fun miniBolt(x: Float, y: Float) = polygon(
    floatArrayOf(x - 2f, y - 9f, x + 5f, y - 9f, x + 1f, y - 2f, x + 6f, y - 2f, x - 4f, y + 10f, x - 1f, y + 1f, x - 5f, y + 1f),
)

private fun raioFace(look: RaioLook) = Face(
    Offset(43f, 33f), Offset(63f, 31f), 8.6f, 8f, 5.2f, 1.6f, 0.6f, 0.24f,
    look.mid, look.iris, Offset(52f, 46f), 26f,
) {
    val mouth = Path().apply {
        moveTo(40f, 45f)
        quadraticTo(50f, 49f, 64f, 42f)
        quadraticTo(61f, 52f, 50f, 52f)
        quadraticTo(43f, 51f, 40f, 45f)
        close()
    }
    drawPath(mouth, Ink)
    val teeth = Path().apply {
        moveTo(42.2f, 45.8f)
        quadraticTo(50f, 49.2f, 61.8f, 43.5f)
        lineTo(60f, 46.6f)
        quadraticTo(50f, 50.4f, 44f, 48f)
        close()
    }
    drawPath(teeth, Color.White)
}

private fun DrawScope.raio(t: Float, stage: Int, mood: Mood) {
    val look = RaioLooks[stage]
    if (stage >= 4) aura(look.mid, 56f, 0.34f)
    stageTransform(t, petScale(stage), mood) {
        RaioArcs.take(stage).forEachIndexed { index, points ->
            val strength = (wave(t, 8, index * 0.3f) + 1.4f) / 2.4f
            val arc = Path().apply {
                moveTo(points[0].first, points[0].second)
                points.drop(1).forEach { (x, y) -> lineTo(x, y) }
            }
            drawPath(
                arc,
                look.spark.copy(alpha = strength.coerceIn(0f, 1f)),
                style = Stroke(2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        if (stage == 5) {
            val strength = ((wave(t, 6) + 1.4f) / 2.4f).coerceIn(0f, 1f)
            drawPath(miniBolt(90f, 50f), look.spark.copy(alpha = strength))
            drawPath(miniBolt(11f, 60f), look.spark.copy(alpha = strength))
        }
        val swing = wave(t, 3) * 1.2f
        val leftArm = Path().apply {
            moveTo(26f, 46f)
            cubicTo(18f, 48f, 13f, 52f, 12f, 58f + swing)
        }
        val rightArm = Path().apply {
            moveTo(72f, 38f)
            cubicTo(80f, 36f, 86f, 31f, 88f, 26f - swing)
        }
        drawPath(leftArm, look.limb, style = Stroke(3f, cap = StrokeCap.Round))
        drawPath(rightArm, look.limb, style = Stroke(3f, cap = StrokeCap.Round))
        drawCircle(look.limb, 2.6f, Offset(12f, 59f + swing))
        drawCircle(look.limb, 2.6f, Offset(88f, 25f - swing))

        soft(
            polygon(RaioBodies[stage]),
            Brush.radialGradient(
                0f to look.light,
                0.55f to look.mid,
                1f to look.edge,
                center = Offset(52f, 38f),
                radius = 60f,
            ),
            edge = RaioEdges[stage],
        )
        curve(50f, 14f, 45f, 18f, 40f, 24f, 2.4f, Color.White.copy(alpha = 0.6f))
        expression(raioFace(look), t, mood)
    }
}

private class LuzLook(
    val mid: Color,
    val edge: Color,
    val limb: Color,
    val iris: Color,
    val ray: Color,
    val cheek: Color,
)

private val LuzLooks = listOf(
    LuzLook(Color(0xFFF1EDD9), Color(0xFFC9C096), Color(0xFFADA47A), Color(0xFF6E6438), Color(0xFFD3CBA4), Color(0xFFD9C9A0)),
    LuzLook(Color(0xFFFFF6C4), Color(0xFFFFD65C), Color(0xFFEDB93A), Color(0xFFC08000), Color(0xFFFFD766), Color(0xFFFFC27A)),
    LuzLook(Color(0xFFFFF0A8), Color(0xFFFFB627), Color(0xFFE9A21E), Color(0xFFB56A00), Color(0xFFFFC233), Color(0xFFFFB25A)),
    LuzLook(Color(0xFFFFE0B8), Color(0xFFFF8F4D), Color(0xFFE8702E), Color(0xFFA8420E), Color(0xFFFF9A52), Color(0xFFFF8A6A)),
    LuzLook(Color(0xFFCFF0FF), Color(0xFF54B8FF), Color(0xFF2F8CE8), Color(0xFF1557B0), Color(0xFF7FD0FF), Color(0xFF7FB4FF)),
    LuzLook(Color(0xFFE8DFFF), Color(0xFFA17BFF), Color(0xFF7A55EE), Color(0xFF4A2FB8), Color(0xFFBFA6FF), Color(0xFFB79BFF)),
)
private val LuzRays = listOf(4, 4, 8, 8, 12, 12)

/** Alcance dos raios pares e ímpares: com poucos raios, todos têm o mesmo tamanho. */
private val LuzReach = listOf(45f to 45f, 49f to 49f, 56f to 41f, 58f to 42f, 60f to 44f, 63f to 45f)

private fun luzFace(look: LuzLook) = Face(
    Offset(39f, 49f), Offset(61f, 49f), 8.6f, 9.6f, 6.2f, 1.2f, -1.2f, 0.2f,
    look.mid, look.iris, Offset(50f, 65f), 20f,
) {
    curve(41f, 65f, 50f, 73f, 60f, 64f, 2.8f)
}

/** Uma luz flutuante: orbe suave com um brilho em estrela que gira devagar, sem pernas. */
private fun DrawScope.luz(t: Float, stage: Int, mood: Mood) {
    val look = LuzLooks[stage]
    val center = Offset(50f, 52f)
    val pulse = (wave(t, 3) + 1f) / 2f
    val glow = when {
        stage == 0 -> 0.3f
        stage >= 4 -> 0.8f
        else -> 0.62f
    }
    aura(look.ray, 50f + stage + pulse * 3f, glow, center)
    stageTransform(t, petScale(stage), mood) {
        val (longReach, shortReach) = LuzReach[stage]
        val count = LuzRays[stage]
        rotate(t * 90f, pivot = center) {
            repeat(count) { index ->
                val angle = index * 2f * PI.toFloat() / count
                val long = index % 2 == 0
                val reach = if (long) longReach + pulse * 4f else shortReach
                val spread = if (long) 0.09f else 0.06f
                val ray = Path().apply {
                    moveTo(center.x + 30f * cos(angle - spread), center.y + 30f * sin(angle - spread))
                    lineTo(center.x + reach * cos(angle), center.y + reach * sin(angle))
                    lineTo(center.x + 30f * cos(angle + spread), center.y + 30f * sin(angle + spread))
                    close()
                }
                drawPath(ray, look.ray)
            }
        }
        if (stage >= 4) drawCircle(look.ray, 38f, center, style = Stroke(2f))
        if (stage == 5) drawCircle(look.ray.copy(alpha = 0.55f), 43f, center, style = Stroke(1.4f))
        listOf(Offset(18f, 16f) to 0f, Offset(85f, 22f) to 0.35f, Offset(84f, 88f) to 0.7f).take(stage).forEach { (at, phase) ->
            val glint = (wave(t, 3, phase) + 1f) / 2f
            drawPath(star(at, 2f + glint * 3f), look.ray.copy(alpha = 0.4f + glint * 0.6f))
        }
        val swing = wave(t, 3) * 1.2f
        listOf(false, true).forEach { mirrored ->
            fun x(value: Float) = if (mirrored) 100f - value else value
            val arm = Path().apply {
                moveTo(x(18f), 60f)
                cubicTo(x(12f), 62f, x(10f), 66f, x(11f), 70f + swing)
            }
            drawPath(arm, look.limb, style = Stroke(3f, cap = StrokeCap.Round))
            drawCircle(look.limb, 2.6f, Offset(x(11f), 71f + swing))
        }
        drawCircle(
            Brush.radialGradient(
                0f to Color.White,
                0.55f to look.mid,
                1f to look.edge,
                center = center,
                radius = 34f,
            ),
            33f,
            center,
        )
        drawOval(look.cheek.copy(alpha = 0.55f), Offset(24f, 58f), Size(10f, 6f))
        drawOval(look.cheek.copy(alpha = 0.55f), Offset(66f, 58f), Size(10f, 6f))
        expression(luzFace(look), t, mood)
    }
}

private class MarombaLook(
    val light: Color,
    val mid: Color,
    val edge: Color,
    val muscle: Color,
    val arm: Color,
    val fist: Color,
    val bulge: Color,
    val highlight: Color,
    val foot: Color,
    val iris: Color,
)

private val MarombaLooks = listOf(
    MarombaLook(
        Color(0xFFDAD7DF), Color(0xFF9A95A8), Color(0xFF66617A), Color(0xFF55506A), Color(0xFF8A85A0),
        Color(0xFF7A7590), Color(0xFFA8A3B8), Color(0xFFE6E3EC), Color(0xFF45405A), Color(0xFF3B3D63),
    ),
    MarombaLook(
        Color(0xFFFFF0A0), Color(0xFFFFA32B), Color(0xFFD9640A), Color(0xFFB4530A), Color(0xFFF08A1A),
        Color(0xFFE0780F), Color(0xFFFFB84D), Color(0xFFFFE9A8), Color(0xFF8A4204), Color(0xFF7A3E12),
    ),
    MarombaLook(
        Color(0xFFFFE08A), Color(0xFFFF5A1F), Color(0xFFB9130A), Color(0xFFA81208), Color(0xFFF0401A),
        Color(0xFFE5340F), Color(0xFFFF7A3D), Color(0xFFFFD08A), Color(0xFF7E0E06), Color(0xFF6E1A08),
    ),
    MarombaLook(
        Color(0xFFFFC8B0), Color(0xFFE8264A), Color(0xFF8E0E3A), Color(0xFF7A0A32), Color(0xFFD81E48),
        Color(0xFFC0183E), Color(0xFFF0506E), Color(0xFFFFC8CE), Color(0xFF5E0826), Color(0xFF5E0A2C),
    ),
    MarombaLook(
        Color(0xFFC8EAFF), Color(0xFF2F8CFF), Color(0xFF1546C9), Color(0xFF123A9E), Color(0xFF2F7DFF),
        Color(0xFF1F63E0), Color(0xFF5CA8FF), Color(0xFFC8EAFF), Color(0xFF0E2E80), Color(0xFF123A9E),
    ),
    MarombaLook(
        Color(0xFFE8DFFF), Color(0xFF7A5BFF), Color(0xFF2C2EC9), Color(0xFF2A2190), Color(0xFF6F5BFF),
        Color(0xFF5B45E8), Color(0xFF9C86FF), Color(0xFFE8DFFF), Color(0xFF211A78), Color(0xFF2A2190),
    ),
)

/** O físico de cada fase; [crown] é quanto a coroa de fogo abaixa enquanto ele ainda é pequeno. */
private class MarombaBuild(
    val width: Float,
    val upper: Float,
    val fore: Float,
    val fist: Float,
    val bulgeX: Float,
    val bulgeY: Float,
    val crown: Float,
)

private val MarombaBuilds = listOf(
    MarombaBuild(0.86f, 8f, 6f, 4.6f, 5.2f, 4f, 10f),
    MarombaBuild(0.92f, 10f, 7f, 5.6f, 6.4f, 5f, 6f),
    MarombaBuild(1f, 13f, 9f, 7f, 8f, 6.6f, 0f),
    MarombaBuild(1f, 14f, 9.5f, 7.2f, 8.6f, 7.2f, 0f),
    MarombaBuild(1.06f, 15f, 10.5f, 7.8f, 9.6f, 8f, 0f),
    MarombaBuild(1.1f, 16.5f, 11.5f, 8.4f, 10.8f, 9f, 0f),
)

private fun marombaFace(look: MarombaLook) = Face(
    Offset(42f, 26f), Offset(58f, 26f), 6.6f, 6f, 3.9f, 1.2f, 0.2f, 0.34f,
    look.mid, look.iris, Offset(50f, 34f), 18f,
) {
    val mouth = Path().apply {
        moveTo(43f, 33.4f)
        quadraticTo(50f, 36.4f, 58f, 32.6f)
        quadraticTo(56.4f, 38.6f, 50f, 38.6f)
        quadraticTo(44.6f, 38.6f, 43f, 33.4f)
        close()
    }
    drawPath(mouth, Ink)
    val teeth = Path().apply {
        moveTo(44.6f, 34.4f)
        quadraticTo(50f, 36.6f, 56.4f, 33.8f)
        lineTo(55.2f, 35.8f)
        quadraticTo(50f, 37.6f, 45.8f, 36f)
        close()
    }
    drawPath(teeth, Color.White)
}

/** Labareda no ombro, nas fases mais fortes. */
private fun shoulderFlame(x: Float, side: Float, shift: Float) = Path().apply {
    val y = 42f
    moveTo(x, y)
    cubicTo(x + 5f * side, y - 3f, x + 8f * side, y - 8f, x + 8f * side + shift, y - 15f)
    cubicTo(x + 3f * side, y - 12f, x, y - 9f, x - 3f * side, y - 4f)
    close()
}

/** O fogo mais forte: pose de bíceps duplo, peitoral e abdômen marcados. */
private fun DrawScope.maromba(t: Float, stage: Int, mood: Mood) {
    val look = MarombaLooks[stage]
    val build = MarombaBuilds[stage]
    val muscle = look.muscle
    val flicker = Flicker(t, energyOf(mood))
    val flex = (wave(t, 3) + 1f) / 2f
    fun tx(value: Float) = 50f + (value - 50f) * build.width
    fun ty(value: Float) = value + build.crown * maxOf(0f, 37f - value) / 37f

    if (stage >= 4) aura(look.mid, 56f, 0.38f)
    if (stage >= 2) embers(t, look.highlight, stage + 1)
    stageTransform(t, petScale(stage), mood) {
        drawLine(muscle, Offset(42f, 86f), Offset(41f, 95f), 8f, StrokeCap.Round)
        drawLine(muscle, Offset(58f, 86f), Offset(59f, 95f), 8f, StrokeCap.Round)
        listOf(34f to 46f, 54f to 66f).forEach { (from, to) ->
            val foot = Path().apply {
                moveTo(from, 99f)
                quadraticTo((from + to) / 2f, 92f, to, 99f)
                close()
            }
            drawPath(foot, look.foot)
            drawPath(foot, look.foot, style = Stroke(2f, join = StrokeJoin.Round))
        }

        val armBrush = Brush.radialGradient(
            0f to look.bulge,
            0.55f to look.arm,
            1f to muscle,
            center = Offset(50f, 40f),
            radius = 62f,
        )
        listOf(false, true).forEach { mirrored ->
            fun x(value: Float) = if (mirrored) 100f - value else value
            fun inner(value: Float) = if (mirrored) 100f - tx(value) else tx(value)
            if (stage >= 4) {
                val side = if (mirrored) 1f else -1f
                drawPath(shoulderFlame(inner(27f) + 3f * side, side, flicker.x(if (mirrored) 3 else 4)), look.bulge)
            }
            val upper = Path().apply {
                moveTo(inner(25f), 47f)
                cubicTo(x(16f), 48f, x(8f), 52f, x(5f), 46f)
            }
            drawPath(upper, armBrush, style = Stroke(build.upper, cap = StrokeCap.Round))
            val fore = Path().apply {
                moveTo(x(4f), 46f)
                cubicTo(x(2f), 40f, x(4f), 34f, x(8f), 29f)
            }
            drawPath(fore, armBrush, style = Stroke(build.fore, cap = StrokeCap.Round))
            drawCircle(look.fist, build.fist, Offset(x(9f), 25f - flex))
            val bulge = build.bulgeY + flex * 1.1f
            drawOval(
                look.bulge,
                Offset(x(14f) - build.bulgeX, 43f - bulge - flex * 0.4f),
                Size(build.bulgeX * 2f, bulge * 2f),
            )
            curve(x(10f), 42f - flex, x(14f), 38f - flex, x(19f), 41f - flex, 1.8f, look.highlight.copy(alpha = 0.8f))
        }

        val torso = Path().apply {
            moveTo(tx(37f), 88f)
            cubicTo(tx(35f), 78f, tx(30f), 68f, tx(24f), 60f)
            cubicTo(tx(19f), 53f, tx(19f), 45f, tx(27f), 41f)
            cubicTo(tx(30f), 39f, tx(33f), 39f, tx(35f), 37f)
            cubicTo(tx(30f), ty(29f), tx(31f), ty(18f), tx(38f) + flicker.x(0), ty(10f) + flicker.y(0))
            cubicTo(tx(38f), ty(17f), tx(41f), ty(20f), tx(46f), ty(20f))
            cubicTo(tx(48f), ty(13f), tx(52f), ty(6f), tx(60f) + flicker.x(1), ty(0f) + flicker.y(1))
            cubicTo(tx(59f), ty(9f), tx(62f), ty(15f), tx(67f), ty(18f))
            cubicTo(tx(69f), ty(15f), tx(72f), ty(12f), tx(76f) + flicker.x(2), ty(10f) + flicker.y(2))
            cubicTo(tx(75f), ty(20f), tx(73f), ty(30f), tx(65f), 37f)
            cubicTo(tx(67f), 39f, tx(70f), 39f, tx(73f), 41f)
            cubicTo(tx(81f), 45f, tx(81f), 53f, tx(76f), 60f)
            cubicTo(tx(70f), 68f, tx(65f), 78f, tx(63f), 88f)
            close()
        }
        soft(
            torso,
            Brush.radialGradient(
                0f to look.light,
                0.55f to look.mid,
                1f to look.edge,
                center = Offset(50f, 54f),
                radius = 60f,
            ),
        )
        curve(tx(29f), 50f, 40f, 60f, 49f, 51f, 2.2f, muscle)
        curve(tx(71f), 50f, 60f, 60f, 51f, 51f, 2.2f, muscle)
        drawLine(muscle, Offset(50f, 52f), Offset(50f, 84f), 1.8f, StrokeCap.Round)
        listOf(64f, 72f).forEach { y ->
            curve(39f, y, 43.5f, y + 2f, 48f, y, 1.8f, muscle)
            curve(52f, y, 56.5f, y + 2f, 61f, y, 1.8f, muscle)
        }
        if (stage >= 4) {
            curve(41f, 80f, 44f, 82f, 48f, 80f, 1.8f, muscle)
            curve(52f, 80f, 56f, 82f, 59f, 80f, 1.8f, muscle)
        }
        expression(marombaFace(look), t, mood)
    }
}
