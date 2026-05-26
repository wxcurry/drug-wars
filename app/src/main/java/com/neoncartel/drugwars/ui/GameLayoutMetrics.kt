package com.neoncartel.drugwars.ui

import kotlin.math.ceil
import kotlin.math.roundToInt

data class PortraitGameLayoutMetrics(
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val horizontalPaddingDp: Int,
    val bottomPaddingDp: Int,
    val sectionGapDp: Int,
    val cityHeightDp: Int,
    val actionRowHeightDp: Int,
    val actionGapDp: Int,
    val marketHeaderHeightDp: Int,
    val marketGridGapDp: Int,
    val marketColumns: Int,
    val marketRows: Int,
    val minimumMarketCardHeightDp: Int,
    val marketFitsWithoutScroll: Boolean,
)

object GameLayoutMetrics {
    const val MaxStandardMarketItems = 14

    fun portrait(
        screenWidthDp: Int,
        screenHeightDp: Int,
        marketItemCount: Int,
    ): PortraitGameLayoutMetrics {
        val safeWidth = screenWidthDp.coerceAtLeast(320)
        val safeHeight = screenHeightDp.coerceAtLeast(568)
        val visibleItems = marketItemCount.coerceIn(1, MaxStandardMarketItems)
        val columns = 2
        val rows = ceil(visibleItems / columns.toDouble()).toInt().coerceAtLeast(1)
        val short = safeHeight < 700
        val narrow = safeWidth < 380
        val horizontalPadding = if (narrow) 6 else 8
        val bottomPadding = if (short) 4 else 6
        val sectionGap = if (short) 4 else 6
        val actionHeight = if (short) 42 else 46
        val actionGap = if (narrow) 3 else 4
        val marketHeader = if (short) 24 else 30
        val marketGridGap = if (short) 3 else 4
        val minimumCardHeight = if (short) 46 else 52
        val fixedMarketHeight = marketHeader + (rows - 1) * marketGridGap + rows * minimumCardHeight
        val availableCityHeight = safeHeight - bottomPadding - sectionGap * 2 - actionHeight - fixedMarketHeight
        val naturalCityHeight = (safeHeight * if (short) 0.25f else 0.30f).roundToInt()
        val cityMinimum = if (short) 138 else 176
        val cityMaximum = if (short) 176 else 250
        val cityHeight = naturalCityHeight
            .coerceIn(cityMinimum, cityMaximum)
            .coerceAtMost(availableCityHeight)
            .coerceAtLeast(132)
        val requiredHeight = cityHeight +
            sectionGap * 2 +
            actionHeight +
            fixedMarketHeight +
            bottomPadding

        return PortraitGameLayoutMetrics(
            screenWidthDp = safeWidth,
            screenHeightDp = safeHeight,
            horizontalPaddingDp = horizontalPadding,
            bottomPaddingDp = bottomPadding,
            sectionGapDp = sectionGap,
            cityHeightDp = cityHeight,
            actionRowHeightDp = actionHeight,
            actionGapDp = actionGap,
            marketHeaderHeightDp = marketHeader,
            marketGridGapDp = marketGridGap,
            marketColumns = columns,
            marketRows = rows,
            minimumMarketCardHeightDp = minimumCardHeight,
            marketFitsWithoutScroll = requiredHeight <= safeHeight,
        )
    }
}

object TradeControlMetrics {
    const val BuyLabel = "Buy"
    const val SellLabel = "Sell"
    const val RailWidthDp = 34
    const val ButtonHeightDp = 17
    const val ButtonGapDp = 1
    const val CornerRadiusDp = 4
    const val TotalRailHeightDp = ButtonHeightDp * 2 + ButtonGapDp
}

object TradeQuantityRules {
    fun maxBuyQuantity(
        available: Int,
        cash: Int,
        price: Int,
        capacityLeft: Int,
        itemWeight: Int,
    ): Int {
        if (available <= 0 || cash <= 0 || price <= 0 || capacityLeft <= 0 || itemWeight <= 0) return 0
        return minOf(
            available,
            cash / price,
            capacityLeft / itemWeight,
        ).coerceAtLeast(0)
    }

    fun maxSellQuantity(owned: Int): Int = owned.coerceAtLeast(0)

    fun sanitizeQuantity(input: String, maxQuantity: Int): Int {
        if (maxQuantity <= 0) return 0
        val parsed = input.filter { it.isDigit() }.toIntOrNull() ?: 1
        return parsed.coerceIn(1, maxQuantity)
    }
}
