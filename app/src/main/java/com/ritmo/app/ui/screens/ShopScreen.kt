package com.ritmo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ritmo.app.domain.Progression
import com.ritmo.app.domain.Shop
import com.ritmo.app.domain.ShopItem
import com.ritmo.app.domain.ShopKind
import com.ritmo.app.ui.UiState
import com.ritmo.app.ui.components.Faisca
import com.ritmo.app.ui.components.Mood
import com.ritmo.app.ui.components.WalletRow
import com.ritmo.app.ui.theme.Gold
import com.ritmo.app.ui.theme.Ice

@Composable
fun ShopScreen(state: UiState, onBuy: (ShopItem) -> Unit, onToggleEquip: (ShopItem) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Loja", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.size(10.dp))
            WalletRow(state.mainStreak.current, state.coins, state.freezes)
        }
        items(Shop.items, key = { it.id }) { item ->
            ShopRow(item, state, onBuy, onToggleEquip)
        }
    }
}

@Composable
private fun ShopRow(
    item: ShopItem,
    state: UiState,
    onBuy: (ShopItem) -> Unit,
    onToggleEquip: (ShopItem) -> Unit,
) {
    val owned = item.id in state.owned
    val equipped = item.id == state.themeId || item.id == state.accessoryId
    val atFreezeLimit = item.kind == ShopKind.FREEZE && state.freezes >= Progression.MAX_FREEZES

    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            when (item.kind) {
                ShopKind.FREEZE -> Icon(Icons.Filled.AcUnit, null, tint = Ice, modifier = Modifier.size(32.dp))
                ShopKind.THEME -> Icon(
                    Icons.Filled.Palette,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                ShopKind.ACCESSORY -> Faisca(Mood.HAPPY, item.id, sizeDp = 52.dp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, fontWeight = FontWeight.Bold)
            Text(item.description, style = MaterialTheme.typography.bodySmall)
            if (item.kind == ShopKind.FREEZE) {
                Text(
                    "Você tem ${state.freezes} de ${Progression.MAX_FREEZES}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        when {
            owned -> OutlinedButton(onClick = { onToggleEquip(item) }) {
                Text(if (equipped) "Remover" else "Usar")
            }
            else -> Button(
                onClick = { onBuy(item) },
                enabled = state.coins >= item.price && !atFreezeLimit,
            ) {
                Icon(Icons.Filled.MonetizationOn, "moedas", tint = Gold, modifier = Modifier.size(18.dp))
                Text(" ${item.price}")
            }
        }
    }
}
