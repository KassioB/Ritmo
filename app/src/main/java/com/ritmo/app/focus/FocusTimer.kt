package com.ritmo.app.focus

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.ritmo.app.MainActivity
import com.ritmo.app.R
import com.ritmo.app.RitmoApp
import com.ritmo.app.data.RitmoRepository
import com.ritmo.app.data.ToggleOutcome
import com.ritmo.app.domain.FinishedPhase
import com.ritmo.app.domain.Focus
import com.ritmo.app.domain.FocusClock
import com.ritmo.app.domain.FocusDurations
import com.ritmo.app.domain.FocusKind
import com.ritmo.app.domain.FocusMode
import com.ritmo.app.domain.FocusPhase
import com.ritmo.app.domain.FocusTarget
import com.ritmo.app.notifications.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/** Uma fase que terminou e o que ela rendeu (só o foco rende). */
data class FocusOutcome(val finished: FinishedPhase, val reward: ToggleOutcome?)

/**
 * O relógio do foco vivo no app inteiro, não na tela: continua contando com o app em segundo plano e,
 * como só guarda instantes, também depois de o sistema fechar o app. Um alarme toca no fim de cada
 * fase do Pomodoro; quem fecha a fase primeiro (a espera aqui dentro ou o alarme) registra e avisa.
 */
class FocusTimer(
    private val context: Context,
    private val repository: RitmoRepository,
    private val scope: CoroutineScope,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val lock = Mutex()
    private val state = MutableStateFlow(read())
    private val outcomes = MutableSharedFlow<FocusOutcome>(extraBufferCapacity = 4)
    private var waiter: Job? = null

    val clock: StateFlow<FocusClock> = state.asStateFlow()

    /** Fases que terminaram com a tela aberta, para ela comemorar. */
    val finished: SharedFlow<FocusOutcome> = outcomes.asSharedFlow()

    init {
        scope.launch { settle() }
    }

    suspend fun start() = change { it.start(now()) }

    suspend fun pause() = change { it.pause(now()) }

    /** Abandona a fase: um foco encerrado antes da hora não vira sessão. */
    suspend fun stop() = change { it.stop() }

    suspend fun setMode(mode: FocusMode) = change { it.withMode(mode) }

    suspend fun setTarget(target: FocusTarget?) = change { it.withTarget(target) }

    /** Sair da conta limpa tudo: a próxima pessoa não herda o relógio de outra. */
    suspend fun clear() = change { FocusClock() }

    /** Encerra o cronômetro e registra o tempo, se passou de um minuto. */
    suspend fun finishStopwatch() {
        val outcome = lock.withLock {
            val (next, finished) = state.value.finish(now())
            write(next)
            finished?.let { FocusOutcome(it, record(it, next.target)) }
        }
        arm()
        outcome?.let { outcomes.emit(it) }
    }

    /** Fecha a fase do Pomodoro que já terminou, se houver. Seguro de chamar a qualquer momento. */
    suspend fun settle() {
        val outcome = lock.withLock {
            val (next, finished) = state.value.settle(now(), durations())
            if (finished == null) return@withLock null
            write(next)
            val reward = if (finished.phase == FocusPhase.FOCUS) record(finished, next.target) else null
            notifyEnd(finished, next.target)
            FocusOutcome(finished, reward)
        }
        arm()
        outcome?.let { outcomes.emit(it) }
    }

    private suspend fun change(transform: (FocusClock) -> FocusClock) {
        lock.withLock { write(transform(state.value)) }
        arm()
    }

    private suspend fun record(finished: FinishedPhase, target: FocusTarget?): ToggleOutcome =
        repository.recordFocus(finished, target?.label, LocalDate.now().toEpochDay())

    private suspend fun durations(): FocusDurations {
        val settings = repository.load()?.settings ?: return FocusDurations()
        return FocusDurations(settings.focusMinutes, settings.breakMinutes)
    }

    /** Agenda o fim da fase: uma espera enquanto o app vive e um alarme para quando ele não estiver vivo. */
    private suspend fun arm() {
        waiter?.cancel()
        val endsAt = state.value.endsAt(durations())
        val alarms = context.getSystemService(AlarmManager::class.java)
        val operation = alarmIntent()
        if (endsAt == null) {
            alarms.cancel(operation)
            return
        }
        if (Reminders.canBeExact(context)) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAt, operation)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAt, operation)
        }
        waiter = scope.launch {
            delay((endsAt - now()).coerceAtLeast(0))
            settle()
        }
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST,
        Intent(context, FocusAlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    @SuppressLint("MissingPermission")
    private fun notifyEnd(finished: FinishedPhase, target: FocusTarget?) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val (title, text) = if (finished.phase == FocusPhase.FOCUS) {
            val minutes = Focus.minutes((finished.millis / 1000).toInt())
            "Foco concluído! +${Focus.xpFor((finished.millis / 1000).toInt())} XP" to
                (target?.let { "$minutes em ${it.title}. Hora de uma pausa." } ?: "$minutes de foco. Hora de uma pausa.")
        } else {
            "A pausa acabou" to "Bora voltar ao foco?"
        }
        val open = PendingIntent.getActivity(
            context,
            ALARM_REQUEST,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(open)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .build(),
        )
    }

    private fun now() = System.currentTimeMillis()

    private fun read(): FocusClock {
        fun long(key: String) = prefs.getLong(key, NONE).takeIf { it != NONE }
        val target = long(TARGET_ID)?.let { id ->
            val kind = FocusKind.entries.firstOrNull { it.name == prefs.getString(TARGET_KIND, null) }
            val title = prefs.getString(TARGET_TITLE, null)
            if (kind != null && title != null) FocusTarget(kind, id, title, prefs.getString(TARGET_EMOJI, null)) else null
        }
        return FocusClock(
            mode = FocusMode.entries.firstOrNull { it.name == prefs.getString(MODE, null) } ?: FocusMode.POMODORO,
            phase = FocusPhase.entries.firstOrNull { it.name == prefs.getString(PHASE, null) } ?: FocusPhase.FOCUS,
            runningSince = long(RUNNING_SINCE),
            banked = prefs.getLong(BANKED, 0),
            startedAt = long(STARTED_AT),
            target = target,
        )
    }

    private fun write(clock: FocusClock) {
        state.value = clock
        prefs.edit {
            putString(MODE, clock.mode.name)
            putString(PHASE, clock.phase.name)
            putLong(RUNNING_SINCE, clock.runningSince ?: NONE)
            putLong(BANKED, clock.banked)
            putLong(STARTED_AT, clock.startedAt ?: NONE)
            putLong(TARGET_ID, clock.target?.id ?: NONE)
            putString(TARGET_KIND, clock.target?.kind?.name)
            putString(TARGET_TITLE, clock.target?.title)
            putString(TARGET_EMOJI, clock.target?.emoji)
        }
    }

    companion object {
        private const val CHANNEL = "foco"
        private const val PREFS = "foco"
        private const val NOTIFICATION_ID = -1
        private const val ALARM_REQUEST = 7
        private const val NONE = Long.MIN_VALUE
        private const val MODE = "modo"
        private const val PHASE = "fase"
        private const val RUNNING_SINCE = "correndo_desde"
        private const val BANKED = "acumulado"
        private const val STARTED_AT = "inicio"
        private const val TARGET_ID = "alvo_id"
        private const val TARGET_KIND = "alvo_tipo"
        private const val TARGET_TITLE = "alvo_titulo"
        private const val TARGET_EMOJI = "alvo_emoji"

        /** Importância alta: o fim do Pomodoro precisa tocar mesmo com o celular no bolso. */
        fun createChannel(context: Context) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "Foco", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Avisa quando o Pomodoro ou a pausa terminam"
                },
            )
        }
    }
}

/** O alarme do fim da fase: acorda o app, se preciso, para fechar a fase e avisar. */
class FocusAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                (context.applicationContext as RitmoApp).focus.settle()
            } finally {
                pending.finish()
            }
        }
    }
}
