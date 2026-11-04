package com.ritmo.app

import android.app.Application
import com.ritmo.app.data.RitmoDatabase
import com.ritmo.app.data.RitmoRepository
import com.ritmo.app.notifications.Reminders

class RitmoApp : Application() {
    val repository by lazy { RitmoRepository(RitmoDatabase.create(this)) }

    override fun onCreate() {
        super.onCreate()
        Reminders.createChannel(this)
    }
}
