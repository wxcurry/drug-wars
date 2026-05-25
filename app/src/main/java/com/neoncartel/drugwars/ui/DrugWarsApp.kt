package com.neoncartel.drugwars.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neoncartel.drugwars.app.GameUiState
import com.neoncartel.drugwars.app.GameViewModel
import com.neoncartel.drugwars.audio.Cue
import com.neoncartel.drugwars.audio.GameAudio
import com.neoncartel.drugwars.domain.content.GameCatalog
import com.neoncartel.drugwars.domain.model.CharacterDefinition
import com.neoncartel.drugwars.domain.model.CharacterId
import com.neoncartel.drugwars.domain.model.City
import com.neoncartel.drugwars.domain.model.CityId
import com.neoncartel.drugwars.domain.model.Difficulty
import com.neoncartel.drugwars.domain.model.EventSeverity
import com.neoncartel.drugwars.domain.model.GameAction
import com.neoncartel.drugwars.domain.model.GameEvent
import com.neoncartel.drugwars.domain.model.GameState
import com.neoncartel.drugwars.domain.model.ItemId
import com.neoncartel.drugwars.domain.model.MarketListing
import com.neoncartel.drugwars.domain.model.TradeMode
import com.neoncartel.drugwars.domain.model.Trend
import kotlinx.coroutines.launch

@Composable
fun DrugWarsApp(viewModel: GameViewModel, audio: GameAudio) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val systemDensity = LocalDensity.current
    val gameDensity = remember(systemDensity) {
        Density(systemDensity.density, fontScale = systemDensity.fontScale.coerceAtMost(1.0f))
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    CompositionLocalProvider(LocalDensity provides gameDensity) {
        Scaffold(
            containerColor = Color(0xFF05060A),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF172033), Color(0xFF05060A)),
                            radius = 900f,
                        ),
                    ),
            ) {
                if (uiState.loading) {
                    LoadingScreen()
                } else {
                    NavHost(
                        navController = navController,
                        startDestination = "select",
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        composable("select") {
                            CharacterSelectScreen(
                                uiState = uiState,
                                onCharacter = viewModel::selectCharacter,
                                onDifficulty = viewModel::setDifficulty,
                                onStart = {
                                    audio.play(Cue.Ambient, enabled = true)
                                    viewModel.startNewGame()
                                    navController.navigate("game") {
                                        popUpTo("select") { inclusive = false }
                                    }
                                },
                                onContinue = {
                                    audio.play(Cue.Click, enabled = uiState.game?.audioEnabled ?: true)
                                    navController.navigate("game")
                                },
                            )
                        }
                        composable("game") {
                            val game = uiState.game
                            if (game == null) {
                                EmptyRunScreen(onBack = { navController.navigate("select") })
                            } else {
                                GameScreen(
                                    game = game,
                                    onTrade = { item, mode ->
                                        audio.play(if (mode == TradeMode.BUY) Cue.Buy else Cue.Sell, game.audioEnabled)
                                        viewModel.dispatch(GameAction.Trade(item, 1, mode))
                                    },
                                    onAction = { action, cue ->
                                        audio.play(cue, game.audioEnabled)
                                        viewModel.dispatch(action)
                                    },
                                    onTravel = { navController.navigate("travel") },
                                    onSettings = { navController.navigate("settings") },
                                )
                            }
                        }
                        composable("travel") {
                            val game = uiState.game
                            if (game != null) {
                                TravelScreen(
                                    game = game,
                                    onBack = { navController.popBackStack() },
                                    onTravel = { cityId ->
                                        audio.play(Cue.Travel, game.audioEnabled)
                                        viewModel.dispatch(GameAction.Travel(cityId))
                                        navController.popBackStack()
                                    },
                                )
                            }
                        }
                        composable("settings") {
                            SettingsScreen(
                                uiState = uiState,
                                onBack = { navController.popBackStack() },
                                onToggleAudio = {
                                    val enabled = uiState.game?.audioEnabled ?: true
                                    audio.play(Cue.Click, enabled)
                                    viewModel.dispatch(GameAction.ToggleAudio)
                                },
                                onClear = {
                                    scope.launch {
                                        audio.play(Cue.Danger, true)
                                        viewModel.abandonRun()
                                        navController.navigate("select") {
                                            popUpTo("select") { inclusive = true }
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularProgressIndicator()
            Text("Loading Neon Cartel", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyRunScreen(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("No active run", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Button(onClick = onBack) { Text("Choose a character") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterSelectScreen(
    uiState: GameUiState,
    onCharacter: (CharacterId) -> Unit,
    onDifficulty: (Difficulty) -> Unit,
    onStart: () -> Unit,
    onContinue: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 460.dp || maxWidth < 820.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 14.dp else 24.dp, vertical = if (compact) 8.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "NEON CARTEL",
                        color = Color.White,
                        fontSize = if (compact) 20.sp else 30.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Pick an operator. Buy low, move fast, survive the heat.",
                        color = Color(0xFFCBD5E1),
                        fontSize = if (compact) 12.sp else 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.game != null) {
                        ElevatedButton(onClick = onContinue) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Continue", maxLines = 1)
                        }
                    }
                    Button(onClick = onStart) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("New Run", maxLines = 1)
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Difficulty.entries.forEach { difficulty ->
                    FilterChip(
                        selected = uiState.difficulty == difficulty,
                        onClick = { onDifficulty(difficulty) },
                        label = { Text(difficulty.label, fontSize = if (compact) 11.sp else 13.sp, maxLines = 1) },
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 10.dp),
            ) {
                items(GameCatalog.characters, key = { it.id.name }) { character ->
                    CharacterCard(
                        character = character,
                        selected = uiState.selectedCharacter == character.id,
                        compact = compact,
                        onClick = { onCharacter(character.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CharacterCard(character: CharacterDefinition, selected: Boolean, compact: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF263348)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 98.dp else 112.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp)),
        color = if (selected) Color(0xFF132236) else Color(0xFF0C111D),
    ) {
        Row(
            modifier = Modifier.padding(if (compact) 8.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
        ) {
            CharacterPortrait(character, Modifier.size(if (compact) 62.dp else 78.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    character.id.label,
                    color = Color.White,
                    fontSize = if (compact) 15.sp else 20.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    character.archetype,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = if (compact) 11.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    character.flavor,
                    color = Color(0xFFCBD5E1),
                    fontSize = if (compact) 10.sp else 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!compact) {
                Column(horizontalAlignment = Alignment.End) {
                    BonusLine("Cap", "+${character.capacityBonus}")
                    BonusLine("Deal", "+${(character.negotiationBonus * 100).toInt()}%")
                    BonusLine("Rare", "+${(character.rareMarketBonus * 100).toInt()}%")
                }
            }
        }
    }
}

@Composable
private fun BonusLine(label: String, value: String) {
    Text("$label $value", color = Color(0xFFE5E7EB), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun GameScreen(
    game: GameState,
    onTrade: (ItemId, TradeMode) -> Unit,
    onAction: (GameAction, Cue) -> Unit,
    onTravel: () -> Unit,
    onSettings: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 430.dp || maxWidth < 760.dp
        if (compact) {
            Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.weight(0.42f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlayerStatsPanel(game, Modifier.width(250.dp).fillMaxHeight(), compact = true)
                    CityPanel(game.currentCity, Modifier.weight(1f).fillMaxHeight(), onSettings)
                }
                MarketAndActions(game, onTrade, onAction, onTravel, Modifier.weight(0.58f).fillMaxWidth())
            }
        } else {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.weight(0.44f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlayerStatsPanel(game, Modifier.width(300.dp).fillMaxHeight(), compact = false)
                    CityPanel(game.currentCity, Modifier.weight(1f).fillMaxHeight(), onSettings)
                    EventPanel(game, Modifier.width(300.dp).fillMaxHeight())
                }
                MarketAndActions(game, onTrade, onAction, onTravel, Modifier.weight(0.56f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PlayerStatsPanel(game: GameState, modifier: Modifier = Modifier, compact: Boolean = false) {
    val character = GameCatalog.character(game.player.characterId)
    Panel(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CharacterPortrait(character, Modifier.size(if (compact) 52.dp else 70.dp))
            Column(Modifier.weight(1f)) {
                Text(character.id.label, color = Color.White, fontSize = if (compact) 16.sp else 20.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text(game.currentCityId.label, color = MaterialTheme.colorScheme.primary, fontSize = if (compact) 13.sp else 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("Day ${game.day}", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                if (compact) {
                    Text("Cash $${game.player.cash}  Debt $${game.player.debt}", color = Color.White, fontSize = 9.sp, maxLines = 1)
                    Text("Pack ${game.usedCapacity}/${game.player.capacity}  Heat ${game.player.heat}%  Gang ${game.player.gangPressure}%", color = Color(0xFFCBD5E1), fontSize = 9.sp, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(if (compact) 4.dp else 10.dp))
        if (compact) {
            CompactStatLine("Health", "${game.player.health}", "Risk", "${game.player.risk}%", "Rep", game.player.reputation.toString())
        } else {
            StatRow(Icons.Filled.Favorite, "Health", "${game.player.health}/100")
            StatRow(Icons.Filled.AttachMoney, "Cash", "$${game.player.cash}")
            StatRow(Icons.Filled.Remove, "Debt", "$${game.player.debt}")
            StatRow(Icons.Filled.ShoppingCart, "Inventory", "${game.usedCapacity}/${game.player.capacity}")
            Meter("Heat", game.player.heat, MaterialTheme.colorScheme.secondary)
            Meter("Risk", game.player.risk, Color(0xFFFDE047))
            Meter("Gang", game.player.gangPressure, Color(0xFFFF6B6B))
            StatRow(Icons.Filled.Shield, "Rep", game.player.reputation.toString())
        }
    }
}

@Composable
private fun CityPanel(city: City, modifier: Modifier = Modifier, onSettings: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(8.dp))) {
        CityBackdrop(city, Modifier.fillMaxSize())
        IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color(0xAA05060A))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistChip(onClick = {}, label = { Text("Danger ${city.danger}") })
            AssistChip(onClick = {}, label = { Text("Law ${city.law}") })
            AssistChip(onClick = {}, label = { Text("Gang ${city.gang}") })
        }
    }
}

@Composable
private fun EventPanel(game: GameState, modifier: Modifier = Modifier) {
    Panel(modifier) {
        Text("Street Feed", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items = game.timeline, key = { event: GameEvent -> event.id }) { event: GameEvent ->
                val color = when (event.severity) {
                    EventSeverity.INFO -> Color(0xFF94A3B8)
                    EventSeverity.GOOD -> Color(0xFF34D399)
                    EventSeverity.WARNING -> Color(0xFFFDE047)
                    EventSeverity.DANGER -> Color(0xFFFF6B6B)
                    EventSeverity.LEGENDARY -> Color(0xFFFF3FB4)
                }
                Column(Modifier.border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(8.dp)) {
                    Text("D${event.day}  ${event.title}", color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(event.body, color = Color(0xFFE5E7EB), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun MarketAndActions(
    game: GameState,
    onTrade: (ItemId, TradeMode) -> Unit,
    onAction: (GameAction, Cue) -> Unit,
    onTravel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel(Modifier.width(210.dp).fillMaxHeight()) {
            Text("Actions", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                item { ActionButton("Travel", Icons.Filled.TravelExplore) { onTravel() } }
                item { ActionButton("Hide", Icons.Filled.Remove) { onAction(GameAction.Hide, Cue.Click) } }
                item { ActionButton("Rest", Icons.Filled.Favorite) { onAction(GameAction.Rest, Cue.Click) } }
                item { ActionButton("Upgrade", Icons.Filled.Add) { onAction(GameAction.UpgradeCapacity, Cue.Click) } }
                item { ActionButton("Bribe", Icons.Filled.LocalPolice) { onAction(GameAction.BribeOfficials, Cue.Siren) } }
                item { ActionButton("Contact", Icons.Filled.Shield) { onAction(GameAction.VisitContact, Cue.Click) } }
                item { ActionButton("Gamble", Icons.Filled.AttachMoney) { onAction(GameAction.Gamble, Cue.Danger) } }
            }
        }
        Panel(Modifier.weight(1f).fillMaxHeight()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Market Board", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(game.market.news, color = Color(0xFFCBD5E1), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(items = game.market.items, key = { listing: MarketListing -> listing.itemId.name }) { listing: MarketListing ->
                    MarketRow(
                        listing = listing,
                        owned = game.inventory[listing.itemId] ?: 0,
                        onBuy = { onTrade(listing.itemId, TradeMode.BUY) },
                        onSell = { onTrade(listing.itemId, TradeMode.SELL) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketRow(listing: MarketListing, owned: Int, onBuy: () -> Unit, onSell: () -> Unit) {
    Surface(
        color = Color(0xFF0B1220),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, trendColor(listing.trend).copy(alpha = 0.45f)),
    ) {
        Row(
            Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ItemGlyph(listing.itemId, Modifier.size(42.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(listing.itemId.label, color = Color.White, fontWeight = FontWeight.Black, maxLines = 1)
                    Text(trendLabel(listing.trend), color = trendColor(listing.trend), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Text("Qty ${listing.available}  Owned $owned  Margin ${listing.marginHint}%", color = Color(0xFFCBD5E1), fontSize = 12.sp)
            }
            Text("$${listing.price}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, modifier = Modifier.width(86.dp))
            OutlinedButton(onClick = onBuy, enabled = listing.available > 0, modifier = Modifier.width(78.dp)) { Text("Buy") }
            Button(onClick = onSell, enabled = owned > 0, modifier = Modifier.width(78.dp)) { Text("Sell") }
        }
    }
}

@Composable
private fun TravelScreen(game: GameState, onBack: () -> Unit, onTravel: (CityId) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Choose Route", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(game.cities, key = { it.id.name }) { city ->
                val current = city.id == game.currentCityId
                Surface(
                    color = if (current) Color(0xFF172033) else Color(0xFF0B1220),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, city.palette.first().toColor().copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth().height(118.dp),
                ) {
                    Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CityBackdrop(city, Modifier.width(190.dp).fillMaxHeight().clip(RoundedCornerShape(6.dp)))
                        Column(Modifier.weight(1f)) {
                            Text(city.id.label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text(city.weather, color = Color(0xFFCBD5E1), maxLines = 1)
                            Text("Danger ${city.danger}  Law ${city.law}  Gang ${city.gang}", color = city.palette.first().toColor(), fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = { onTravel(city.id) }, enabled = !current) {
                            Text(if (current) "Here" else "Travel")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    uiState: GameUiState,
    onBack: () -> Unit,
    onToggleAudio: () -> Unit,
    onClear: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Settings", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
        Panel(Modifier.fillMaxWidth().height(104.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Audio", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Toggle local generated effects.", color = Color(0xFFCBD5E1))
                }
                Button(onClick = onToggleAudio) {
                    Icon(if (uiState.game?.audioEnabled == false) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.game?.audioEnabled == false) "Off" else "On")
                }
            }
        }
        Panel(Modifier.fillMaxWidth().height(146.dp)) {
            Text("Run", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Autosave uses Room and updates after every action.", color = Color(0xFFCBD5E1))
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onClear) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Clear Save")
            }
        }
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xE60A1020)),
        border = BorderStroke(1.dp, Color(0xFF263348)),
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp), content = content)
    }
}

@Composable
private fun ActionButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(42.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(label, modifier = Modifier.weight(1f), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun StatRow(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(label, color = Color(0xFFCBD5E1), modifier = Modifier.weight(1f), fontSize = 12.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun CompactStatLine(
    leftLabel: String,
    leftValue: String,
    middleLabel: String,
    middleValue: String,
    rightLabel: String,
    rightValue: String,
) {
    Row(Modifier.fillMaxWidth().height(24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CompactStat(leftLabel, leftValue, Modifier.weight(1f))
        CompactStat(middleLabel, middleValue, Modifier.weight(1f))
        CompactStat(rightLabel, rightValue, Modifier.weight(1f))
    }
}

@Composable
private fun CompactStat(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFFCBD5E1), fontSize = 10.sp, maxLines = 1)
        Text(value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun Meter(label: String, value: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color(0xFFCBD5E1), fontSize = 12.sp)
            Text("$value%", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = value / 100f,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color(0xFF263348),
        )
    }
}

private fun trendLabel(trend: Trend): String = when (trend) {
    Trend.CRASH -> "▼▼"
    Trend.DOWN -> "▼"
    Trend.STABLE -> "■"
    Trend.UP -> "▲"
    Trend.JACKPOT -> "◆"
}

private fun trendColor(trend: Trend): Color = when (trend) {
    Trend.CRASH -> Color(0xFF38BDF8)
    Trend.DOWN -> Color(0xFF60A5FA)
    Trend.STABLE -> Color(0xFFCBD5E1)
    Trend.UP -> Color(0xFFFDE047)
    Trend.JACKPOT -> Color(0xFFFF3FB4)
}
