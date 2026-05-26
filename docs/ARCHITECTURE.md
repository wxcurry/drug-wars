# Architecture

Drug Wars uses a clean, layered Android architecture.

## Domain

`domain/model` contains immutable game state, action, city, item, character, and market models.

`domain/content/GameCatalog.kt` is the local content database for:

- 19 market goods
- 8 operators
- 10 cities

`domain/system/GameEngine.kt` is a deterministic reducer:

```text
GameState + GameAction -> GameResult
```

The reducer owns trading, market generation, travel, risk, police, gang, upgrades, contacts, gambling, and retirement. It does not depend on Android APIs, so unit tests run on the JVM.

## Data

Room persists one autosave slot in `save_slots`.

`GameStateCodec` encodes the full playable state: player stats, city, day, inventory, hidden stash, current market listings, finite quantities, news, event feed, and audio setting.

`SaveSlotRepository` exposes a Flow-backed saved game stream and suspend functions for load/save/clear.

## App

`AppContainer` wires Room, repository, and `GameEngine`.

`GameViewModel` owns UI state:

- load saved run on startup
- create new run
- dispatch gameplay actions
- autosave after every state transition
- clear save

## UI

The UI is Jetpack Compose and Material 3.

Navigation Compose routes:

- `select`
- `game`
- `travel`
- `settings`

The main game screen is portrait-first and responsive. It presents:

- left player stat panel
- center animated city skyline
- right street feed on larger screens
- bottom market/action interface

Procedural art is rendered in Compose Canvas, while generated vector assets and WAV effects are stored under `app/src/main/res`.

## Testing

Unit tests cover:

- new game defaults and market creation
- buy/sell inventory and cash mutation
- travel day/city/market mutation
- escalating heat/gang danger
- capacity upgrades
- save codec round trip
