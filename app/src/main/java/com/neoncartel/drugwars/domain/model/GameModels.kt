package com.neoncartel.drugwars.domain.model

enum class ItemId(val label: String) {
    WEED("Leaf"),
    COKE("Pearl"),
    HASH("Resin"),
    LUDES("Sleepers"),
    METH("Glass"),
    HEROIN("Velvet"),
    OXYS("Blue Tabs"),
    VIAGRA("Vigor"),
    K2("Spice"),
    CRACK("Rocks"),
    DABS("Wax"),
    MOONSHINE("Moonshine"),
    MDMA("Pulse"),
    FLAKKA("Static"),
    DMT("Prisms"),
    SHROOMS("Caps"),
    SOMAS("Drifters"),
    XANAX("Calmers"),
    MORPHINE("Mercy"),
}

enum class CityId(val label: String) {
    NEON_HARBOR("Neon Harbor"),
    DUSTLINE("Dustline"),
    IRON_DELTA("Iron Delta"),
    VELVET_STRIP("Velvet Strip"),
    RAIN_SPIRE("Rain Spire"),
    BLACKWATER_PORT("Blackwater Port"),
    FROST_GATE("Frost Gate"),
    SUNKEN_MARKET("Sunken Market"),
    ASH_BOROUGH("Ash Borough"),
    GLASS_MESA("Glass Mesa"),
}

enum class CharacterId(val label: String) {
    MARLOWE("Marlowe"),
    NOVA("Nova"),
    VERA("Vera"),
    KNOX("Knox"),
    RAZOR("Razor"),
    MICA("Mica"),
    BRASS("Brass"),
    ORBIT("Orbit"),
}

enum class Difficulty(
    val label: String,
    val riskMultiplier: Double,
    val priceMultiplier: Double,
    val startingDebt: Int,
) {
    EASY("Easy", 0.78, 0.92, 0),
    NORMAL("Normal", 1.0, 1.0, 0),
    HARD("Hard", 1.25, 1.08, 250),
    NIGHTMARE("Nightmare", 1.55, 1.18, 500),
}

enum class Trend {
    CRASH,
    DOWN,
    STABLE,
    UP,
    JACKPOT,
}

enum class TradeMode {
    BUY,
    SELL,
}

enum class GameStatus {
    ACTIVE,
    JAILED,
    DEAD,
    RETIRED,
}

enum class EventSeverity(val weight: Int) {
    INFO(0),
    GOOD(1),
    WARNING(2),
    DANGER(3),
    LEGENDARY(4),
}

data class ItemDefinition(
    val id: ItemId,
    val baseValue: Int,
    val volatility: Double,
    val rarity: Double,
    val weight: Int,
    val iconSeed: Int,
)

data class CharacterDefinition(
    val id: CharacterId,
    val archetype: String,
    val flavor: String,
    val capacityBonus: Int = 0,
    val negotiationBonus: Double = 0.0,
    val heatReduction: Int = 0,
    val escapeBonus: Double = 0.0,
    val rareMarketBonus: Double = 0.0,
    val stashProtection: Double = 0.0,
)

data class City(
    val id: CityId,
    val danger: Int,
    val law: Int,
    val gang: Int,
    val priceIndex: Double,
    val palette: List<String>,
    val weather: String,
    val skylineSeed: Int,
    val affinities: Map<ItemId, Double>,
)

data class PlayerStats(
    val characterId: CharacterId,
    val health: Int = 100,
    val cash: Int = 500,
    val debt: Int = 0,
    val capacity: Int = 30,
    val heat: Int = 0,
    val reputation: Int = 0,
    val risk: Int = 8,
    val gangPressure: Int = 0,
)

data class MarketListing(
    val itemId: ItemId,
    val price: Int,
    val trend: Trend,
    val available: Int,
    val previousPrice: Int,
    val marginHint: Int,
    val eventTag: String? = null,
)

data class MarketState(
    val cityId: CityId,
    val day: Int,
    val items: List<MarketListing>,
    val news: String,
)

data class GameEvent(
    val id: String,
    val day: Int,
    val title: String,
    val body: String,
    val severity: EventSeverity,
)

data class GameState(
    val seed: Long,
    val rngCursor: Int,
    val difficulty: Difficulty,
    val status: GameStatus,
    val day: Int,
    val currentCityId: CityId,
    val player: PlayerStats,
    val cities: List<City>,
    val market: MarketState,
    val inventory: Map<ItemId, Int>,
    val hiddenStash: Map<ItemId, Int>,
    val timeline: List<GameEvent>,
    val audioEnabled: Boolean = true,
) {
    val currentCity: City
        get() = cities.first { it.id == currentCityId }

    val usedCapacity: Int
        get() = inventory.entries.sumOf { (itemId, quantity) ->
            val definition = com.neoncartel.drugwars.domain.content.GameCatalog.item(itemId)
            definition.weight * quantity
        }
}

data class GameEffect(
    val sound: String? = null,
    val message: String? = null,
)

data class GameResult(
    val state: GameState,
    val effects: List<GameEffect> = emptyList(),
)

sealed interface GameAction {
    data class Trade(
        val itemId: ItemId,
        val quantity: Int,
        val mode: TradeMode,
    ) : GameAction

    data class Travel(val destination: CityId) : GameAction
    data object Hide : GameAction
    data object Rest : GameAction
    data object UpgradeCapacity : GameAction
    data object BribeOfficials : GameAction
    data object VisitContact : GameAction
    data object Gamble : GameAction
    data object ToggleAudio : GameAction
    data object Retire : GameAction
}
