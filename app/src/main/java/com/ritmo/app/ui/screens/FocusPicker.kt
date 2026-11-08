package com.ritmo.app.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ritmo.app.domain.FocusKind
import com.ritmo.app.domain.FocusTarget
import com.ritmo.app.domain.Quadrant
import com.ritmo.app.ui.ObjectiveRow
import com.ritmo.app.ui.UiState
import com.ritmo.app.ui.components.GroupCard
import com.ritmo.app.ui.components.IconBadge
import com.ritmo.app.ui.components.PageTopBar
import com.ritmo.app.ui.theme.ObjectiveColors

private data class Choice(val target: FocusTarget, val color: Color, val done: Boolean = false)

private data class Section(val title: String, val choices: List<Choice>)

/** Escolher no que focar: tarefas (matriz e listas) ou hábitos. */
@Composable
fun FocusPicker(state: UiState, selected: FocusTarget?, onPick: (FocusTarget) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    var habits by rememberSaveable { mutableStateOf(selected?.kind == FocusKind.HABIT) }
    var query by rememberSaveable { mutableStateOf("") }

    val sections = (if (habits) habitSections(state) else taskSections(state))
        .map { section -> section.copy(choices = section.choices.filter { it.target.title.contains(query.trim(), ignoreCase = true) }) }
        .filter { it.choices.isNotEmpty() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            PageTopBar("Selecione um foco", Icons.Rounded.Close, "Fechar", onClose)
            Column(Modifier.padding(horizontal = 16.dp).padding(top = 12.dp)) {
                Row {
                    PickerTab("Tarefa", !habits) { habits = false }
                    Spacer(Modifier.width(18.dp))
                    PickerTab("Hábito", habits) { habits = true }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(60) },
                    placeholder = { Text("Buscar") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Escolha uma ${if (habits) "rotina" else "tarefa"} para focar nela com o Pomodoro ou o cronômetro.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().imePadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (sections.isEmpty()) {
                    item {
                        Text(
                            when {
                                query.isNotBlank() -> "Nada encontrado para \"${query.trim()}\"."
                                habits -> "Nenhum hábito ainda. Crie um na tela Hoje."
                                else -> "Nenhuma tarefa pendente. Crie tarefas na Matriz ou nas Listas."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                sections.forEach { section ->
                    item(key = "${if (habits) "h" else "t"}-${section.title}") {
                        GroupCard(title = section.title) {
                            section.choices.forEach { choice ->
                                ChoiceLine(choice, choice.target == selected) {
                                    onPick(choice.target)
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.navigationBarsPadding()) }
            }
        }
    }
}

private fun habitSections(state: UiState): List<Section> {
    fun choices(rows: List<ObjectiveRow>) = rows.map { row ->
        val objective = row.objective
        Choice(
            FocusTarget(FocusKind.HABIT, objective.id, objective.title, objective.emoji),
            ObjectiveColors[objective.colorIndex % ObjectiveColors.size],
            done = row.done,
        )
    }
    return listOf(
        Section("Hoje", choices(state.todayRows.sortedBy { it.done })),
        Section("Outros dias", choices(state.otherRows)),
    )
}

@Composable
private fun taskSections(state: UiState): List<Section> {
    val matrix = Quadrant.entries.map { quadrant ->
        val color = quadrant.color()
        Section(
            "Matriz · ${quadrant.title}",
            state.tasks.filter { it.quadrant == quadrant && it.doneDay == null }
                .map { Choice(FocusTarget(FocusKind.TASK, it.id, it.title), color) },
        )
    }
    val lists = state.lists.map { row ->
        val color = ObjectiveColors[row.list.colorIndex % ObjectiveColors.size]
        Section(
            "${row.list.emoji} ${row.list.title}",
            row.pending.map { Choice(FocusTarget(FocusKind.ITEM, it.id, it.title), color) },
        )
    }
    return matrix + lists
}

@Composable
private fun PickerTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Text(
        label,
        style = MaterialTheme.typography.titleLarge,
        color = if (selected) scheme.primary else scheme.onSurfaceVariant,
        modifier = Modifier
            .clickable(onClickLabel = "Mostrar $label", onClick = onClick)
            .semantics { if (selected) heading() }
            .padding(vertical = 4.dp),
    )
}

@Composable
private fun ChoiceLine(choice: Choice, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val emoji = choice.target.emoji
        if (emoji != null) {
            IconBadge(emoji, choice.color, size = 34.dp)
        } else {
            // Tarefas não têm emoji: um círculo vazio na cor da caixa ou da lista, como uma caixa de marcar.
            Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(18.dp).border(2.dp, choice.color, CircleShape))
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            choice.target.title,
            color = if (choice.done) scheme.onSurfaceVariant else scheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected) Icon(Icons.Rounded.Check, contentDescription = "Escolhido", tint = scheme.primary)
    }
}
