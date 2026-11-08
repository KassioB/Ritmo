package com.ritmo.app

import android.app.Application
import com.ritmo.app.data.AccountRepository
import com.ritmo.app.data.Cloud
import com.ritmo.app.data.RitmoDatabase
import com.ritmo.app.data.RitmoRepository
import com.ritmo.app.focus.FocusTimer
import com.ritmo.app.notifications.PetIcon
import com.ritmo.app.notifications.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class RitmoApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var iconSync: Job? = null

    private val database by lazy { RitmoDatabase.create(this) }
    val repository by lazy { RitmoRepository(database) }
    val accounts by lazy { AccountRepository(database, repository) }
    val focus by lazy { FocusTimer(this, repository, scope) }

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        Cloud.init(this)
        Reminders.createChannel(this)
        FocusTimer.createChannel(this)
        scope.launch {
            accounts.reconcile()
            // Espera a pessoa parar de mexer por um instante e sobe o progresso para a conta.
            repository.snapshot.debounce(SYNC_DELAY_MS).collect { accounts.sync() }
        }
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
        const val SYNC_DELAY_MS = 2_000L
    }
}
