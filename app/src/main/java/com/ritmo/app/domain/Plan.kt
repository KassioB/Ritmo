package com.ritmo.app.domain

/**
 * O que cada plano libera. Telas e repositório consultam daqui, então mudar um limite é mudar uma
 * linha. Limite nulo significa sem limite. Os limites só impedem criar mais: nada que o usuário já
 * tem é apagado ou escondido quando ele volta para o plano grátis.
 */
data class Plan(
    val isPro: Boolean,
    val maxObjectives: Int?,
    val maxRemindersPerObjective: Int?,
    val maxLists: Int?,
    val maxItemsPerList: Int?,
    val ranges: List<Range>,
    val maxFreezes: Int,
    val monthlyFreezes: Int,
    val repairWindowDays: Int,
    val repairPrice: Int,
    val coinMultiplier: Int,
) {
    fun canAddObjective(active: Int) = fits(active, maxObjectives)

    fun canAddReminder(count: Int) = fits(count, maxRemindersPerObjective)

    fun canAddList(count: Int) = fits(count, maxLists)

    fun canAddItem(count: Int) = fits(count, maxItemsPerList)

    fun priceOf(item: ShopItem) = if (item.kind == ShopKind.REPAIR) repairPrice else item.price

    fun allows(item: ShopItem) = isPro || !item.proOnly

    private fun fits(count: Int, limit: Int?) = limit == null || count < limit

    companion object {
        val FREE = Plan(
            isPro = false,
            maxObjectives = 5,
            maxRemindersPerObjective = 2,
            maxLists = 3,
            maxItemsPerList = 30,
            ranges = listOf(Range.WEEK),
            maxFreezes = Progression.MAX_FREEZES,
            monthlyFreezes = 0,
            repairWindowDays = Streaks.REPAIR_WINDOW_DAYS,
            repairPrice = Shop.REPAIR_PRICE,
            coinMultiplier = 1,
        )

        val PRO = Plan(
            isPro = true,
            maxObjectives = null,
            maxRemindersPerObjective = null,
            maxLists = null,
            maxItemsPerList = null,
            ranges = Range.entries,
            maxFreezes = 5,
            monthlyFreezes = 2,
            repairWindowDays = 7,
            repairPrice = Shop.REPAIR_PRICE / 2,
            coinMultiplier = 2,
        )

        fun of(isPro: Boolean) = if (isPro) PRO else FREE
    }
}
