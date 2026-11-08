package com.ritmo.app.domain

enum class FocusMode(val label: String) {
    POMODORO("Pomodoro"),
    STOPWATCH("Cronômetro"),
}

enum class FocusPhase(val label: String) {
    FOCUS("Foco"),
    BREAK("Pausa"),
}

/** No que a pessoa está focando: um hábito, um item de lista ou uma tarefa da matriz. */
enum class FocusKind { HABIT, ITEM, TASK }

data class FocusTarget(val kind: FocusKind, val id: Long, val title: String, val emoji: String? = null) {
    val label get() = if (emoji.isNullOrBlank()) title else "$emoji $title"
}

data class FocusDurations(val focusMinutes: Int = Focus.DEFAULT_FOCUS, val breakMinutes: Int = Focus.DEFAULT_BREAK) {
    fun of(phase: FocusPhase): Long = FocusClock.MINUTE * if (phase == FocusPhase.FOCUS) focusMinutes else breakMinutes
}

/** Uma fase que terminou: o foco vira sessão registrada; a pausa só avisa. */
data class FinishedPhase(val mode: FocusMode, val phase: FocusPhase, val startedAt: Long, val millis: Long)

/**
 * O relógio do foco, sem nada de Android. Guarda só instantes do relógio de parede, então a contagem
 * continua certa com o app fechado: ao voltar, [settle] fecha a fase que terminou nesse meio-tempo.
 */
data class FocusClock(
    val mode: FocusMode = FocusMode.POMODORO,
    val phase: FocusPhase = FocusPhase.FOCUS,
    /** Quando a contagem voltou a correr; nulo enquanto parada. */
    val runningSince: Long? = null,
    /** Tempo já contado antes de [runningSince]. */
    val banked: Long = 0,
    /** Início da fase, que vira o início da sessão registrada. */
    val startedAt: Long? = null,
    val target: FocusTarget? = null,
) {
    val running get() = runningSince != null

    /** Há uma fase em andamento, correndo ou pausada. */
    val active get() = startedAt != null

    fun elapsed(now: Long): Long = (banked + (runningSince?.let { now - it } ?: 0)).coerceAtLeast(0)

    /** Quanto dura a fase atual; o cronômetro não tem fim. */
    fun length(durations: FocusDurations): Long? =
        if (mode == FocusMode.POMODORO) durations.of(phase) else null

    fun remaining(now: Long, durations: FocusDurations): Long? =
        length(durations)?.let { (it - elapsed(now)).coerceAtLeast(0) }

    /** Quando a fase termina, se estiver correndo; é quando o alarme toca. */
    fun endsAt(durations: FocusDurations): Long? {
        val since = runningSince ?: return null
        val length = length(durations) ?: return null
        return since + length - banked
    }

    fun start(now: Long) = if (running) this else copy(runningSince = now, startedAt = startedAt ?: now)

    fun pause(now: Long) = if (!running) this else copy(banked = elapsed(now), runningSince = null)

    /** Abandona a fase atual. Encerrar uma pausa é pulá-la: volta para o foco. */
    fun stop() = copy(phase = FocusPhase.FOCUS, runningSince = null, banked = 0, startedAt = null)

    /** Trocar de modo só com o relógio parado, para não perder uma sessão sem querer. */
    fun withMode(mode: FocusMode) = if (active) this else copy(mode = mode, phase = FocusPhase.FOCUS)

    fun withTarget(target: FocusTarget?) = copy(target = target)

    /**
     * Fecha a fase do Pomodoro que já acabou. Depois do foco vem a pausa, e depois da pausa o foco,
     * sempre parados: cada fase começa com um toque, como no relógio de cozinha.
     */
    fun settle(now: Long, durations: FocusDurations): Pair<FocusClock, FinishedPhase?> {
        val length = length(durations) ?: return this to null
        val start = startedAt ?: return this to null
        if (!running || elapsed(now) < length) return this to null
        val next = if (phase == FocusPhase.FOCUS) FocusPhase.BREAK else FocusPhase.FOCUS
        val finished = FinishedPhase(mode, phase, start, length)
        return copy(phase = next, runningSince = null, banked = 0, startedAt = null) to finished
    }

    /** O cronômetro termina quando a pessoa decide. Menos de um minuto não vira sessão. */
    fun finish(now: Long): Pair<FocusClock, FinishedPhase?> {
        val start = startedAt ?: return this to null
        val millis = elapsed(now)
        val session = FinishedPhase(mode, phase, start, millis).takeIf { millis >= MINUTE }
        return stop() to session
    }

    companion object {
        const val MINUTE = 60_000L
    }
}

object Focus {
    const val DEFAULT_FOCUS = 25
    const val DEFAULT_BREAK = 5
    val FOCUS_OPTIONS = listOf(15, 20, 25, 30, 45, 50, 60)
    val BREAK_OPTIONS = listOf(3, 5, 10, 15)

    private const val XP_PER_BLOCK = 3
    private const val BLOCK_MINUTES = 5

    /** Um teto por sessão, para um cronômetro esquecido ligado não virar uma fonte de XP. */
    const val MAX_XP = 36

    /** 3 XP a cada 5 minutos: um Pomodoro de 25 rende 15, como um item médio de lista. */
    fun xpFor(seconds: Int): Int = (seconds / 60 / BLOCK_MINUTES * XP_PER_BLOCK).coerceAtMost(MAX_XP)

    /** "25:00" ou, a partir de uma hora, "1:05:09". */
    fun clock(millis: Long): String {
        val total = (millis + 999) / 1000
        val hours = total / 3600
        val minutes = total % 3600 / 60
        val seconds = total % 60
        fun two(value: Long) = value.toString().padStart(2, '0')
        return if (hours > 0) "$hours:${two(minutes)}:${two(seconds)}" else "${two(minutes)}:${two(seconds)}"
    }

    /** "25 min" ou "1 h 10 min". */
    fun minutes(seconds: Int): String {
        val minutes = seconds / 60
        return if (minutes >= 60) {
            val rest = minutes % 60
            if (rest == 0) "${minutes / 60} h" else "${minutes / 60} h $rest min"
        } else {
            "$minutes min"
        }
    }
}
