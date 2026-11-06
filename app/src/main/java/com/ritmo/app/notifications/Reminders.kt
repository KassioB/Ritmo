package com.ritmo.app.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.ritmo.app.MainActivity
import com.ritmo.app.R
import com.ritmo.app.RitmoApp
import com.ritmo.app.data.Snapshot
import com.ritmo.app.domain.ReminderPlan
import com.ritmo.app.domain.Streaks
import com.ritmo.app.domain.dias
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Agenda e dispara os lembretes; as regras de horário ficam em [ReminderPlan]. */
object Reminders {
    private const val CHANNEL = "lembretes"
    private const val PREFS = "lembretes"
    private const val LAST_CHECK = "ultima_verificacao"
    private const val EVENING_ID = 0L

    /** Depois de muito tempo sem verificar (celular desligado), avisos velhos só atrapalham. */
    private const val MAX_LATE_HOURS = 2L

    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Lembretes", NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    /** No Android 12+ tocar na hora exata depende de uma permissão que o usuário concede. */
    fun canBeExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    /**
     * Reprograma o alarme a partir da última verificação, para que um horário ainda não entregue
     * pelo sistema continue pendente. [edited] recomeça de agora: um lembrete recém-criado para
     * um horário que já passou não deve tocar.
     */
    fun sync(context: Context, state: Snapshot, edited: Boolean = false) {
        val now = LocalDateTime.now()
        val last = lastCheck(context)
        val after = if (edited || last == null || last.isAfter(now)) {
            setLastCheck(context, now)
            now
        } else {
            last
        }
        schedule(context, state, after)
    }

    /** Quem saiu da conta deixa de receber avisos até entrar de novo. */
    private fun activeObjectives(state: Snapshot) =
        if (state.settings.remindersEnabled && !state.signedOut) state.objectives else emptyList()

    private fun alertMinutes(state: Snapshot) =
        state.settings.streakAlertMinutes.takeIf { state.settings.streakAlertEnabled && !state.signedOut }

    private fun schedule(context: Context, state: Snapshot, after: LocalDateTime) {
        val next = ReminderPlan.next(after, activeObjectives(state), alertMinutes(state))
        if (next != null) {
            setAlarm(context, next)
        } else {
            context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
        }
    }

    private fun setAlarm(context: Context, at: LocalDateTime) {
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val manager = context.getSystemService(AlarmManager::class.java)
        val operation = pendingIntent(context)
        if (canBeExact(context)) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, operation)
        } else {
            // Sem a permissão, o sistema pode atrasar o disparo em até uma hora.
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, operation)
        }
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun lastCheck(context: Context): LocalDateTime? {
        val millis = prefs(context).getLong(LAST_CHECK, 0L).takeIf { it > 0L } ?: return null
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
    }

    private fun setLastCheck(context: Context, at: LocalDateTime) {
        prefs(context).edit {
            putLong(LAST_CHECK, at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        }
    }

    suspend fun handle(context: Context) {
        val repository = (context.applicationContext as RitmoApp).repository
        val state = repository.load() ?: return
        val now = LocalDateTime.now()
        val oldest = now.minusHours(MAX_LATE_HOURS)
        val from = (lastCheck(context) ?: now).let { if (it.isBefore(oldest)) oldest else it }

        ReminderPlan.between(from, now, activeObjectives(state))
            .filterNot { state.isDone(it.objective.id, it.at.toLocalDate().toEpochDay()) }
            // Vários horários vencidos juntos viram um só aviso por objetivo.
            .distinctBy { it.objective.id }
            .forEach { due ->
                val objective = due.objective
                val streak = Streaks.forObjective(objective.id, state.records).current
                val detail = if (streak > 0) {
                    "Sequência atual: ${dias(streak)}. Não deixe esfriar!"
                } else {
                    "Bora começar hoje?"
                }
                notify(context, objective.id, "${objective.emoji} ${objective.title}", detail)
            }

        val alert = alertMinutes(state)
        if (alert != null && ReminderPlan.alertBetween(from, now, alert)) {
            val today = LocalDate.now().toEpochDay()
            val streak = state.mainStreak(today).current
            if (state.scheduledOn(today).isNotEmpty() && !state.isComplete(today) && streak > 0) {
                notify(
                    context,
                    EVENING_ID,
                    "Seu streak de ${dias(streak)} está em risco!",
                    "Ainda dá tempo de completar o dia.",
                )
            }
        }
        setLastCheck(context, now)
        schedule(context, state, now)
        PetIcon.sync(context, state)
    }

    @SuppressLint("MissingPermission")
    private fun notify(context: Context, id: Long, title: String, text: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify(
            id.toInt(),
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Reminders.handle(context)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Alarmes não sobrevivem a reinícios, atualizações do app ou troca de fuso. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = (context.applicationContext as RitmoApp).repository.load()
                if (state != null) {
                    Reminders.sync(context, state)
                    PetIcon.sync(context, state)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
