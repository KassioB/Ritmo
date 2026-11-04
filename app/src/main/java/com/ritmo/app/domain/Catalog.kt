package com.ritmo.app.domain

data class Stats(
    val completions: Int,
    val perfectDays: Int,
    val bestMainStreak: Int,
    val bestObjectiveStreak: Int,
    val level: Int,
    val freezesUsed: Int,
    val purchases: Int,
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val coins: Int,
    val isUnlocked: (Stats) -> Boolean,
)

object Achievements {
    val all = listOf(
        Achievement("first_step", "Primeiro passo", "Conclua seu primeiro objetivo", 5) { it.completions >= 1 },
        Achievement("perfect_1", "Dia perfeito", "Complete todos os objetivos de um dia", 10) { it.perfectDays >= 1 },
        Achievement("perfect_10", "Dez de dez", "Tenha 10 dias perfeitos", 30) { it.perfectDays >= 10 },
        Achievement("main_7", "Uma semana no ritmo", "Streak principal de 7 dias", 25) { it.bestMainStreak >= 7 },
        Achievement("main_30", "Um mês no ritmo", "Streak principal de 30 dias", 100) { it.bestMainStreak >= 30 },
        Achievement("main_100", "Centenário", "Streak principal de 100 dias", 300) { it.bestMainStreak >= 100 },
        Achievement("objective_7", "Pegando o jeito", "Streak de 7 dias em um objetivo", 15) { it.bestObjectiveStreak >= 7 },
        Achievement("objective_30", "Virou hábito", "Streak de 30 dias em um objetivo", 60) { it.bestObjectiveStreak >= 30 },
        Achievement("completions_100", "Cem vezes", "Conclua 100 objetivos", 40) { it.completions >= 100 },
        Achievement("level_5", "Subindo", "Alcance o nível 5", 30) { it.level >= 5 },
        Achievement("level_10", "Veterano", "Alcance o nível 10", 80) { it.level >= 10 },
        Achievement("freeze_1", "Salvo pelo gelo", "Um bloqueio protegeu seu streak", 5) { it.freezesUsed >= 1 },
        Achievement("shop_1", "Primeira compra", "Compre algo na loja", 5) { it.purchases >= 1 },
    )
}

enum class ShopKind { FREEZE, THEME, ACCESSORY }

data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
    val kind: ShopKind,
)

object Shop {
    const val FREEZE_ID = "freeze"
    val items = listOf(
        ShopItem(FREEZE_ID, "Bloqueio de streak", "Protege seus streaks em um dia perdido", 50, ShopKind.FREEZE),
        ShopItem("theme_ocean", "Tema Oceano", "Azuis profundos", 100, ShopKind.THEME),
        ShopItem("theme_forest", "Tema Floresta", "Verdes calmos", 100, ShopKind.THEME),
        ShopItem("theme_plum", "Tema Ameixa", "Roxos vibrantes", 100, ShopKind.THEME),
        ShopItem("acc_glasses", "Óculos escuros", "A Faísca fica estilosa", 150, ShopKind.ACCESSORY),
        ShopItem("acc_party", "Chapéu de festa", "Todo dia é uma comemoração", 150, ShopKind.ACCESSORY),
        ShopItem("acc_crown", "Coroa", "Para quem mantém o ritmo", 300, ShopKind.ACCESSORY),
    )
}
