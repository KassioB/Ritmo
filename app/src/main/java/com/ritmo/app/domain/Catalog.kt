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

enum class ShopKind { FREEZE, REPAIR, THEME, PET }

data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
    val kind: ShopKind,
    /** Não se compra com moedas: vem incluído no plano Pro. */
    val proOnly: Boolean = false,
)

/** Modo claro ou escuro; os temas de cor fixa, como Branco e Preto, ignoram esta escolha. */
enum class ThemeMode(val label: String) {
    SYSTEM("Automático"),
    LIGHT("Claro"),
    DARK("Escuro"),
}

/** Personagem que acompanha o usuário. [id] nulo é a Faísca, que vem de graça. */
data class Pet(val id: String?, val name: String, val article: String)

object Pets {
    const val RAIO = "pet_raio"
    const val LUZ = "pet_luz"
    const val MAROMBA = "pet_maromba"

    val default = Pet(null, "Faísca", "a")
    val all = listOf(default, Pet(RAIO, "Raio", "o"), Pet(LUZ, "Luz", "a"), Pet(MAROMBA, "Maromba", "o"))

    fun byId(id: String?) = all.firstOrNull { it.id == id } ?: default
}

object Shop {
    const val FREEZE_ID = "freeze"
    const val REPAIR_ID = "streak_repair"
    const val REPAIR_PRICE = 100
    val items = listOf(
        ShopItem(FREEZE_ID, "Bloqueio de streak", "Protege seus streaks em um dia perdido", 50, ShopKind.FREEZE),
        ShopItem(REPAIR_ID, "Retomar sequência", "Recupera um streak quebrado recente", REPAIR_PRICE, ShopKind.REPAIR),
        ShopItem(Pets.RAIO, "Raio", "Elétrico e sempre pronto para a próxima", 150, ShopKind.PET),
        ShopItem(Pets.LUZ, "Luz", "Serena, ilumina até os dias difíceis", 150, ShopKind.PET),
        ShopItem(Pets.MAROMBA, "Maromba", "Fogo bruto que não pula um dia de treino", 300, ShopKind.PET),
        ShopItem("theme_ocean", "Tema Azul", "Azul profundo com detalhes em branco", 100, ShopKind.THEME),
        ShopItem("theme_white", "Tema Branco", "Branco limpo com detalhes em roxo", 100, ShopKind.THEME),
        ShopItem("theme_black", "Tema Preto", "Preto marcante com detalhes em menta", 100, ShopKind.THEME),
        ShopItem("theme_gold", "Tema Ouro", "Tons quentes com detalhes dourados", 0, ShopKind.THEME, proOnly = true),
        ShopItem("theme_emerald", "Tema Esmeralda", "Verde profundo e sereno", 0, ShopKind.THEME, proOnly = true),
    )

    fun isProOnly(id: String) = items.any { it.id == id && it.proOnly }
}

data class IconGroup(val name: String, val icons: List<String>)

/** Ícones de objetivos e listas, separados por assunto para serem fáceis de achar. */
object IconCatalog {
    val groups = listOf(
        IconGroup("Saúde", listOf("💧", "🏃", "🚶", "🏋️", "🚴", "🏊", "🧘", "🥗", "🍎", "💊", "🛏️", "🦷", "❤️", "🧠", "🚭", "⚖️", "👟", "💉")),
        IconGroup("Estudo", listOf("📚", "📖", "✍️", "📝", "🎓", "🔬", "🌍", "🗣️", "🎧", "💡", "📐", "🔤", "🖊️", "📓", "🎒", "🏫", "🔭", "📜")),
        IconGroup("Trabalho", listOf("💻", "💼", "📊", "📈", "📅", "⏰", "📞", "📧", "🎯", "✅", "🚀", "💰", "💳", "🏦", "🖨️", "📌", "🗂️", "⌨️")),
        IconGroup("Casa", listOf("🏠", "🧹", "🍳", "🛒", "🍽️", "🌱", "🐶", "🐱", "🔧", "🚿", "🗑️", "👕", "🛋️", "🔑", "🔌", "🌻", "🚪", "🚗")),
        IconGroup("Comida", listOf("🥦", "🥕", "🍌", "🍇", "🍗", "🥩", "🐟", "🥚", "🍚", "🍞", "🧀", "🥛", "🍕", "🍔", "🍰", "🍫", "🥤", "☕")),
        IconGroup("Lazer", listOf("🎸", "🎹", "🎮", "🎨", "📷", "🎬", "🎵", "✈️", "🏖️", "⚽", "🏀", "🎾", "🎲", "🎭", "🎤", "🏕️", "🚢", "🏄")),
        IconGroup("Bem-estar", listOf("☀️", "🌙", "😊", "🙏", "🌿", "🌸", "📵", "🛁", "🌊", "⭐", "🎁", "🔥", "🌈", "🕯️", "🤝", "👪", "💬", "🏆")),
    )

    /** Primeiro grupo que contém [icon], para abrir o seletor já no ícone escolhido. */
    fun groupOf(icon: String) = groups.indexOfFirst { icon in it.icons }.coerceAtLeast(0)
}
