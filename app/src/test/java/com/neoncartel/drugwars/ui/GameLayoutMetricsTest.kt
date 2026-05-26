package com.neoncartel.drugwars.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameLayoutMetricsTest {
    @Test
    fun portraitMetricsFitMaximumMarketOnSmallPhone() {
        val metrics = GameLayoutMetrics.portrait(screenWidthDp = 320, screenHeightDp = 568, marketItemCount = 14)

        assertEquals(2, metrics.marketColumns)
        assertEquals(7, metrics.marketRows)
        assertTrue(metrics.marketFitsWithoutScroll)
        assertTrue(metrics.cityHeightDp >= 138)
        assertTrue(metrics.minimumMarketCardHeightDp >= 46)
    }

    @Test
    fun portraitMetricsGiveCityImageDominantTopSpaceOnTallPhone() {
        val metrics = GameLayoutMetrics.portrait(screenWidthDp = 390, screenHeightDp = 844, marketItemCount = 14)

        assertTrue(metrics.marketFitsWithoutScroll)
        assertTrue(metrics.cityHeightDp >= 236)
        assertTrue(metrics.cityHeightDp > metrics.actionRowHeightDp * 5)
    }

    @Test
    fun portraitMetricsKeepEveryStandardInventoryVisibleAcrossPhoneSizes() {
        val phoneSizes = listOf(
            360 to 640,
            375 to 667,
            390 to 844,
            412 to 915,
        )

        phoneSizes.forEach { (width, height) ->
            val metrics = GameLayoutMetrics.portrait(width, height, marketItemCount = 14)

            assertTrue("Expected market to fit at ${width}x$height", metrics.marketFitsWithoutScroll)
            assertTrue(metrics.minimumMarketCardHeightDp >= 46)
        }
    }

    @Test
    fun tradeControlsStayCompactAndExplicit() {
        assertEquals("Buy", TradeControlMetrics.BuyLabel)
        assertEquals("Sell", TradeControlMetrics.SellLabel)
        assertTrue(TradeControlMetrics.RailWidthDp <= 34)
        assertTrue(TradeControlMetrics.TotalRailHeightDp <= 36)
    }

    @Test
    fun menuBoxFrameUsesRequestedEdgeSizeWhenSpaceAllows() {
        val insets = MenuBoxFrameGeometry.destinationInsets(
            width = 320f,
            height = 160f,
            horizontalEdge = 28f,
            verticalEdge = 24f,
        )

        assertEquals(28f, insets.left)
        assertEquals(28f, insets.right)
        assertEquals(24f, insets.top)
        assertEquals(24f, insets.bottom)
    }

    @Test
    fun menuBoxFrameClampsEdgesForTinyBoxes() {
        val insets = MenuBoxFrameGeometry.destinationInsets(
            width = 42f,
            height = 30f,
            horizontalEdge = 28f,
            verticalEdge = 24f,
        )

        assertEquals(21f, insets.left)
        assertEquals(21f, insets.right)
        assertEquals(15f, insets.top)
        assertEquals(15f, insets.bottom)
    }

    @Test
    fun buyQuantityIsLimitedBySupplyCashAndCapacity() {
        assertEquals(
            3,
            TradeQuantityRules.maxBuyQuantity(
                available = 12,
                cash = 1_000,
                price = 100,
                capacityLeft = 6,
                itemWeight = 2,
            ),
        )
        assertEquals(
            2,
            TradeQuantityRules.maxBuyQuantity(
                available = 12,
                cash = 250,
                price = 100,
                capacityLeft = 20,
                itemWeight = 1,
            ),
        )
        assertEquals(
            0,
            TradeQuantityRules.maxBuyQuantity(
                available = 12,
                cash = 1_000,
                price = 100,
                capacityLeft = 0,
                itemWeight = 1,
            ),
        )
    }

    @Test
    fun sellQuantityIsLimitedByOwnedInventory() {
        assertEquals(7, TradeQuantityRules.maxSellQuantity(owned = 7))
        assertEquals(0, TradeQuantityRules.maxSellQuantity(owned = -2))
    }

    @Test
    fun quantityInputIsClampedToAllowedRange() {
        assertEquals(1, TradeQuantityRules.sanitizeQuantity("", maxQuantity = 8))
        assertEquals(1, TradeQuantityRules.sanitizeQuantity("0", maxQuantity = 8))
        assertEquals(8, TradeQuantityRules.sanitizeQuantity("50", maxQuantity = 8))
        assertEquals(4, TradeQuantityRules.sanitizeQuantity("4a", maxQuantity = 8))
        assertEquals(0, TradeQuantityRules.sanitizeQuantity("4", maxQuantity = 0))
    }
}
