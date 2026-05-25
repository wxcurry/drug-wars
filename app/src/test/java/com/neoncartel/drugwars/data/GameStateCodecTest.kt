package com.neoncartel.drugwars.data

import com.neoncartel.drugwars.data.local.GameStateCodec
import com.neoncartel.drugwars.domain.model.CharacterId
import com.neoncartel.drugwars.domain.model.Difficulty
import com.neoncartel.drugwars.domain.model.GameAction
import com.neoncartel.drugwars.domain.model.ItemId
import com.neoncartel.drugwars.domain.model.TradeMode
import com.neoncartel.drugwars.domain.system.GameEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateCodecTest {
    private val engine = GameEngine()

    @Test
    fun encodedStateRestoresPlayerMarketInventoryAndEvents() {
        val start = engine.newGame(CharacterId.ORBIT, Difficulty.HARD, seed = 98_765L)
        val first = start.market.items.first()
        val bought = engine.reduce(start.copy(player = start.player.copy(cash = 50_000)), GameAction.Trade(first.itemId, 3, TradeMode.BUY)).state
        val moved = engine.reduce(bought, GameAction.Travel(bought.cities.last().id)).state

        val restored = GameStateCodec.decode(GameStateCodec.encode(moved))

        assertEquals(moved.seed, restored.seed)
        assertEquals(moved.day, restored.day)
        assertEquals(moved.currentCityId, restored.currentCityId)
        assertEquals(moved.player, restored.player)
        assertEquals(moved.inventory, restored.inventory)
        assertEquals(moved.market.cityId, restored.market.cityId)
        assertEquals(moved.market.items, restored.market.items)
        assertEquals(moved.timeline.take(6), restored.timeline.take(6))
        assertTrue(restored.inventory[ItemId.valueOf(first.itemId.name)] == moved.inventory[first.itemId])
    }
}
