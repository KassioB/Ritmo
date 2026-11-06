package com.ritmo.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ritmo.app.domain.PetIcons
import com.ritmo.app.notifications.PetIcon
import com.ritmo.app.ui.RitmoRoot
import com.ritmo.app.ui.RitmoViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class MainActivity : ComponentActivity() {
    private val viewModel: RitmoViewModel by viewModels()
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RitmoRoot(viewModel) }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.state.filterNotNull()
            .map { PetIcons.alias(it.petId, it.stage.index, it.mood) }
            .flowWithLifecycle(lifecycle)
            .distinctUntilChanged()
            .onEach { PetIcon.showInRecents(this, it) }
            .launchIn(lifecycleScope)
    }

    override fun onStart() {
        super.onStart()
        (application as RitmoApp).cancelPetIconSync()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDay()
    }

    override fun onStop() {
        super.onStop()
        // O launcher volta a aparecer agora, então é aqui que o rosto do ícone precisa mudar.
        if (!isChangingConfigurations) (application as RitmoApp).syncPetIconSoon()
    }
}
