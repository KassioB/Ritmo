package com.ritmo.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ritmo.app.data.Checklist
import com.ritmo.app.ui.components.ChoiceTile
import com.ritmo.app.ui.components.ColorPicker
import com.ritmo.app.ui.components.EditorPage
import com.ritmo.app.ui.components.IconPicker
import com.ritmo.app.ui.components.IconPreview
import com.ritmo.app.ui.components.SectionLabel
import com.ritmo.app.ui.theme.ObjectiveColors

private const val DEFAULT_ICON = "🛒"

/** Atalhos para os usos mais comuns: preenchem nome e ícone de uma vez. */
private val suggestions = listOf(
    "🛒" to "Compras",
    "🏋️" to "Exercícios",
    "📚" to "Estudos",
    "🏠" to "Casa",
    "✈️" to "Viagem",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListEditor(
    list: Checklist?,
    today: Long,
    onSave: (Checklist) -> Unit,
    onClose: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(list?.title.orEmpty()) }
    var emoji by rememberSaveable { mutableStateOf(list?.emoji ?: DEFAULT_ICON) }
    var colorIndex by rememberSaveable { mutableIntStateOf(list?.colorIndex ?: 0) }
    val accent = ObjectiveColors[colorIndex % ObjectiveColors.size]

    val dirty = title != list?.title.orEmpty() ||
        emoji != (list?.emoji ?: DEFAULT_ICON) ||
        colorIndex != (list?.colorIndex ?: 0)

    EditorPage(
        title = if (list == null) "Nova lista" else "Editar lista",
        canSave = title.isNotBlank(),
        dirty = dirty,
        onClose = onClose,
        onSave = {
            val base = list ?: Checklist(title = "", emoji = emoji, colorIndex = colorIndex, createdDay = today)
            onSave(base.copy(title = title.trim(), emoji = emoji, colorIndex = colorIndex))
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconPreview(emoji, accent)
            Spacer(Modifier.width(14.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(60) },
                label = { Text("Nome da lista") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        if (list == null) {
            SectionLabel("Sugestões")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                suggestions.forEach { (icon, name) ->
                    ChoiceTile(
                        selected = title == name,
                        onClick = {
                            title = name
                            emoji = icon
                        },
                        accent = accent,
                    ) {
                        Text("$icon  $name", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        SectionLabel("Ícone")
        IconPicker(emoji, accent) { emoji = it }

        SectionLabel("Cor")
        ColorPicker(colorIndex) { colorIndex = it }
    }
}
