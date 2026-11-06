package com.ritmo.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Porta de entrada do launcher: só abre a [MainActivity] e sai. Os ícones do pet apontam para cá
 * para que a tarefa do app nunca dependa de um ícone que pode ser desligado com ela aberta.
 *
 * Fechar esta tela não basta: a tarefa guarda o ícone que a criou e o sistema a destrói quando ele
 * é desligado. Por isso o manifesto declara relinquishTaskIdentity, que passa a identidade da
 * tarefa para a [MainActivity].
 */
class LauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }
}
