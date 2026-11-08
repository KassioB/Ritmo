package com.ritmo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.ritmo.app.data.MatrixTask
import com.ritmo.app.domain.Progression
import com.ritmo.app.domain.Quadrant
import com.ritmo.app.ui.UiState
import com.ritmo.app.ui.components.CheckToggle
import com.ritmo.app.ui.components.ChoiceTile

/**
 * Vermelho, âmbar, azul e verde, as cores que a matriz costuma usar. Fixas em todos os temas para as
 * quatro caixas nunca ficarem parecidas; por isso só pintam marcas, e o texto fica na cor do texto.
 */
fun Quadrant.color(): Color = when (this) {
    Quadrant.DO -> Color(0xFFE5484D)
    Quadrant.PLAN -> Color(0xFFF5A524)
    Quadrant.DELEGATE -> Color(0xFF3E7BFA)
    Quadrant.DROP -> Color(0xFF2BB673)
}

/** O número sobre a cor: escuro no âmbar, que é claro demais para texto branco. */
private fun Quadrant.onColor(): Color = if (this == Quadrant.PLAN) Color(0xFF241A00) else Color.White

/** A matriz de Eisenhower: o que é urgente e o que é importante, cada coisa na sua caixa. */
@Composable
fun MatrixScreen(
    state: UiState,
    onAdd: (String, Quadrant) -> Unit,
    onToggle: (Long) -> Unit,
    onUpdate: (MatrixTask) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var adding by rememberSaveable { mutableStateOf<Quadrant?>(null) }
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }

    Column(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Quadrant.entries.chunked(2).forEach { pair ->
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { quadrant ->
                    QuadrantCard(
                        quadrant = quadrant,
                        // Pendentes primeiro; as concluídas hoje ficam riscadas embaixo.
                        tasks = state.tasks.filter { it.quadrant == quadrant }.sortedBy { it.doneDay != null },
                        onAdd = { adding = quadrant },
                        onToggle = onToggle,
                        onEdit = { editing = it },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }

    adding?.let { quadrant ->
        TaskDialog(
            task = null,
            start = quadrant,
            onSave = { title, chosen ->
                onAdd(title, chosen)
                adding = null
            },
            onDelete = null,
            onDismiss = { adding = null },
        )
    }
    state.tasks.firstOrNull { it.id == editing }?.let { task ->
        TaskDialog(
            task = task,
            start = task.quadrant,
            onSave = { title, chosen ->
                onUpdate(task.copy(title = title, quadrant = chosen))
                editing = null
            },
            onDelete = {
                onDelete(task.id)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun QuadrantCard(
    quadrant: Quadrant,
    tasks: List<MatrixTask>,
    onAdd: () -> Unit,
    onToggle: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier,
) {
    val accent = quadrant.color()
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .background(scheme.surfaceContainer)
            .border(2.dp, accent.copy(alpha = 0.45f), shape),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(22.dp).background(accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${quadrant.ordinal + 1}",
                    color = quadrant.onColor(),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
                Text(
                    quadrant.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    quadrant.detail,
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Rounded.Add, contentDescription = "Adicionar em ${quadrant.title}", tint = accent)
            }
        }
        if (tasks.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Nada aqui",
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 6.dp)) {
                items(tasks, key = { it.id }) { task -> TaskLine(task, accent, { onToggle(task.id) }, { onEdit(task.id) }) }
            }
        }
    }
}

@Composable
private fun TaskLine(task: MatrixTask, accent: Color, onToggle: () -> Unit, onEdit: () -> Unit) {
    val done = task.doneDay != null
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Editar tarefa", onClick = onEdit)
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckToggle(done, task.title, accent, CircleShape, onToggle, size = 22.dp)
        Text(
            task.title,
            color = if (done) scheme.onSurfaceVariant else scheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (done) TextDecoration.LineThrough else null,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TaskDialog(
    task: MatrixTask?,
    start: Quadrant,
    onSave: (String, Quadrant) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var quadrant by rememberSaveable(task?.id) { mutableStateOf(start) }
    val save = { if (title.isNotBlank()) onSave(title.trim(), quadrant) }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(if (task == null) "Nova tarefa" else "Editar tarefa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    label = { Text("O que precisa ser feito?") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Quadrant.entries.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { option ->
                            ChoiceTile(
                                quadrant == option,
                                { quadrant = option },
                                Modifier.weight(1f),
                                accent = option.color(),
                            ) {
                                Text(option.title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                Text(
                                    option.detail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                )
                            }
                        }
                    }
                }
                Text(
                    "Concluir rende ${Progression.TASK_XP} XP.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Excluir tarefa", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = save) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
