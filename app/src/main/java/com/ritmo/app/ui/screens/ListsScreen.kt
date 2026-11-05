package com.ritmo.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ritmo.app.ui.ListRow
import com.ritmo.app.ui.UiState
import com.ritmo.app.ui.components.ChunkyProgress
import com.ritmo.app.ui.components.EmptyMessage
import com.ritmo.app.ui.components.GroupCard
import com.ritmo.app.ui.components.IconBadge
import com.ritmo.app.ui.components.Mood
import com.ritmo.app.ui.components.SectionBanner
import com.ritmo.app.ui.theme.ObjectiveColors

@Composable
fun ListsScreen(state: UiState, onOpen: (Long) -> Unit, onCreate: () -> Unit) {
    if (state.lists.isEmpty()) {
        EmptyMessage(
            "Nenhuma lista ainda",
            "Monte listas de compras, exercícios ou estudos. Cada item concluído rende XP, igual à rotina.",
            state.accessoryId,
            Mood.HAPPY,
            action = "Criar lista",
            onAction = onCreate,
        )
        return
    }

    val pending = state.lists.sumOf { it.pending.size }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionBanner(
                overline = if (state.lists.size == 1) "1 lista" else "${state.lists.size} listas",
                title = when (pending) {
                    0 -> "Tudo concluído por aqui!"
                    1 -> "Falta 1 item"
                    else -> "Faltam $pending itens"
                },
            )
        }
        item {
            GroupCard {
                state.lists.forEach { row -> ListLine(row) { onOpen(row.list.id) } }
            }
        }
    }
}

@Composable
private fun ListLine(row: ListRow, onClick: () -> Unit) {
    val accent = ObjectiveColors[row.list.colorIndex % ObjectiveColors.size]
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(row.list.emoji, accent)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                row.list.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            ChunkyProgress(row.progress, height = 8.dp)
        }
        Spacer(Modifier.width(14.dp))
        Text(
            "${row.done.size}/${row.total}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
