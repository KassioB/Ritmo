package com.ritmo.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.ritmo.app.domain.IconCatalog
import com.ritmo.app.ui.theme.ObjectiveColors
import com.ritmo.app.ui.theme.Ritmo

private const val ICONS_PER_ROW = 6

/**
 * Tela inteira de edição. Não há gesto nem toque fora que a feche: só o X, o voltar do sistema
 * ou salvar. Com alterações pendentes, sair pede confirmação para o rascunho não sumir.
 */
@Composable
fun EditorPage(
    title: String,
    canSave: Boolean,
    dirty: Boolean,
    onSave: () -> Unit,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val close = { if (dirty) confirmDiscard = true else onClose() }
    BackHandler(onBack = close)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            PageTopBar(title, Icons.Rounded.Close, "Fechar", close)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                content = content,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp),
            ) {
                ChunkyButton("Salvar", onSave, Modifier.fillMaxWidth(), enabled = canSave)
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            properties = DialogProperties(dismissOnClickOutside = false),
            title = { Text("Descartar alterações?") },
            text = { Text("O que você preencheu aqui ainda não foi salvo.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onClose()
                }) { Text("Descartar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Continuar editando") }
            },
        )
    }
}

@Composable
fun SectionLabel(text: String, hint: String? = null) {
    Column {
        Text(text, style = MaterialTheme.typography.titleMedium)
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Grade de ícones por assunto. A escolha atual aparece com moldura, fundo e selo de marcado. */
@Composable
fun IconPicker(selected: String, accent: Color, onSelect: (String) -> Unit) {
    var group by rememberSaveable { mutableIntStateOf(IconCatalog.groupOf(selected)) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconCatalog.groups.forEachIndexed { index, item ->
                GroupTab(item.name, index == group, accent, selected in item.icons) { group = index }
            }
        }
        IconCatalog.groups[group].icons.chunked(ICONS_PER_ROW).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { icon ->
                    IconTile(icon, icon == selected, accent, Modifier.weight(1f)) { onSelect(icon) }
                }
                repeat(ICONS_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun GroupTab(name: String, active: Boolean, accent: Color, holdsSelection: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (active) accent else scheme.surfaceContainerHigh)
            .selectable(selected = active, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            color = if (active) Color.White else scheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
        )
        if (holdsSelection && !active) {
            Spacer(Modifier.size(6.dp))
            Box(Modifier.size(7.dp).background(accent, CircleShape))
        }
    }
}

@Composable
private fun IconTile(icon: String, chosen: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val grow by animateFloatAsState(
        if (chosen) 1.1f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "icone",
    )
    Box(modifier.aspectRatio(1f).scale(grow)) {
        Box(
            Modifier
                .matchParentSize()
                .clip(shape)
                .background(if (chosen) accent.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                .then(if (chosen) Modifier.border(3.dp, accent, shape) else Modifier)
                .selectable(selected = chosen, role = Role.RadioButton, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, fontSize = 24.sp)
        }
        if (chosen) {
            CheckSeal(accent, Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-5).dp))
        }
    }
}

@Composable
private fun CheckSeal(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(20.dp)
            .background(color, CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
    }
}

@Composable
fun ColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ObjectiveColors.forEachIndexed { index, color ->
            val chosen = index == selected % ObjectiveColors.size
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onSelect(index) }
                    .semantics {
                        this.selected = chosen
                        contentDescription = "Cor ${index + 1}"
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (chosen) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

/** Prévia grande: é o ícone que vai aparecer na rotina, com a cor escolhida. */
@Composable
fun IconPreview(icon: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(84.dp)
            .background(color.copy(alpha = 0.26f), CircleShape)
            .border(3.dp, color, CircleShape)
            .semantics { contentDescription = "Ícone escolhido: $icon" },
        contentAlignment = Alignment.Center,
    ) {
        Text(icon, fontSize = 42.sp)
    }
}

/** Mensagem curta com um atalho para resolver uma pendência. */
@Composable
fun NoticeRow(message: String, action: String, onAction: () -> Unit) {
    val brand = Ritmo.brand
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        TextButton(onClick = onAction) { Text(action, color = brand.doneText) }
    }
}
