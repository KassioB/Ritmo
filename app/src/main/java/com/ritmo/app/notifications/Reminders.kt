package com.ritmo.app.notifications

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
import com.ritmo.app.MainActivity
import com.ritmo.app.R
import com.ritmo.app.RitmoApp
import com.ritmo.app.data.Objective
import com.ritmo.app.domain.Streaks
import com.ritmo.app.domain.dias
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object Reminders {
    private const val CHANNEL = "lembretes"
    const val EXTRA_ID = "objective_id"
    private const val EVENING_ID = 0L
    private const val EVENING_MINUTES = 20 * 60

    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Lembretes", NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    /** Alinha os alarmes com os objetivos atuais. Pode ser chamado a qualquer momento. */
    fun sync(context: Context, objectives: List<Objective>) {
        objectives.forEach { objective ->
            if (objective.archivedDay == null && objective.reminderMinutes != null) {
                schedule(context, objective)
            } else {
                alarmManager(context).cancel(pendingIntent(context, objective.id))
            }
        }
        scheduleEvening(context)
    }

    private fun schedule(context: Context, objective: Objective) {
        val minutes = objective.reminderMinutes ?: return
        val now = LocalDateTime.now()
        val time = LocalTime.of(minutes / 60, minutes % 60)
        val next = (0L..7L)
            .map { LocalDate.now().plusDays(it) }
            .map { it.atTime(time) }
            .firstOrNull { it.isAfter(now) && objective.isScheduledOn(it.toLocalDate().toEpochDay()) }
            ?: return
        setAlarm(context, objective.id, next)
    }

    private fun scheduleEvening(context: Context) {
        val time = LocalTime.of(EVENING_MINUTES / 60, 0)
        val today = LocalDate.now().atTime(time)
        val next = if (today.isAfter(LocalDateTime.now())) today else today.plusDays(1)
        setAlarm(context, EVENING_ID, next)
    }

    private fun setAlarm(context: Context, id: Long, at: LocalDateTime) {
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Não exige permissão de alarme exato; o sistema pode atrasar alguns minutos.
        alarmManager(context).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            millis,
            pendingIntent(context, id),
        )
    }

    private fun alarmManager(context: Context) = context.getSystemService(AlarmManager::class.java)

    private fun pendingIntent(context: Context, id: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_ID, id)
        return PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    suspend fun handle(context: Context, id: Long) {
        val repository = (context.applicationContext as RitmoApp).repository
        val state = repository.load() ?: return
        val today = LocalDate.now().toEpochDay()

        if (id == EVENING_ID) {
            val streak = state.mainStreak(today).current
            if (state.scheduledOn(today).isNotEmpty() && !state.isComplete(today) && streak > 0) {
                notify(
                    context,
                    EVENING_ID,
                    "Seu streak de ${dias(streak)} está em risco!",
                    "A Faísca está preocupada. Ainda dá tempo de completar o dia.",
                )
            }
        } else {
            val objective = state.objectives.firstOrNull { it.id == id }
            if (objective != null && objective.isScheduledOn(today) && !state.isDone(id, today)) {
                val streak = Streaks.forObjective(id, state.records).current
                val detail = if (streak > 0) "Sequência atual: ${dias(streak)}. Não deixe esfriar!" else "Bora começar hoje?"
                notify(context, id, "${objective.emoji} ${objective.title}", detail)
            }
        }
        sync(context, state.objectives)
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
        val id = intent.getLongExtra(Reminders.EXTRA_ID, -1)
        if (id < 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Reminders.handle(context, id)
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
                if (state != null) Reminders.sync(context, state.objectives)
            } finally {
                pending.finish()
            }
        }
    }
}
