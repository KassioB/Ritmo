package com.ritmo.app.notifications

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.ritmo.app.RitmoApp
import com.ritmo.app.data.Snapshot
import com.ritmo.app.domain.Evolution
import com.ritmo.app.domain.PetIcons
import com.ritmo.app.domain.PetMood
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Mantém o ícone do launcher igual ao pet. O Android não deixa redesenhar um ícone: cada estado
 * é um activity-alias declarado de antemão e a troca liga um e desliga o outro.
 */
object PetIcon {
    private const val ALIAS_PREFIX = "com.ritmo.app.icon."

    /** Folga para o sistema agrupar o alarme com outros e gastar menos bateria. */
    private const val CHECK_WINDOW_MS = 15 * 60 * 1000L

    @Synchronized
    fun sync(context: Context, state: Snapshot) {
        val now = LocalDateTime.now()
        val today = now.toLocalDate().toEpochDay()
        val closed = state.closedUntil(today)
        val stage = Evolution.stageFor(closed.mainStreak(today).current).index
        val mood = closed.feeling(today, now.hour).mood
        show(context, ComponentName(context, ALIAS_PREFIX + PetIcons.alias(state.settings.petId, stage, mood)))
        scheduleCheck(context, now)
    }

    /** A tarefa pertence à MainActivity, que não é um atalho: os recentes usariam o ícone padrão. */
    @SuppressLint("DiscouragedApi")
    @Suppress("DEPRECATION")
    fun showInRecents(activity: Activity, alias: String) {
        val id = activity.resources.getIdentifier("ic_pet_${alias.lowercase()}", "mipmap", activity.packageName)
        val icon = ContextCompat.getDrawable(activity, id)?.toBitmap() ?: return
        activity.setTaskDescription(ActivityManager.TaskDescription(null, icon))
    }

    private fun show(context: Context, icon: ComponentName) {
        val manager = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        val active = manager.queryIntentActivities(launcher, 0)
            .map { ComponentName(it.activityInfo.packageName, it.activityInfo.name) }
        if (active == listOf(icon)) return

        // Liga o novo primeiro: o app nunca fica sem entrada no launcher, nem por um instante.
        manager.setComponentEnabledSetting(icon, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        (active - icon).forEach {
            manager.setComponentEnabledSetting(it, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    }

    /** RTC não acorda o aparelho: o ícone só precisa estar certo quando alguém olhar a tela. */
    private fun scheduleCheck(context: Context, now: LocalDateTime) {
        val at = PetMood.nextChange(now).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val operation = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, PetIconReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        context.getSystemService(AlarmManager::class.java).setWindow(AlarmManager.RTC, at, CHECK_WINDOW_MS, operation)
    }
}

class PetIconReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = (context.applicationContext as RitmoApp).repository.load()
                if (state != null) PetIcon.sync(context, state)
            } finally {
                pending.finish()
            }
        }
    }
}
