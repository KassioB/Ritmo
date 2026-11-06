package com.ritmo.app

import android.app.Application
import com.ritmo.app.data.RitmoDatabase
import com.ritmo.app.data.RitmoRepository
import com.ritmo.app.notifications.PetIcon
import com.ritmo.app.notifications.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RitmoApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var iconSync: Job? = null

    val repository by lazy { RitmoRepository(RitmoDatabase.create(this)) }

    override fun onCreate() {
        super.onCreate()
        Reminders.createChannel(this)
    }

    /**
     * Logo após a troca, o launcher passa cerca de 1 s com o atalho apontando para o ícone desligado
     * e tocar nele dá "app não instalado". A espera deixa quem saiu sem querer voltar antes disso.
     * Fica fora do escopo da tela porque o botão voltar a destrói logo em seguida.
     */
    fun syncPetIconSoon() {
        iconSync = scope.launch {
            delay(ICON_SYNC_DELAY_MS)
            repository.load()?.let { PetIcon.sync(this@RitmoApp, it) }
        }
    }

    fun cancelPetIconSync() {
        iconSync?.cancel()
    }

    private companion object {
        const val ICON_SYNC_DELAY_MS = 3_000L
    }
}
