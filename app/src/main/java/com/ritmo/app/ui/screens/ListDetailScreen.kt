package com.ritmo.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.ritmo.app.data.ChecklistItem
import com.ritmo.app.domain.Difficulty
import com.ritmo.app.ui.ListRow
import com.ritmo.app.ui.components.CheckToggle
import com.ritmo.app.ui.components.ChoiceTile
import com.ritmo.app.ui.components.ChunkyFab
import com.ritmo.app.ui.components.ChunkyProgress
import com.ritmo.app.ui.components.GroupCard
import com.ritmo.app.ui.components.IconBadge
import com.ritmo.app.ui.components.PageTopBar
import com.ritmo.app.ui.theme.ObjectiveColors
import com.ritmo.app.ui.theme.Ritmo

private val CheckBoxShape = RoundedCornerShape(8.dp)

/** Uma lista aberta: pendentes em cima, concluídos recolhíveis e o campo de adicionar fixo embaixo. */
@Composable
fun ListDetailScreen(
    row: ListRow,
    onBack: () -> Unit,
    onToggle: (Long) -> Unit,
    canAdd: Boolean,
    onLimit: () -> Unit,
    onAdd: (String, Difficulty) -> Unit,
    onUpdateItem: (ChecklistItem) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onEditList: () -> Unit,
    onUncheckAll: () -> Unit,
    onDeleteList: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val accent = ObjectiveColors[row.list.colorIndex % ObjectiveColors.size]
    var showDone by rememberSaveable { mutableStateOf(true) }
    var editingItem by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            PageTopBar(
                title = row.list.title,
                navIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                navDescription = "Voltar",
                onNav = onBack,
                leading = { IconBadge(row.list.emoji, accent, size = 36.dp) },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = "Mais opções",
                                tint = Ritmo.brand.onHeader,
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Editar lista") },
                                onClick = {
                                    menuOpen = false
                                    onEditList()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Desmarcar tudo") },
                                enabled = row.done.isNotEmpty(),
                                onClick = {
                                    menuOpen = false
                                    onUncheckAll()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Excluir lista", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuOpen = false
                                    confirmDelete = true
                                },
                            )
                        }
                    }
                },
            )

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (row.total > 0) {
                    item(key = "progresso") {
                        Column {
                            ChunkyProgress(row.progress)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${row.done.size} de ${row.total} concluídos",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                } else {
                    item(key = "vazia") {
                        Text(
                            "Lista vazia. Adicione o primeiro item no campo abaixo.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (row.pending.isNotEmpty()) {
                    item(key = "pendentes") {
                        GroupCard {
                            row.pending.forEach { item ->
                                ItemLine(item, accent, { onToggle(item.id) }, { editingItem = item.id })
                            }
                        }
                    }
                }
                if (row.done.isNotEmpty()) {
                    item(key = "concluidos") { DoneHeader(row.done.size, showDone) { showDone = !showDone } }
                    if (showDone) {
                        item(key = "lista-concluidos") {
                            GroupCard {
                                row.done.forEach { item ->
                                    ItemLine(item, accent, { onToggle(item.id) }, { editingItem = item.id })
                                }
                            }
                        }
                    }
                }
            }

            if (canAdd) AddItemBar(accent, onAdd) else ItemLimitBar(onLimit)
        }
    }

    (row.pending + row.done).firstOrNull { it.id == editingItem }?.let { item ->
        ItemDialog(
            item = item,
            onSave = {
                onUpdateItem(it)
                editingItem = null
            },
            onDelete = {
                onDeleteItem(item.id)
                editingItem = null
            },
            onDismiss = { editingItem = null },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            properties = DialogProperties(dismissOnClickOutside = false),
            title = { Text("Excluir lista?") },
            text = { Text("Os itens desta lista serão apagados. O XP que você já ganhou continua seu.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteList()
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ItemLine(item: ChecklistItem, accent: Color, onToggle: () -> Unit, onEdit: () -> Unit) {
    val done = item.doneDay != null
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Editar item", onClick = onEdit)
            .padding(start = 4.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckToggle(done, item.title, accent, CheckBoxShape, onToggle, size = 28.dp)
        Spacer(Modifier.width(6.dp))
        Text(
            item.title,
            modifier = Modifier.weight(1f),
            color = if (done) scheme.onSurfaceVariant else scheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (done) TextDecoration.LineThrough else null,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "+${item.difficulty.xp} XP",
            color = scheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun DoneHeader(count: Int, expanded: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Concluídos ($count)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Icon(
            if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = if (expanded) "Recolher concluídos" else "Mostrar concluídos",
        )
    }
}

/** Toma o lugar do campo de adicionar: marcar, editar e apagar continuam normais. */
@Composable
private fun ItemLimitBar(onLimit: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Lista cheia no plano grátis",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onLimit) { Text("Ver o Pro") }
    }
}

/** Fica aberto depois de adicionar, para digitar vários itens em sequência. */
@Composable
private fun AddItemBar(accent: Color, onAdd: (String, Difficulty) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var difficulty by rememberSaveable { mutableStateOf(Difficulty.MEDIUM) }
    val submit = {
        val title = text.trim()
        if (title.isNotEmpty()) {
            onAdd(title, difficulty)
            text = ""
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(120) },
            placeholder = { Text("Adicionar item") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.weight(1f),
        )
        DifficultyCycle(difficulty, accent) { difficulty = it }
        ChunkyFab("Adicionar item", { submit() }, icon = Icons.Rounded.Add, size = 48.dp, enabled = text.isNotBlank())
    }
}

/** Toque para alternar entre as dificuldades sem sair da digitação. */
@Composable
private fun DifficultyCycle(value: Difficulty, accent: Color, onChange: (Difficulty) -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val next = Difficulty.entries[(value.ordinal + 1) % Difficulty.entries.size]
    Column(
        Modifier
            .clip(shape)
            .border(2.dp, accent, shape)
            .clickable { onChange(next) }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Dificuldade ${value.label}, ${value.xp} XP. Toque para mudar."
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("+${value.xp}", style = MaterialTheme.typography.titleSmall)
        Text("XP", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ItemDialog(
    item: ChecklistItem,
    onSave: (ChecklistItem) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable(item.id) { mutableStateOf(item.title) }
    var difficulty by rememberSaveable(item.id) { mutableStateOf(item.difficulty) }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text("Editar item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Difficulty.entries.forEach { option ->
                        ChoiceTile(difficulty == option, { difficulty = option }, Modifier.weight(1f)) {
                            Text(option.label, style = MaterialTheme.typography.labelMedium)
                            Text("${option.xp} XP", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                if (item.doneDay != null) {
                    Text(
                        "A dificuldade vale a partir da próxima vez que o item for marcado.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = onDelete) {
                    Text("Excluir item", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = { onSave(item.copy(title = title.trim(), difficulty = difficulty)) },
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
