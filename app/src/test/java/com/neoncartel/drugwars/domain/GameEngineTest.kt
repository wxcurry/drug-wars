package com.neoncartel.drugwars.domain

import com.neoncartel.drugwars.domain.content.GameCatalog
import com.neoncartel.drugwars.domain.model.CharacterId
import com.neoncartel.drugwars.domain.model.Difficulty
import com.neoncartel.drugwars.domain.model.GameAction
import com.neoncartel.drugwars.domain.model.GameStatus
import com.neoncartel.drugwars.domain.model.ItemId
import com.neoncartel.drugwars.domain.model.MarketQuality
import com.neoncartel.drugwars.domain.model.MarketRarity
import com.neoncartel.drugwars.domain.model.TradeMode
import com.neoncartel.drugwars.domain.system.GameEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {
    private val engine = GameEngine()

    @Test
    fun newGameStartsWithExpectedResourcesAndMarket() {
        val state = engine.newGame(CharacterId.NOVA, Difficulty.NORMAL, seed = 1234L)

        assertEquals(500, state.player.cash)
        assertEquals(0, state.player.debt)
        assertEquals(1, state.day)
        assertEquals(GameStatus.ACTIVE, state.status)
        assertTrue(state.player.capacity >= 28)
        assertTrue(state.market.items.size in 8..14)
        assertTrue(state.market.items.all { it.available > 0 && it.price > 0 })
        assertTrue(state.market.items.all { it.rarity in MarketRarity.entries })
        assertTrue(state.market.items.all { it.quality in MarketQuality.entries })
    }

    @Test
    fun catalogUsesRequestedAlaskaCityNamesInOrder() {
        assertEquals(
            listOf(
                "Anchorage",
                "Fairbanks",
                "Juneau",
                "Sitka",
                "Ketchikan",
                "Wasilla",
                "Kenai",
                "Kodiak",
                "Bethel",
                "Palmer",
                "Nome",
                "Seward",
                "Valdez",
                "Homer",
                "Soldotna",
                "Cordova",
                "Unalaska",
                "Barrow",
                "Kotzebue",
                "Petersburg",
            ),
            GameCatalog.cities.map { it.id.label },
        )
    }

    @Test
    fun newGameStartsInAnchorage() {
        val state = engine.newGame(CharacterId.NOVA, Difficulty.NORMAL, seed = 1234L)

        assertEquals("Anchorage", state.currentCityId.label)
    }

    @Test
    fun rarityAndQualityMultipliersIncreasePricePressure() {
        assertTrue(MarketRarity.RARE.priceMultiplier > MarketRarity.COMMON.priceMultiplier)
        assertTrue(MarketRarity.EXTREMELY_RARE.priceMultiplier > MarketRarity.RARE.priceMultiplier)
        assertTrue(MarketQuality.GREAT.priceMultiplier > MarketQuality.STANDARD.priceMultiplier)
        assertTrue(MarketQuality.STANDARD.priceMultiplier > MarketQuality.COMPLETE_SHIT.priceMultiplier)
    }

    @Test
    fun generatedMarketsRandomizeRarityAndQuality() {
        val listings = (1L..80L).flatMap { seed ->
            engine.newGame(CharacterId.NOVA, Difficulty.NORMAL, seed = seed).market.items
        }

        assertTrue(listings.map { it.rarity }.toSet().contains(MarketRarity.RARE))
        assertTrue(listings.map { it.quality }.toSet().size >= 3)
    }

    @Test
    fun extremelyRareListingsRemainScarceAcrossGeneratedMarkets() {
        val listings = (1L..600L).flatMap { seed ->
            engine.newGame(CharacterId.NOVA, Difficulty.NORMAL, seed = seed).market.items
        }
        val extremelyRareCount = listings.count { it.rarity == MarketRarity.EXTREMELY_RARE }

        assertTrue("Expected at least one extremely rare listing across the sample", extremelyRareCount > 0)
        assertTrue(
            "Extremely rare listings should stay below 3% of generated listings but were $extremelyRareCount of ${listings.size}",
            extremelyRareCount.toDouble() / listings.size < 0.03,
        )
    }

    @Test
    fun buyAndSellUpdatesCashInventoryAndReputation() {
        val start = engine.newGame(CharacterId.NOVA, Difficulty.NORMAL, seed = 222L)
        val listing = start.market.items.minBy { it.price }
        val bought = engine.reduce(start, GameAction.Trade(listing.itemId, quantity = 2, mode = TradeMode.BUY)).state
        val sold = engine.reduce(bought, GameAction.Trade(listing.itemId, quantity = 1, mode = TradeMode.SELL)).state

        assertEquals(start.player.cash - listing.price * 2, bought.player.cash)
        assertEquals(2, bought.inventory[listing.itemId])
        assertEquals(bought.player.cash + listing.price, sold.player.cash)
        assertEquals(1, sold.inventory[listing.itemId])
        assertTrue(sold.player.reputation > start.player.reputation)
    }

    @Test
    fun travelAdvancesDayChangesCityAndRegeneratesMarket() {
        val start = engine.newGame(CharacterId.MARLOWE, Difficulty.NORMAL, seed = 777L)
        val destination = start.cities.first { it.id != start.currentCityId }.id
        val result = engine.reduce(start, GameAction.Travel(destination))

        assertEquals(2, result.state.day)
        assertEquals(destination, result.state.currentCityId)
        assertNotEquals(start.market.cityId, result.state.market.cityId)
        assertTrue(result.state.player.cash < start.player.cash)
        assertTrue(result.state.timeline.isNotEmpty())
    }

    @Test
    fun repeatedWaitingIncreasesHeatAndCanTriggerDanger() {
        var state = engine.newGame(CharacterId.RAZOR, Difficulty.HARD, seed = 15L)

        repeat(5) {
            state = engine.reduce(state, GameAction.Hide).state
        }

        assertTrue(state.player.heat >= 16)
        assertTrue(state.player.gangPressure >= 10)
        assertTrue(state.timeline.any { it.severity.weight >= 2 })
    }

    @Test
    fun upgradesSpendCashAndIncreaseCapacity() {
        val start = engine.newGame(CharacterId.BRASS, Difficulty.NORMAL, seed = 4321L)
        val upgraded = engine.reduce(start.copy(player = start.player.copy(cash = 2_000)), GameAction.UpgradeCapacity).state

        assertTrue(upgraded.player.capacity > start.player.capacity)
        assertTrue(upgraded.player.cash < 2_000)
    }
}
