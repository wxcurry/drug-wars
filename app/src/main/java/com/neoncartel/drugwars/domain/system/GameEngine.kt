package com.neoncartel.drugwars.domain.system

import com.neoncartel.drugwars.domain.content.GameCatalog
import com.neoncartel.drugwars.domain.model.CharacterId
import com.neoncartel.drugwars.domain.model.CityId
import com.neoncartel.drugwars.domain.model.Difficulty
import com.neoncartel.drugwars.domain.model.EventSeverity
import com.neoncartel.drugwars.domain.model.GameAction
import com.neoncartel.drugwars.domain.model.GameEffect
import com.neoncartel.drugwars.domain.model.GameEvent
import com.neoncartel.drugwars.domain.model.GameResult
import com.neoncartel.drugwars.domain.model.GameState
import com.neoncartel.drugwars.domain.model.GameStatus
import com.neoncartel.drugwars.domain.model.ItemId
import com.neoncartel.drugwars.domain.model.MarketListing
import com.neoncartel.drugwars.domain.model.MarketState
import com.neoncartel.drugwars.domain.model.PlayerStats
import com.neoncartel.drugwars.domain.model.TradeMode
import com.neoncartel.drugwars.domain.model.Trend
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class GameEngine {
    fun newGame(
        characterId: CharacterId,
        difficulty: Difficulty,
        seed: Long = System.currentTimeMillis(),
    ): GameState {
        val character = GameCatalog.character(characterId)
        val startingCity = GameCatalog.cities.first().id
        val player = PlayerStats(
            characterId = characterId,
            debt = difficulty.startingDebt,
            capacity = 30 + character.capacityBonus,
            risk = (8 * difficulty.riskMultiplier).roundToInt(),
        )
        val market = generateMarket(
            cityId = startingCity,
            day = 1,
            seed = seed,
            cursor = 0,
            difficulty = difficulty,
            rareBonus = character.rareMarketBonus,
            previous = emptyMap(),
        )
        return GameState(
            seed = seed,
            rngCursor = 1,
            difficulty = difficulty,
            status = GameStatus.ACTIVE,
            day = 1,
            currentCityId = startingCity,
            player = player,
            cities = GameCatalog.cities,
            market = market,
            inventory = emptyMap(),
            hiddenStash = emptyMap(),
            timeline = listOf(
                GameEvent(
                    id = "start",
                    day = 1,
                    title = "${character.id.label} enters ${startingCity.label}",
                    body = "${character.flavor} Start with $500, a small pack, and a city full of bad prices.",
                    severity = EventSeverity.INFO,
                ),
            ),
        )
    }

    fun reduce(state: GameState, action: GameAction): GameResult {
        if (state.status != GameStatus.ACTIVE && action !is GameAction.ToggleAudio) {
            return GameResult(state, listOf(GameEffect(message = "This run is no longer active.")))
        }
        return when (action) {
            is GameAction.Trade -> trade(state, action.itemId, action.quantity, action.mode)
            is GameAction.Travel -> travel(state, action.destination)
            GameAction.Hide -> hide(state)
            GameAction.Rest -> rest(state)
            GameAction.UpgradeCapacity -> upgradeCapacity(state)
            GameAction.BribeOfficials -> bribe(state)
            GameAction.VisitContact -> contact(state)
            GameAction.Gamble -> gamble(state)
            GameAction.ToggleAudio -> GameResult(state.copy(audioEnabled = !state.audioEnabled))
            GameAction.Retire -> GameResult(
                appendEvent(
                    state.copy(status = GameStatus.RETIRED),
                    "Retired",
                    "You cash out with $${state.player.cash} and ${state.player.reputation} reputation.",
                    EventSeverity.GOOD,
                ),
                listOf(GameEffect(sound = "ui_confirm", message = "Run retired.")),
            )
        }
    }

    private fun trade(
        state: GameState,
        itemId: ItemId,
        requestedQuantity: Int,
        mode: TradeMode,
    ): GameResult {
        val quantity = requestedQuantity.coerceAtLeast(0)
        if (quantity == 0) return GameResult(state)
        val listing = state.market.items.firstOrNull { it.itemId == itemId }
            ?: return blocked(state, "No local supply", "${itemId.label} is not moving in ${state.currentCityId.label}.")
        val character = GameCatalog.character(state.player.characterId)
        val definition = GameCatalog.item(itemId)
        return when (mode) {
            TradeMode.BUY -> {
                val price = discounted(listing.price, character.negotiationBonus)
                val cashLimited = state.player.cash / price
                val capacityLeft = max(0, state.player.capacity - state.usedCapacity)
                val capacityLimited = capacityLeft / definition.weight
                val bought = min(quantity, min(listing.available, min(cashLimited, capacityLimited)))
                if (bought <= 0) {
                    blocked(state, "Deal blocked", "You need more cash or inventory room.")
                } else {
                    val cost = price * bought
                    val inventory = state.inventory.plusQuantity(itemId, bought)
                    val market = state.market.adjustAvailability(itemId, -bought)
                    val updated = state.copy(
                        player = state.player.copy(
                            cash = state.player.cash - cost,
                            reputation = state.player.reputation + max(1, bought / 2),
                            risk = (state.player.risk + 1).coerceAtMost(100),
                        ),
                        inventory = inventory,
                        market = market,
                    )
                    GameResult(
                        appendEvent(updated, "Bought ${itemId.label}", "Moved $bought units for $$cost.", EventSeverity.INFO),
                        listOf(GameEffect(sound = "buy", message = "Bought $bought ${itemId.label}.")),
                    )
                }
            }

            TradeMode.SELL -> {
                val owned = state.inventory[itemId] ?: 0
                val sold = min(quantity, owned)
                if (sold <= 0) {
                    blocked(state, "Nothing to sell", "Your pack has no ${itemId.label}.")
                } else {
                    val price = markedUp(listing.price, character.negotiationBonus)
                    val revenue = price * sold
                    val inventory = state.inventory.plusQuantity(itemId, -sold)
                    val updated = state.copy(
                        player = state.player.copy(
                            cash = state.player.cash + revenue,
                            reputation = state.player.reputation + max(1, sold),
                            heat = (state.player.heat + 1 - character.heatReduction).coerceAtLeast(0),
                        ),
                        inventory = inventory,
                    )
                    GameResult(
                        appendEvent(updated, "Sold ${itemId.label}", "Cleared $sold units for $$revenue.", EventSeverity.GOOD),
                        listOf(GameEffect(sound = "sell", message = "Sold $sold ${itemId.label}.")),
                    )
                }
            }
        }
    }

    private fun travel(state: GameState, destination: CityId): GameResult {
        if (destination == state.currentCityId) {
            return blocked(state, "Already here", "You are already working ${destination.label}.")
        }
        val destinationCity = GameCatalog.city(destination)
        val random = state.random()
        val cost = (55 + destinationCity.danger * 0.8 + state.day * 2).roundToInt()
        val paid = min(cost, state.player.cash)
        val character = GameCatalog.character(state.player.characterId)
        val heatGain = ((destinationCity.law / 18.0) * state.difficulty.riskMultiplier).roundToInt()
        val gangGain = ((destinationCity.gang / 22.0) * state.difficulty.riskMultiplier).roundToInt()
        val nextDay = state.day + 1
        val previous = state.market.items.associate { it.itemId to it.price }
        val market = generateMarket(
            cityId = destination,
            day = nextDay,
            seed = state.seed,
            cursor = state.rngCursor + 1,
            difficulty = state.difficulty,
            rareBonus = character.rareMarketBonus,
            previous = previous,
        )
        val moved = state.copy(
            day = nextDay,
            currentCityId = destination,
            rngCursor = state.rngCursor + 2,
            player = state.player.copy(
                cash = state.player.cash - paid,
                heat = (state.player.heat + heatGain - character.heatReduction).coerceIn(0, 100),
                gangPressure = (state.player.gangPressure + gangGain).coerceIn(0, 100),
                risk = (state.player.risk + destinationCity.danger / 12).coerceIn(0, 100),
            ),
            market = market,
        )
        val withArrival = appendEvent(
            moved,
            "Arrived in ${destination.label}",
            "Fare paid: $$paid. ${market.news}",
            EventSeverity.INFO,
        )
        val afterEncounter = resolveDanger(withArrival, random, travel = true)
        return GameResult(afterEncounter, listOf(GameEffect(sound = "travel", message = "Arrived in ${destination.label}.")))
    }

    private fun hide(state: GameState): GameResult {
        val city = state.currentCity
        val character = GameCatalog.character(state.player.characterId)
        val next = state.copy(
            day = state.day + 1,
            rngCursor = state.rngCursor + 1,
            player = state.player.copy(
                heat = (state.player.heat + (3 * state.difficulty.riskMultiplier).roundToInt() + city.law / 24 - character.heatReduction).coerceIn(0, 100),
                gangPressure = (state.player.gangPressure + (2 * state.difficulty.riskMultiplier).roundToInt() + city.gang / 28).coerceIn(0, 100),
                risk = (state.player.risk + 2).coerceIn(0, 100),
            ),
            market = regenerateLocalMarket(state, state.day + 1),
        )
        val waited = appendEvent(next, "Stayed hidden", "The city keeps moving without you. Attention still rises.", EventSeverity.WARNING)
        return GameResult(resolveDanger(waited, state.random(), travel = false), listOf(GameEffect(sound = "hide")))
    }

    private fun rest(state: GameState): GameResult {
        val cost = min(90 + state.currentCity.danger, state.player.cash)
        val next = state.copy(
            day = state.day + 1,
            rngCursor = state.rngCursor + 1,
            player = state.player.copy(
                cash = state.player.cash - cost,
                health = (state.player.health + 22).coerceAtMost(100),
                heat = (state.player.heat - 8).coerceAtLeast(0),
                risk = (state.player.risk - 4).coerceAtLeast(0),
            ),
            market = regenerateLocalMarket(state, state.day + 1),
        )
        return GameResult(
            appendEvent(next, "Rested", "Spent $$cost on a quiet room and patch work.", EventSeverity.GOOD),
            listOf(GameEffect(sound = "rest", message = "Recovered health.")),
        )
    }

    private fun upgradeCapacity(state: GameState): GameResult {
        val cost = 450 + state.player.capacity * 9
        if (state.player.cash < cost) {
            return blocked(state, "Upgrade too expensive", "A compartment rig costs $$cost.")
        }
        val next = state.copy(
            player = state.player.copy(
                cash = state.player.cash - cost,
                capacity = state.player.capacity + 10,
                reputation = state.player.reputation + 3,
            ),
        )
        return GameResult(
            appendEvent(next, "Pack upgraded", "Capacity increased by 10 for $$cost.", EventSeverity.GOOD),
            listOf(GameEffect(sound = "upgrade", message = "Capacity upgraded.")),
        )
    }

    private fun bribe(state: GameState): GameResult {
        val cost = 180 + state.player.heat * 8
        if (state.player.cash < cost) {
            return blocked(state, "Bribe refused", "Officials want $$cost to look away.")
        }
        val next = state.copy(
            player = state.player.copy(
                cash = state.player.cash - cost,
                heat = (state.player.heat - 22).coerceAtLeast(0),
                reputation = state.player.reputation + 2,
            ),
        )
        return GameResult(
            appendEvent(next, "Heat scrubbed", "A clerk lost the paperwork for $$cost.", EventSeverity.GOOD),
            listOf(GameEffect(sound = "bribe", message = "Heat reduced.")),
        )
    }

    private fun contact(state: GameState): GameResult {
        val fee = min(120 + state.day * 6, state.player.cash)
        val random = state.random()
        val previous = state.market.items.associate { it.itemId to it.price }
        val boosted = generateMarket(
            cityId = state.currentCityId,
            day = state.day,
            seed = state.seed + 9_991,
            cursor = state.rngCursor + 3,
            difficulty = state.difficulty,
            rareBonus = GameCatalog.character(state.player.characterId).rareMarketBonus + 0.18,
            previous = previous,
        )
        val severity = if (boosted.items.any { it.trend == Trend.JACKPOT }) EventSeverity.LEGENDARY else EventSeverity.GOOD
        val next = state.copy(
            rngCursor = state.rngCursor + 3,
            player = state.player.copy(cash = state.player.cash - fee, reputation = state.player.reputation + 1),
            market = if (random.nextDouble() < 0.8) boosted else state.market,
        )
        return GameResult(
            appendEvent(next, "Underground contact", "Paid $$fee for whispers. ${next.market.news}", severity),
            listOf(GameEffect(sound = "contact", message = "Contact checked.")),
        )
    }

    private fun gamble(state: GameState): GameResult {
        val stake = min(100 + state.day * 4, state.player.cash)
        if (stake <= 0) return blocked(state, "No stake", "You need cash to sit at the back-room table.")
        val random = state.random()
        val win = random.nextDouble() < 0.45 + GameCatalog.character(state.player.characterId).negotiationBonus
        val delta = if (win) (stake * random.nextDouble(1.2, 3.4)).roundToInt() else -stake
        val next = state.copy(
            rngCursor = state.rngCursor + 1,
            player = state.player.copy(
                cash = (state.player.cash + delta).coerceAtLeast(0),
                heat = (state.player.heat + 2).coerceIn(0, 100),
                gangPressure = (state.player.gangPressure + 2).coerceIn(0, 100),
            ),
        )
        return GameResult(
            appendEvent(
                next,
                if (win) "Table run" else "Cold table",
                if (win) "Won $$delta in a coded dice game." else "Lost $$stake before the dealer blinked.",
                if (win) EventSeverity.GOOD else EventSeverity.WARNING,
            ),
            listOf(GameEffect(sound = if (win) "sell" else "danger", message = if (win) "Gamble paid." else "Gamble lost.")),
        )
    }

    private fun resolveDanger(state: GameState, random: Random, travel: Boolean): GameState {
        val city = state.currentCity
        val character = GameCatalog.character(state.player.characterId)
        val pressure = (state.player.heat * city.law + state.player.gangPressure * city.gang) / 100.0
        val threshold = 35.0 / state.difficulty.riskMultiplier
        val guaranteed = state.player.heat + state.player.gangPressure > 42
        if (!guaranteed && random.nextDouble(0.0, 100.0) > pressure - threshold) return state

        val policeLeaning = state.player.heat + city.law > state.player.gangPressure + city.gang || random.nextBoolean()
        return if (policeLeaning) {
            val escapeChance = (0.42 + character.escapeBonus - state.player.heat / 180.0).coerceIn(0.08, 0.86)
            if (random.nextDouble() < escapeChance) {
                appendEvent(
                    state.copy(player = state.player.copy(heat = (state.player.heat + 6).coerceIn(0, 100))),
                    "Police sweep dodged",
                    if (travel) "A checkpoint scanned the route, but you slipped through a service lane." else "A raid hit the block, but you were already on the stairs.",
                    EventSeverity.WARNING,
                )
            } else {
                val fine = min(state.player.cash, 120 + state.player.heat * 9 + state.day * 5)
                val confiscated = confiscate(state.inventory, random, 0.45)
                val next = state.copy(
                    day = state.day + 2,
                    player = state.player.copy(
                        cash = state.player.cash - fine,
                        heat = (state.player.heat - 18).coerceAtLeast(0),
                        health = (state.player.health - 8).coerceAtLeast(1),
                    ),
                    inventory = confiscated,
                    market = regenerateLocalMarket(state, state.day + 2),
                )
                appendEvent(next, "Arrest scare", "Bail and fines cost $$fine. Some cargo disappeared in evidence intake.", EventSeverity.DANGER)
            }
        } else {
            val loss = min(state.player.cash, 80 + state.player.gangPressure * 5 + random.nextInt(0, 160))
            val damage = random.nextInt(8, 24)
            val inventory = confiscate(state.inventory, random, 0.28 - character.stashProtection)
            val health = (state.player.health - damage).coerceAtLeast(0)
            val next = state.copy(
                status = if (health <= 0) GameStatus.DEAD else state.status,
                player = state.player.copy(
                    cash = state.player.cash - loss,
                    health = health,
                    gangPressure = (state.player.gangPressure - 10).coerceAtLeast(0),
                    risk = (state.player.risk + 6).coerceIn(0, 100),
                ),
                inventory = inventory,
            )
            appendEvent(next, "Gang ambush", "A crew clipped you for $$loss and $damage health. Some cargo may be gone.", EventSeverity.DANGER)
        }
    }

    private fun generateMarket(
        cityId: CityId,
        day: Int,
        seed: Long,
        cursor: Int,
        difficulty: Difficulty,
        rareBonus: Double,
        previous: Map<ItemId, Int>,
    ): MarketState {
        val city = GameCatalog.city(cityId)
        val random = Random(seed xor (cityId.ordinalKey * 31_337L) xor (day * 971L) xor (cursor * 53L))
        val eventRoll = random.nextDouble()
        val eventItem = GameCatalog.items.random(random).id
        val eventTag = when {
            eventRoll < 0.08 + rareBonus -> "legendary buyer"
            eventRoll < 0.2 -> "shortage"
            eventRoll < 0.34 -> "surplus"
            eventRoll > 0.94 -> "market crash"
            else -> null
        }
        val listings = GameCatalog.items.mapNotNull { item ->
            val affinity = city.affinities[item.id] ?: 1.0
            val spawnChance = (item.rarity * affinity + rareBonus).coerceIn(0.18, 0.96)
            if (random.nextDouble() > spawnChance) return@mapNotNull null
            val eventMultiplier = when {
                item.id == eventItem && eventTag == "legendary buyer" -> random.nextDouble(2.4, 4.5)
                item.id == eventItem && eventTag == "shortage" -> random.nextDouble(1.55, 2.35)
                item.id == eventItem && eventTag == "surplus" -> random.nextDouble(0.35, 0.68)
                item.id == eventItem && eventTag == "market crash" -> random.nextDouble(0.18, 0.42)
                else -> 1.0
            }
            val volatility = random.nextDouble(1.0 - item.volatility / 2.4, 1.0 + item.volatility)
            val price = (item.baseValue * city.priceIndex * difficulty.priceMultiplier * affinity * eventMultiplier * volatility)
                .roundToInt()
                .coerceAtLeast(12)
            val old = previous[item.id] ?: (item.baseValue * city.priceIndex).roundToInt()
            val trend = when {
                eventMultiplier >= 2.3 -> Trend.JACKPOT
                eventMultiplier <= 0.45 -> Trend.CRASH
                price > old * 1.12 -> Trend.UP
                price < old * 0.88 -> Trend.DOWN
                else -> Trend.STABLE
            }
            val quantityBase = ((1500.0 / item.baseValue) * random.nextDouble(0.8, 3.4) * affinity).roundToInt()
            MarketListing(
                itemId = item.id,
                price = price,
                trend = trend,
                available = quantityBase.coerceIn(3, 72),
                previousPrice = old,
                marginHint = (((price - item.baseValue).toDouble() / item.baseValue) * 100).roundToInt(),
                eventTag = if (item.id == eventItem) eventTag else null,
            )
        }.let { seeded ->
            if (seeded.size >= 8) seeded else {
                val missing = GameCatalog.items
                    .filterNot { item -> seeded.any { it.itemId == item.id } }
                    .shuffled(random)
                    .take(8 - seeded.size)
                    .map { item ->
                        val price = (item.baseValue * city.priceIndex * random.nextDouble(0.8, 1.35)).roundToInt()
                        MarketListing(item.id, price, Trend.STABLE, random.nextInt(5, 28), item.baseValue, 0)
                    }
                seeded + missing
            }
        }.sortedBy { it.itemId.label }.take(14)

        val news = when (eventTag) {
            "legendary buyer" -> "${eventItem.label} has a legendary buyer in ${city.id.label}."
            "shortage" -> "${eventItem.label} shortage spikes street prices."
            "surplus" -> "Loose supply pushes ${eventItem.label} prices down."
            "market crash" -> "${eventItem.label} market crash creates a risky entry."
            else -> "${city.weather}. Prices churn across ${listings.size} active goods."
        }
        return MarketState(cityId = cityId, day = day, items = listings, news = news)
    }

    private fun regenerateLocalMarket(state: GameState, day: Int): MarketState {
        val character = GameCatalog.character(state.player.characterId)
        return generateMarket(
            cityId = state.currentCityId,
            day = day,
            seed = state.seed,
            cursor = state.rngCursor + 2,
            difficulty = state.difficulty,
            rareBonus = character.rareMarketBonus,
            previous = state.market.items.associate { it.itemId to it.price },
        )
    }

    private fun blocked(state: GameState, title: String, body: String): GameResult =
        GameResult(
            appendEvent(state, title, body, EventSeverity.WARNING),
            listOf(GameEffect(sound = "ui_error", message = body)),
        )

    private fun appendEvent(
        state: GameState,
        title: String,
        body: String,
        severity: EventSeverity,
    ): GameState {
        val event = GameEvent(
            id = "${state.day}-${state.rngCursor}-${state.timeline.size}-$title",
            day = state.day,
            title = title,
            body = body,
            severity = severity,
        )
        return state.copy(timeline = (listOf(event) + state.timeline).take(24))
    }

    private fun discounted(price: Int, bonus: Double): Int =
        (price * (1.0 - bonus)).roundToInt().coerceAtLeast(1)

    private fun markedUp(price: Int, bonus: Double): Int =
        (price * (1.0 + bonus / 2.0)).roundToInt().coerceAtLeast(1)

    private fun GameState.random(): Random =
        Random(seed xor (rngCursor * 104_729L) xor (day * 7_919L) xor currentCityId.ordinalKey)

    private val CityId.ordinalKey: Long
        get() = ordinal.toLong() + 1L

    private fun Map<ItemId, Int>.plusQuantity(itemId: ItemId, delta: Int): Map<ItemId, Int> {
        val next = ((this[itemId] ?: 0) + delta).coerceAtLeast(0)
        return if (next == 0) this - itemId else this + (itemId to next)
    }

    private fun MarketState.adjustAvailability(itemId: ItemId, delta: Int): MarketState =
        copy(
            items = items.map { listing ->
                if (listing.itemId == itemId) {
                    listing.copy(available = (listing.available + delta).coerceAtLeast(0))
                } else {
                    listing
                }
            },
        )

    private fun confiscate(inventory: Map<ItemId, Int>, random: Random, chance: Double): Map<ItemId, Int> =
        inventory.mapValues { (_, quantity) ->
            if (random.nextDouble() < chance.coerceAtLeast(0.03)) (quantity * random.nextDouble(0.15, 0.7)).roundToInt() else quantity
        }.filterValues { it > 0 }
}
