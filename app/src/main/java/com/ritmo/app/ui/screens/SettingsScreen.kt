package com.ritmo.app.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.ritmo.app.domain.Plan
import com.ritmo.app.domain.Shop
import com.ritmo.app.domain.ShopItem
import com.ritmo.app.domain.ShopKind
import com.ritmo.app.domain.ThemeMode
import com.ritmo.app.domain.clock
import com.ritmo.app.notifications.Reminders
import com.ritmo.app.ui.UiState
import com.ritmo.app.ui.components.ChoiceTile
import com.ritmo.app.ui.components.GroupCard
import com.ritmo.app.ui.components.Mascot
import com.ritmo.app.ui.components.PageTopBar
import java.time.LocalDate

private const val DEFAULT_THEME_NAME = "Ritmo"
private const val THEMES_PER_ROW = 3
private const val LOCKED_IN_SHOP = "Bloqueado, disponível na loja"

@Composable
fun SettingsScreen(
    state: UiState,
    onChange: ((Settings) -> Settings) -> Unit,
    onOpenShop: () -> Unit,
    onOpenPro: (String) -> Unit,
    onExport: (Uri) -> Unit,
    account: AccountActions,
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
                state.account?.let { AccountCard(it, account) }
                PlanCard(state.plan) { onOpenPro(ProReason.NONE) }
                AppearanceCard(state, onChange, onOpenShop, onOpenPro)
                RemindersCard(settings, onChange) { pickingAlert = true }
                PreferencesCard(settings, onChange)
                DataCard(state.plan, onExport, onOpenPro)
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

@Composable
private fun PlanCard(plan: Plan, onOpen: () -> Unit) {
    if (plan.isPro) {
        ProPromo("Ritmo Pro ativo", "Limites liberados e moedas em dobro.", onOpen)
    } else {
        ProPromo("Plano grátis", "Conheça o Pro: sem limites e com mais proteção para o streak.", onOpen)
    }
}

/** Temas e pets ainda não comprados ficam visíveis, com o cadeado, e levam à loja. */
@Composable
private fun AppearanceCard(
    state: UiState,
    onChange: ((Settings) -> Settings) -> Unit,
    onOpenShop: () -> Unit,
    onOpenPro: (String) -> Unit,
) {
    val settings = state.settings
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

        Label("Tema")
        val themes = listOf<ShopItem?>(null) + Shop.items.filter { it.kind == ShopKind.THEME }
        Column(
            Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            themes.chunked(THEMES_PER_ROW).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { item ->
                        if (item == null) {
                            PickerTile(DEFAULT_THEME_NAME, settings.themeId == null, null, Modifier.weight(1f), {
                                onChange { it.copy(themeId = null) }
                            }) { Swatches("theme_default") }
                        } else {
                            ThemeTile(item, state, onChange, onOpenShop, onOpenPro, Modifier.weight(1f))
                        }
                    }
                }
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
private fun ThemeTile(
    item: ShopItem,
    state: UiState,
    onChange: ((Settings) -> Settings) -> Unit,
    onOpenShop: () -> Unit,
    onOpenPro: (String) -> Unit,
    modifier: Modifier,
) {
    val available = item.id in state.owned || (item.proOnly && state.plan.isPro)
    val lock = when {
        available -> null
        item.proOnly -> "Bloqueado, disponível no Pro"
        else -> LOCKED_IN_SHOP
    }
    PickerTile(item.name.removePrefix("Tema "), state.themeId == item.id, lock, modifier, {
        when {
            available -> onChange { it.copy(themeId = item.id) }
            item.proOnly -> onOpenPro(ProReason.THEME)
            else -> onOpenShop()
        }
    }) { Swatches(item.id) }
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
    PickerTile(pet.name, state.petId == pet.id, LOCKED_IN_SHOP.takeUnless { available }, modifier, {
        if (available) onChange { it.copy(petId = pet.id) } else onOpenShop()
    }) {
        Mascot(pet.id, maxOf(state.stage.index, 1), Mood.HAPPY, sizeDp = 46.dp)
    }
}

@Composable
private fun PickerTile(
    name: String,
    selected: Boolean,
    lock: String?,
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
            if (lock != null) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = lock,
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

/** O próprio sistema pergunta onde salvar, então o app não precisa de permissão de arquivos. */
@Composable
internal fun rememberHistoryDownload(onExport: (Uri) -> Unit): () -> Unit {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { target ->
        if (target != null) onExport(target)
    }
    return { picker.launch("ritmo-historico-${LocalDate.now()}.csv") }
}

@Composable
private fun DataCard(plan: Plan, onExport: (Uri) -> Unit, onOpenPro: (String) -> Unit) {
    val download = rememberHistoryDownload(onExport)
    GroupCard(title = "Dados") {
        Spacer(Modifier.height(4.dp))
        if (plan.isPro) {
            ActionLine("Histórico em planilha", "Exportar", download)
        } else {
            ActionLine("Histórico em planilha", "Pro") { onOpenPro(ProReason.EXPORT) }
        }
        Spacer(Modifier.height(4.dp))
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
