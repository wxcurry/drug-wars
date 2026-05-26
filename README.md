# Drug Wars

Native Android strategy/trading game inspired by classic territory market games, rebuilt with Kotlin, Jetpack Compose, Material 3, Room, coroutines, Flow, and Navigation Compose.

The player starts with `$500`, a small pack, low reputation, and a risky route through ten distinct cities. Buy low, travel, sell high, manage heat and gang pressure, upgrade capacity, bribe officials, visit contacts, gamble, and survive arrests or ambushes.

## Build

```bash
./gradlew :app:assembleDebug
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Run checks:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lint
```

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- MVVM with a clean domain/data/UI separation
- Room database autosave
- Coroutines and Flow
- Navigation Compose
- Gradle Kotlin DSL
- Procedural Compose art and local WAV effects

## Gameplay Systems

- 8 playable operators with unique passive bonuses.
- 10 cities with distinct danger, law, gang, pricing, weather, skyline palette, and regional supply bias.
- 19 stylized contraband/underground goods with base values, rarity, volatility, finite city quantities, shortages, surpluses, crashes, and jackpot buyer events.
- Deterministic market generation per seed, city, day, and action cursor.
- Risk model that escalates heat and gang pressure when lingering, travelling, trading, or gambling.
- Police sweep, arrest, fine, bail, confiscation, jail-time skip, gang ambush, robbery, damage, and escape outcomes.
- Room-backed autosave after every action.
- Portrait-first responsive dashboard for phones and tablets.

## Repository Structure

```text
app/src/main/java/com/neoncartel/drugwars/domain
  Pure gameplay models, content catalogs, and deterministic reducer engine.

app/src/main/java/com/neoncartel/drugwars/data
  Room database, save slot DAO, repository, and state codec.

app/src/main/java/com/neoncartel/drugwars/app
  Application container and GameViewModel.

app/src/main/java/com/neoncartel/drugwars/ui
  Compose screens, procedural art, Material 3 theme, and navigation.

app/src/main/res
  Generated vector logo/city/item assets and local generated WAV effects.
```

## Screenshots And Release Art

Generated local preview assets live in `screenshots/`:

- `feature-graphic.svg`
- `gameplay-dashboard.svg`
- `character-select.svg`

These are repository graphics generated from the same art direction as the app. Device screenshots can be replaced after running on a target emulator or phone.

## GitHub Flow Used

Major systems were developed on separate branches and merged through PRs:

- PR #1: deterministic gameplay engine
- PR #2: Room autosave state
- PR #3: playable Compose game UI
