package com.ritmo.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ritmo.app.data.Settings
import com.ritmo.app.domain.Mood
import com.ritmo.app.domain.Pet
import com.ritmo.app.domain.Pets
import com.ritmo.app.domain.Shop
import com.ritmo.app.domain.ShopKind
import com.ritmo.app.domain.ThemeMode
import com.ritmo.app.domain.clock
import com.ritmo.app.notifications.Reminders
import com.ritmo.app.ui.UiState
import com.ritmo.app.ui.components.ChoiceTile
import com.ritmo.app.ui.components.GroupCard
import com.ritmo.app.ui.components.Mascot
import com.ritmo.app.ui.components.PageTopBar

private const val DEFAULT_THEME_NAME = "Ritmo"

@Composable
fun SettingsScreen(
    state: UiState,
    onChange: ((Settings) -> Settings) -> Unit,
    onOpenShop: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val settings = state.settings
    var pickingAlert by rememberSaveable { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            PageTopBar("Configurações", Icons.AutoMirrored.Rounded.ArrowBack, "Voltar", onBack)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AppearanceCard(state, onChange, onOpenShop)
                RemindersCard(settings, onChange) { pickingAlert = true }
                PreferencesCard(settings, onChange)
                AboutCard()
            }
        }
    }

    if (pickingAlert) {
        ReminderDialog(
            start = settings.streakAlertMinutes,
            title = "Horário do aviso",
            onConfirm = { minutes ->
                onChange { it.copy(streakAlertMinutes = minutes) }
                pickingAlert = false
            },
            onDismiss = { pickingAlert = false },
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
    )
}

/** Temas e pets ainda não comprados ficam visíveis, com o cadeado, e levam à loja. */
@Composable
private fun AppearanceCard(
    state: UiState,
    onChange: ((Settings) -> Settings) -> Unit,
    onOpenShop: () -> Unit,
) {
    val settings = state.settings
    val fixedMode = settings.themeId == "theme_white" || settings.themeId == "theme_black"
    GroupCard(title = "Aparência") {
        Label("Modo")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                ChoiceTile(
                    settings.themeMode == mode,
                    { onChange { it.copy(themeMode = mode) } },
                    Modifier.weight(1f),
                ) {
                    Text(mode.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
        if (fixedMode) {
            Text(
                "O tema escolhido tem modo fixo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp),
            )
        }

        Label("Tema")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PickerTile(DEFAULT_THEME_NAME, settings.themeId == null, false, Modifier.weight(1f), {
                onChange { it.copy(themeId = null) }
            }) { Swatches("theme_default") }
            Shop.items.filter { it.kind == ShopKind.THEME }.forEach { item ->
                val owned = item.id in state.owned
                PickerTile(
                    item.name.removePrefix("Tema "),
                    settings.themeId == item.id,
                    !owned,
                    Modifier.weight(1f),
                    { if (owned) onChange { it.copy(themeId = item.id) } else onOpenShop() },
                ) { Swatches(item.id) }
            }
        }

        Label("Pet")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Pets.all.forEach { pet -> PetTile(pet, state, onChange, onOpenShop, Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PetTile(
    pet: Pet,
    state: UiState,
    onChange: ((Settings) -> Settings) -> Unit,
    onOpenShop: () -> Unit,
    modifier: Modifier,
) {
    val available = pet.id == null || pet.id in state.owned
    PickerTile(pet.name, state.petId == pet.id, !available, modifier, {
        if (available) onChange { it.copy(petId = pet.id) } else onOpenShop()
    }) {
        Mascot(pet.id, maxOf(state.stage.index, 1), Mood.HAPPY, sizeDp = 46.dp)
    }
}

@Composable
private fun PickerTile(
    name: String,
    selected: Boolean,
    locked: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    preview: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) scheme.primary.copy(alpha = 0.16f) else scheme.surfaceContainer)
            .border(2.dp, if (selected) scheme.primary else scheme.outline, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.height(46.dp), contentAlignment = Alignment.Center) { preview() }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (locked) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = "Bloqueado, disponível na loja",
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp),
                )
            }
            Text(
                name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SwitchLine(title: String, detail: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (detail != null) {
                Text(
                    detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Switch(checked, onChange)
    }
}

@Composable
private fun RemindersCard(
    settings: Settings,
    onChange: ((Settings) -> Settings) -> Unit,
    onPickAlert: () -> Unit,
) {
    val context = LocalContext.current
    var exact by remember { mutableStateOf(true) }
    var notifications by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        exact = Reminders.canBeExact(context)
        notifications = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose {}
    }
    GroupCard(title = "Lembretes") {
        Spacer(Modifier.height(4.dp))
        SwitchLine("Lembretes dos objetivos", null, settings.remindersEnabled) { on ->
            onChange { it.copy(remindersEnabled = on) }
        }
        SwitchLine("Aviso de streak em risco", "Avisa se o dia ainda não estiver completo", settings.streakAlertEnabled) { on ->
            onChange { it.copy(streakAlertEnabled = on) }
        }
        if (settings.streakAlertEnabled) {
            ActionLine("Horário do aviso", clock(settings.streakAlertMinutes), onPickAlert)
        }
        ActionLine(
            "Notificações",
            if (notifications) "Ativadas" else "Ativar",
        ) { openNotificationSettings(context) }
        ActionLine(
            "Lembrete na hora exata",
            if (exact) "Permitido" else "Permitir",
        ) { openExactAlarmSettings(context) }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ActionLine(title: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) { Text(action) }
    }
}

@Composable
private fun PreferencesCard(settings: Settings, onChange: ((Settings) -> Settings) -> Unit) {
    GroupCard(title = "Preferências") {
        Spacer(Modifier.height(4.dp))
        SwitchLine("Vibrar ao concluir", null, settings.hapticsEnabled) { on ->
            onChange { it.copy(hapticsEnabled = on) }
        }
        Label("A semana começa no")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChoiceTile(!settings.weekStartsMonday, { onChange { it.copy(weekStartsMonday = false) } }, Modifier.weight(1f)) {
                Text("Domingo", style = MaterialTheme.typography.labelLarge)
            }
            ChoiceTile(settings.weekStartsMonday, { onChange { it.copy(weekStartsMonday = true) } }, Modifier.weight(1f)) {
                Text("Segunda", style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AboutCard() {
    val context = LocalContext.current
    val version = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    GroupCard(title = "Sobre") {
        Text(
            "Ritmo $version",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}
