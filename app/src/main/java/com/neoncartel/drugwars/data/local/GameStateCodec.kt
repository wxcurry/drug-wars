package com.neoncartel.drugwars.data.local

import com.neoncartel.drugwars.domain.content.GameCatalog
import com.neoncartel.drugwars.domain.model.CharacterId
import com.neoncartel.drugwars.domain.model.CityId
import com.neoncartel.drugwars.domain.model.Difficulty
import com.neoncartel.drugwars.domain.model.EventSeverity
import com.neoncartel.drugwars.domain.model.GameEvent
import com.neoncartel.drugwars.domain.model.GameState
import com.neoncartel.drugwars.domain.model.GameStatus
import com.neoncartel.drugwars.domain.model.ItemId
import com.neoncartel.drugwars.domain.model.MarketQuality
import com.neoncartel.drugwars.domain.model.MarketListing
import com.neoncartel.drugwars.domain.model.MarketRarity
import com.neoncartel.drugwars.domain.model.MarketState
import com.neoncartel.drugwars.domain.model.PlayerStats
import com.neoncartel.drugwars.domain.model.Trend
import java.nio.charset.StandardCharsets
import java.util.Base64

object GameStateCodec {
    fun encode(state: GameState): String = buildString {
        appendLine("version=1")
        appendLine(
            "state=${listOf(
                state.seed,
                state.rngCursor,
                state.difficulty.name,
                state.status.name,
                state.day,
                state.currentCityId.name,
                state.audioEnabled,
            ).joinToString(",")}",
        )
        appendLine(
            "player=${listOf(
                state.player.characterId.name,
                state.player.health,
                state.player.cash,
                state.player.debt,
                state.player.capacity,
                state.player.heat,
                state.player.reputation,
                state.player.risk,
                state.player.gangPressure,
            ).joinToString(",")}",
        )
        appendLine("inventory=${encodeInventory(state.inventory)}")
        appendLine("stash=${encodeInventory(state.hiddenStash)}")
        appendLine("market=${state.market.cityId.name},${state.market.day},${safe(state.market.news)}")
        appendLine("listings=${encodeListings(state.market.items)}")
        appendLine("events=${encodeEvents(state.timeline)}")
    }

    fun decode(payload: String): GameState {
        val lines = payload
            .lineSequence()
            .filter { it.contains("=") }
            .associate {
                val index = it.indexOf("=")
                it.substring(0, index) to it.substring(index + 1)
            }
        require(lines["version"] == "1") { "Unsupported save version" }
        val stateParts = lines.getValue("state").split(",")
        val playerParts = lines.getValue("player").split(",")
        val marketParts = lines.getValue("market").split(",", limit = 3)
        val market = MarketState(
            cityId = CityId.valueOf(marketParts[0]),
            day = marketParts[1].toInt(),
            news = unsafe(marketParts[2]),
            items = decodeListings(lines["listings"].orEmpty()),
        )
        val player = PlayerStats(
            characterId = CharacterId.valueOf(playerParts[0]),
            health = playerParts[1].toInt(),
            cash = playerParts[2].toInt(),
            debt = playerParts[3].toInt(),
            capacity = playerParts[4].toInt(),
            heat = playerParts[5].toInt(),
            reputation = playerParts[6].toInt(),
            risk = playerParts[7].toInt(),
            gangPressure = playerParts[8].toInt(),
        )
        return GameState(
            seed = stateParts[0].toLong(),
            rngCursor = stateParts[1].toInt(),
            difficulty = Difficulty.valueOf(stateParts[2]),
            status = GameStatus.valueOf(stateParts[3]),
            day = stateParts[4].toInt(),
            currentCityId = CityId.valueOf(stateParts[5]),
            audioEnabled = stateParts[6].toBoolean(),
            player = player,
            cities = GameCatalog.cities,
            market = market,
            inventory = decodeInventory(lines["inventory"].orEmpty()),
            hiddenStash = decodeInventory(lines["stash"].orEmpty()),
            timeline = decodeEvents(lines["events"].orEmpty()),
        )
    }

    private fun encodeInventory(inventory: Map<ItemId, Int>): String =
        inventory.entries
            .sortedBy { it.key.name }
            .joinToString(";") { "${it.key.name}:${it.value}" }

    private fun decodeInventory(value: String): Map<ItemId, Int> =
        value.split(";")
            .filter { it.isNotBlank() }
            .associate {
                val parts = it.split(":", limit = 2)
                ItemId.valueOf(parts[0]) to parts[1].toInt()
            }

    private fun encodeListings(items: List<MarketListing>): String =
        items.joinToString(";") { listing ->
            listOf(
                listing.itemId.name,
                listing.price,
                listing.trend.name,
                listing.available,
                listing.previousPrice,
                listing.marginHint,
                safe(listing.eventTag.orEmpty()),
                listing.rarity.name,
                listing.quality.name,
            ).joinToString(":")
        }

    private fun decodeListings(value: String): List<MarketListing> =
        value.split(";")
            .filter { it.isNotBlank() }
            .map {
                val parts = it.split(":", limit = 9)
                MarketListing(
                    itemId = ItemId.valueOf(parts[0]),
                    price = parts[1].toInt(),
                    trend = Trend.valueOf(parts[2]),
                    available = parts[3].toInt(),
                    previousPrice = parts[4].toInt(),
                    marginHint = parts[5].toInt(),
                    eventTag = unsafe(parts[6]).ifBlank { null },
                    rarity = parts.getOrNull(7)?.let(MarketRarity::valueOf) ?: MarketRarity.COMMON,
                    quality = parts.getOrNull(8)?.let(MarketQuality::valueOf) ?: MarketQuality.STANDARD,
                )
            }

    private fun encodeEvents(events: List<GameEvent>): String =
        events.take(24).joinToString(";") { event ->
            listOf(
                safe(event.id),
                event.day,
                safe(event.title),
                safe(event.body),
                event.severity.name,
            ).joinToString(":")
        }

    private fun decodeEvents(value: String): List<GameEvent> =
        value.split(";")
            .filter { it.isNotBlank() }
            .map {
                val parts = it.split(":", limit = 5)
                GameEvent(
                    id = unsafe(parts[0]),
                    day = parts[1].toInt(),
                    title = unsafe(parts[2]),
                    body = unsafe(parts[3]),
                    severity = EventSeverity.valueOf(parts[4]),
                )
            }

    private fun safe(value: String): String =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun unsafe(value: String): String =
        if (value.isBlank()) {
            ""
        } else {
            String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
        }
}
