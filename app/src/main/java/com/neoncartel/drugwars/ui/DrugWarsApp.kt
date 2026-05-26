package com.neoncartel.drugwars.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neoncartel.drugwars.R
import com.neoncartel.drugwars.app.GameUiState
import com.neoncartel.drugwars.app.GameViewModel
import com.neoncartel.drugwars.audio.Cue
import com.neoncartel.drugwars.audio.GameAudio
import com.neoncartel.drugwars.domain.content.GameCatalog
import com.neoncartel.drugwars.domain.model.CharacterDefinition
import com.neoncartel.drugwars.domain.model.CharacterId
import com.neoncartel.drugwars.domain.model.CityId
import com.neoncartel.drugwars.domain.model.Difficulty
import com.neoncartel.drugwars.domain.model.GameAction
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
                                    onTrade = { item, quantity, mode ->
                                        audio.play(if (mode == TradeMode.BUY) Cue.Buy else Cue.Sell, game.audioEnabled)
                                        viewModel.dispatch(GameAction.Trade(item, quantity, mode))
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
            Text("Loading Drug Wars", color = Color.White, fontWeight = FontWeight.Bold)
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
        val portrait = maxWidth < 560.dp
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xE605060A),
                            Color(0xAA05060A),
                            Color(0xF205060A),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 14.dp else 24.dp, vertical = if (compact) 8.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp),
        ) {
            if (portrait) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DrugWarsTextLockup(compact = compact, modifier = Modifier.fillMaxWidth())
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (uiState.game != null) {
                            MenuAssetButton(
                                drawableId = R.drawable.button_continue,
                                contentDescription = "Continue",
                                compact = compact,
                                onClick = onContinue,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        MenuAssetButton(
                            drawableId = R.drawable.button_new_run,
                            contentDescription = "New Run",
                            compact = compact,
                            onClick = onStart,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DrugWarsTextLockup(compact = compact, modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (uiState.game != null) {
                            MenuAssetButton(
                                drawableId = R.drawable.button_continue,
                                contentDescription = "Continue",
                                compact = compact,
                                onClick = onContinue,
                                modifier = Modifier.width(if (compact) 150.dp else 188.dp),
                            )
                        }
                        MenuAssetButton(
                            drawableId = R.drawable.button_new_run,
                            contentDescription = "New Run",
                            compact = compact,
                            onClick = onStart,
                            modifier = Modifier.width(if (compact) 150.dp else 188.dp),
                        )
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
private fun DrugWarsTextLockup(compact: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp)) {
        Text(
            "DRUG WARS",
            color = Color.White,
            fontSize = if (compact) 20.sp else 30.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "Alaska is cold. So is the game.",
            color = Color(0xFFCBD5E1),
            fontSize = if (compact) 12.sp else 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MenuAssetButton(
    @DrawableRes drawableId: Int,
    contentDescription: String,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(if (compact) 52.dp else 62.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(drawableId),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun CharacterCard(character: CharacterDefinition, selected: Boolean, compact: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF263348)
    MenuBoxFrame(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 98.dp else 112.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp)),
        contentPadding = PaddingValues(horizontal = if (compact) 14.dp else 20.dp, vertical = if (compact) 10.dp else 14.dp),
        edgeWidth = if (compact) 18.dp else 22.dp,
        edgeHeight = if (compact) 16.dp else 20.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
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
    onTrade: (ItemId, Int, TradeMode) -> Unit,
    onAction: (GameAction, Cue) -> Unit,
    onTravel: () -> Unit,
    onSettings: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val metrics = remember(maxWidth, maxHeight, game.market.items.size) {
            GameLayoutMetrics.portrait(
                screenWidthDp = maxWidth.value.toInt(),
                screenHeightDp = maxHeight.value.toInt(),
                marketItemCount = game.market.items.size,
            )
        }
        Column(Modifier.fillMaxSize()) {
            CityHero(
                game = game,
                onSettings = onSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(metrics.cityHeightDp.dp),
            )
            Spacer(Modifier.height(metrics.sectionGapDp.dp))
            ActionStrip(
                onAction = onAction,
                onTravel = onTravel,
                gap = metrics.actionGapDp.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(metrics.actionRowHeightDp.dp)
                    .padding(horizontal = metrics.horizontalPaddingDp.dp),
            )
            Spacer(Modifier.height(metrics.sectionGapDp.dp))
            MarketBoard(
                game = game,
                onTrade = onTrade,
                metrics = metrics,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        start = metrics.horizontalPaddingDp.dp,
                        end = metrics.horizontalPaddingDp.dp,
                        bottom = metrics.bottomPaddingDp.dp,
                    ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CityHero(game: GameState, onSettings: () -> Unit, modifier: Modifier = Modifier) {
    val city = game.currentCity
    val palette = city.palette.map { it.toColor() }
    val compact = game.market.items.size >= 12
    val usesAnchorageBanner = city.id == CityId.NEON_HARBOR
    Box(modifier) {
        CityBackdrop(city, Modifier.fillMaxSize(), showLabels = false)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x9905060A),
                            Color.Transparent,
                            Color(0xCC05060A),
                        ),
                    ),
                ),
        )
        IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
        }
        if (!usesAnchorageBanner) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 10.dp, end = 48.dp),
            ) {
                GraffitiText(
                    text = city.id.label.uppercase(),
                    color = Color.White,
                    accent = palette[0],
                    fontSize = if (compact) 22.sp else 28.sp,
                )
                Text(
                    city.weather.uppercase(),
                    color = palette.getOrElse(3) { Color.White },
                    fontSize = if (compact) 9.sp else 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        shadow = Shadow(color = Color.Black, offset = Offset(1.5f, 1.5f), blurRadius = 3f),
                    ),
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 42.dp, end = 24.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy((-2).dp),
        ) {
            CitySprayStat("DANGER", city.danger, palette[1])
            CitySprayStat("LAW", city.law, palette[0])
            CitySprayStat("GANG", city.gang, Color(0xFFFF6B6B))
        }
        FlowRow(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = if (compact) 7.dp else 9.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            maxItemsInEachRow = 4,
        ) {
            PlayerSprayStat("DAY", game.day.toString(), palette[0], compact)
            PlayerSprayStat("CASH", "$${game.player.cash}", MaterialTheme.colorScheme.primary, compact)
            PlayerSprayStat("PACK", "${game.usedCapacity}/${game.player.capacity}", Color.White, compact)
            PlayerSprayStat("HEAT", "${game.player.heat}%", Color(0xFFFF6B6B), compact)
            PlayerSprayStat("HP", "${game.player.health}", Color(0xFF34D399), compact)
            PlayerSprayStat("DEBT", "$${game.player.debt}", Color(0xFFFDE047), compact)
            PlayerSprayStat("RISK", "${game.player.risk}%", Color(0xFFF472B6), compact)
            PlayerSprayStat("REP", game.player.reputation.toString(), palette[1], compact)
        }
    }
}

@Composable
private fun GraffitiText(text: String, color: Color, accent: Color, fontSize: androidx.compose.ui.unit.TextUnit) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = FontWeight.Black,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            shadow = Shadow(color = accent.copy(alpha = 0.95f), offset = Offset(2.5f, 2.5f), blurRadius = 0.5f),
        ),
    )
}

@Composable
private fun CitySprayStat(label: String, value: Int, color: Color) {
    Text(
        "$label $value",
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.End,
        maxLines = 1,
        modifier = Modifier.rotate(-5f),
        style = TextStyle(
            shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 2f),
        ),
    )
}

@Composable
private fun PlayerSprayStat(label: String, value: String, color: Color, compact: Boolean) {
    Text(
        "$label $value",
        color = color,
        fontSize = if (compact) 9.sp else 10.sp,
        fontWeight = FontWeight.Black,
        maxLines = 1,
        style = TextStyle(
            shadow = Shadow(color = Color.Black, offset = Offset(1.4f, 1.4f), blurRadius = 2f),
        ),
    )
}

@Composable
private fun ActionStrip(
    onAction: (GameAction, Cue) -> Unit,
    onTravel: () -> Unit,
    gap: Dp,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
        CompactActionButton("Route", Icons.Filled.TravelExplore, "Travel", Modifier.weight(1f)) { onTravel() }
        CompactActionButton("Hide", Icons.Filled.Remove, "Hide", Modifier.weight(1f)) { onAction(GameAction.Hide, Cue.Click) }
        CompactActionButton("Rest", Icons.Filled.Favorite, "Rest", Modifier.weight(1f)) { onAction(GameAction.Rest, Cue.Click) }
        CompactActionButton("Pack", Icons.Filled.Add, "Upgrade capacity", Modifier.weight(1f)) { onAction(GameAction.UpgradeCapacity, Cue.Click) }
        CompactActionButton("Bribe", Icons.Filled.LocalPolice, "Bribe officials", Modifier.weight(1f)) { onAction(GameAction.BribeOfficials, Cue.Siren) }
        CompactActionButton("Meet", Icons.Filled.Shield, "Visit contact", Modifier.weight(1f)) { onAction(GameAction.VisitContact, Cue.Click) }
        CompactActionButton("Bet", Icons.Filled.AttachMoney, "Gamble", Modifier.weight(1f)) { onAction(GameAction.Gamble, Cue.Danger) }
    }
}

@Composable
private fun CompactActionButton(
    label: String,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    MenuBoxFrame(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(7.dp))
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
        edgeWidth = 13.dp,
        edgeHeight = 12.dp,
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Text(
                label,
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MarketBoard(
    game: GameState,
    onTrade: (ItemId, Int, TradeMode) -> Unit,
    metrics: PortraitGameLayoutMetrics,
    modifier: Modifier = Modifier,
) {
    val rows = game.market.items.chunked(metrics.marketColumns)
    var pendingTrade by remember { mutableStateOf<PendingTrade?>(null) }
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().height(metrics.marketHeaderHeightDp.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Market", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(
                game.market.news,
                color = Color(0xFFCBD5E1),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(metrics.marketGridGapDp.dp),
        ) {
            rows.forEach { rowListings ->
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(metrics.marketGridGapDp.dp),
                ) {
                    rowListings.forEach { listing ->
                        MarketTile(
                            listing = listing,
                            owned = game.inventory[listing.itemId] ?: 0,
                            onBuy = { pendingTrade = PendingTrade(listing, game.inventory[listing.itemId] ?: 0, TradeMode.BUY) },
                            onSell = { pendingTrade = PendingTrade(listing, game.inventory[listing.itemId] ?: 0, TradeMode.SELL) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    if (rowListings.size < metrics.marketColumns) {
                        Spacer(Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
    pendingTrade?.let { trade ->
        TradeQuantityDialog(
            trade = trade,
            game = game,
            onDismiss = { pendingTrade = null },
            onConfirm = { itemId, quantity, mode ->
                onTrade(itemId, quantity, mode)
                pendingTrade = null
            },
        )
    }
}

private data class PendingTrade(
    val listing: MarketListing,
    val owned: Int,
    val mode: TradeMode,
)

@Composable
private fun TradeQuantityDialog(
    trade: PendingTrade,
    game: GameState,
    onDismiss: () -> Unit,
    onConfirm: (ItemId, Int, TradeMode) -> Unit,
) {
    val item = GameCatalog.item(trade.listing.itemId)
    val capacityLeft = (game.player.capacity - game.usedCapacity).coerceAtLeast(0)
    val maxQuantity = when (trade.mode) {
        TradeMode.BUY -> TradeQuantityRules.maxBuyQuantity(
            available = trade.listing.available,
            cash = game.player.cash,
            price = trade.listing.price,
            capacityLeft = capacityLeft,
            itemWeight = item.weight,
        )
        TradeMode.SELL -> TradeQuantityRules.maxSellQuantity(trade.owned)
    }
    var quantityText by remember(trade.listing.itemId, trade.mode) { mutableStateOf("1") }
    val quantity = TradeQuantityRules.sanitizeQuantity(quantityText, maxQuantity)
    val verb = if (trade.mode == TradeMode.BUY) "Buy" else "Sell"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("$verb ${trade.listing.itemId.label}", fontWeight = FontWeight.Black)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Price $${trade.listing.price}  Available ${trade.listing.available}  Owned ${trade.owned}",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                )
                Text(
                    if (trade.mode == TradeMode.BUY) {
                        "Cash $${game.player.cash}  Pack room ${capacityLeft / item.weight} units"
                    } else {
                        "You can sell up to $maxQuantity units."
                    },
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { quantityText = (quantity - 1).coerceAtLeast(1).toString() },
                        enabled = quantity > 1,
                    ) {
                        Text("-")
                    }
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { value ->
                            quantityText = value.filter { it.isDigit() }.take(3)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    TextButton(
                        onClick = { quantityText = (quantity + 1).coerceAtMost(maxQuantity).toString() },
                        enabled = quantity < maxQuantity,
                    ) {
                        Text("+")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { quantityText = "1" }, enabled = maxQuantity > 0) {
                        Text("1")
                    }
                    TextButton(onClick = { quantityText = maxQuantity.toString() }, enabled = maxQuantity > 0) {
                        Text("Max $maxQuantity")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(trade.listing.itemId, quantity, trade.mode) },
                enabled = maxQuantity > 0,
            ) {
                Text("$verb $quantity")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun MarketTile(listing: MarketListing, owned: Int, onBuy: () -> Unit, onSell: () -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val dense = maxHeight < 58.dp || maxWidth < 178.dp
        val trend = trendColor(listing.trend)
        MenuBoxFrame(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = if (dense) 8.dp else 10.dp, vertical = if (dense) 5.dp else 7.dp),
            edgeWidth = if (dense) 14.dp else 17.dp,
            edgeHeight = if (dense) 13.dp else 16.dp,
        ) {
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (dense) 4.dp else 6.dp),
            ) {
                ItemGlyph(
                    itemId = listing.itemId,
                    modifier = Modifier.size(if (dense) 20.dp else 24.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(
                        listing.itemId.label,
                        color = Color.White,
                        fontSize = if (dense) 10.sp else 12.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Q${listing.available} O$owned ${listing.marginHint}%",
                        color = Color(0xFFCBD5E1),
                        fontSize = if (dense) 8.sp else 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                    Text(
                        "$${listing.price}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = if (dense) 10.sp else 12.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                    )
                    Text(
                        trendLabel(listing.trend),
                        color = trend,
                        fontSize = if (dense) 9.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(TradeControlMetrics.ButtonGapDp.dp),
                    modifier = Modifier.width(TradeControlMetrics.RailWidthDp.dp),
                ) {
                    TradeControl(TradeControlMetrics.BuyLabel, listing.available > 0, onBuy)
                    TradeControl(TradeControlMetrics.SellLabel, owned > 0, onSell)
                }
            }
        }
    }
}

@Composable
private fun TradeControl(label: String, enabled: Boolean, onClick: () -> Unit) {
    val tint = if (enabled) MaterialTheme.colorScheme.primary else Color(0xFF475569)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(TradeControlMetrics.ButtonHeightDp.dp)
            .clip(RoundedCornerShape(TradeControlMetrics.CornerRadiusDp.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = if (enabled) Color(0xFF111827) else Color(0xFF0A1020),
        shape = RoundedCornerShape(TradeControlMetrics.CornerRadiusDp.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = if (enabled) 0.8f else 0.35f)),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = tint,
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
private fun TravelScreen(game: GameState, onBack: () -> Unit, onTravel: (CityId) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 720.dp
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                Text("Choose Route", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
            }
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(game.cities, key = { it.id.name }) { city ->
                    val current = city.id == game.currentCityId
                    MenuBoxFrame(
                        modifier = Modifier.fillMaxWidth().height(118.dp),
                        contentPadding = PaddingValues(12.dp),
                        edgeWidth = 22.dp,
                        edgeHeight = 20.dp,
                    ) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("Settings", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
        Panel(Modifier.fillMaxWidth().height(104.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Audio", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Toggle local generated effects.", color = Color(0xFFCBD5E1))
                }
                Button(onClick = onToggleAudio) {
                    Icon(
                        if (uiState.game?.audioEnabled == false) {
                            Icons.AutoMirrored.Filled.VolumeOff
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = null,
                    )
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
    MenuBoxFrame(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
        edgeWidth = 22.dp,
        edgeHeight = 20.dp,
    ) {
        Column(Modifier.fillMaxSize(), content = content)
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
