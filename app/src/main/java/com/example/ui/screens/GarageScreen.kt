package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.GameBalanceConfig
import com.example.core.model.BikeUpgradeType
import com.example.core.model.EffectiveBikeStats
import com.example.core.model.Player
import com.example.ui.components.CurrencyHeader
import com.example.ui.components.NeonButton
import com.example.ui.components.StatBar
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GarageScreen(
    player: Player?,
    bikes: List<EffectiveBikeStats>,
    onBack: () -> Unit,
    onSelectBike: (String) -> Unit,
    onUnlockBike: (String) -> Unit,
    onUpgradeBike: (String, BikeUpgradeType) -> Unit
) {
    var selectedBikeIndex by remember {
        val idx = bikes.indexOfFirst { it.bike.id == player?.selectedBikeId }
        mutableIntStateOf(if (idx >= 0) idx else 0)
    }

    val currentBikeStats = bikes.getOrNull(selectedBikeIndex) ?: bikes.firstOrNull()
    var activeTab by remember { mutableStateOf("UPGRADES") } // "UPGRADES", "LIVERY", "DYNO"

    // Customization State
    var customPrimaryColor by remember(currentBikeStats) {
        mutableStateOf(currentBikeStats?.bike?.primaryColorHex ?: 0xFF00F0FF)
    }
    var customUnderglowColor by remember { mutableStateOf(0xFF00F0FF) }
    var selectedRacingNumber by remember { mutableStateOf("46") }
    var selectedDecalStyle by remember { mutableStateOf("Cyber Flames") }

    // Dyno Rev Simulator State
    var isRevving by remember { mutableStateOf(false) }
    var revRpm by remember { mutableFloatStateOf(1000f) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(CarbonDark)) {
        val dummyTrack = remember { com.example.game.tracks.TrackCatalog.getTrackLayout("trk_tokyo_01") }
        val dummyRacer = remember(currentBikeStats, customPrimaryColor) {
            com.example.core.model.RacerState(
                id = "dummy",
                name = "player",
                isPlayer = true,
                posX = 0f, posY = 0f,
                speedKmh = if (isRevving) 180f else 45f,
                primaryColorHex = customPrimaryColor,
                bikeName = currentBikeStats?.bike?.name ?: "Legend X1",
                totalRaceDistance = 1500f
            )
        }

        com.example.game.renderer.GameCanvasRenderer(
            track = dummyTrack,
            racers = listOf(dummyRacer),
            obstacles = emptyList(),
            modifier = Modifier.fillMaxSize()
        )
        
        // Semi-transparent overlay for UI readability
        Box(modifier = Modifier.fillMaxSize().background(CarbonDark.copy(alpha = 0.6f)))

        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(CarbonDark.copy(alpha = 0.9f))) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CutCornerShape(8.dp))
                                .background(CarbonCard.copy(alpha = 0.8f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = TextWhite
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "RACING BIKE STUDIO",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontStyle = FontStyle.Italic
                            )
                        )
                    }
                    CurrencyHeader(player = player, modifier = Modifier.background(Color.Transparent))
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // 1. Bike Selector Tabs
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(bikes.indices.toList()) { idx ->
                            val item = bikes[idx]
                            val isSelected = idx == selectedBikeIndex
                            val isUnlocked = item.upgradeLevels.unlocked
                            val isCurrentEquipped = item.bike.id == player?.selectedBikeId

                            Surface(
                                modifier = Modifier
                                    .width(150.dp)
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) NeonCyan else CarbonCardBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedBikeIndex = idx },
                                color = if (isSelected) CarbonSurface else CarbonCard
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CutCornerShape(2.dp))
                                                .background(Color(item.bike.primaryColorHex))
                                        )
                                        if (isCurrentEquipped) {
                                            Text(
                                                text = "EQUIPPED",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = NeonCyan,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 9.sp
                                                )
                                            )
                                        } else if (!isUnlocked) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = item.bike.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                if (currentBikeStats != null) {
                    // 2. Bike Hero & Action Header
                    item {
                        val isUnlocked = currentBikeStats.upgradeLevels.unlocked
                        val isEquipped = currentBikeStats.bike.id == player?.selectedBikeId
                        val canAffordUnlock = (player?.coins ?: 0L) >= currentBikeStats.bike.price
                        val levelMet = (player?.level ?: 1) >= currentBikeStats.bike.unlockLevel

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = CarbonCard,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentBikeStats.bike.name.uppercase(),
                                            style = MaterialTheme.typography.headlineLarge.copy(
                                                fontWeight = FontWeight.Black,
                                                fontStyle = FontStyle.Italic,
                                                color = TextWhite
                                            )
                                        )
                                        Text(
                                            text = currentBikeStats.bike.description,
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextGray)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (isUnlocked) {
                                        if (isEquipped) {
                                            Surface(
                                                color = NeonCyan.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(8.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
                                            ) {
                                                Text(
                                                    text = "EQUIPPED",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Black,
                                                        color = NeonCyan
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        } else {
                                            NeonButton(
                                                text = "EQUIP",
                                                onClick = { onSelectBike(currentBikeStats.bike.id) },
                                                color = NeonCyan,
                                                isPrimary = true,
                                                modifier = Modifier.height(40.dp)
                                            )
                                        }
                                    } else {
                                        NeonButton(
                                            text = if (!levelMet) "LV ${currentBikeStats.bike.unlockLevel}" else "BUY %,d".format(currentBikeStats.bike.price),
                                            icon = Icons.Default.MonetizationOn,
                                            color = if (!levelMet || !canAffordUnlock) TextMuted else ElectricYellow,
                                            enabled = levelMet && canAffordUnlock,
                                            onClick = { onUnlockBike(currentBikeStats.bike.id) },
                                            modifier = Modifier.height(44.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Stats Overview
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = CarbonCard,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "PERFORMANCE RATINGS",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan,
                                        letterSpacing = 1.2.sp
                                    )
                                )

                                StatBar(
                                    label = "Top Speed (${currentBikeStats.topSpeedKmh.toInt()} km/h)",
                                    value = ((currentBikeStats.topSpeedKmh / 280f) * 100).toInt(),
                                    color = NeonCyan,
                                    icon = Icons.Default.Speed
                                )
                                StatBar(
                                    label = "Acceleration",
                                    value = currentBikeStats.acceleration.toInt(),
                                    color = NeonOrange,
                                    icon = Icons.Default.FlashOn
                                )
                                StatBar(
                                    label = "Handling",
                                    value = currentBikeStats.handling.toInt(),
                                    color = EmeraldGreen,
                                    icon = Icons.Default.Tune
                                )
                                StatBar(
                                    label = "Braking",
                                    value = currentBikeStats.braking.toInt(),
                                    color = RacingRed,
                                    icon = Icons.Default.Shield
                                )
                                StatBar(
                                    label = "Nitro Boost (${currentBikeStats.nitroDurationSec}s)",
                                    value = currentBikeStats.bike.baseNitro,
                                    color = NitroPurple,
                                    icon = Icons.Default.LocalFireDepartment
                                )
                            }
                        }
                    }

                    // 4. Studio Feature Tabs (Upgrades, Livery & Paint, Dyno & Telemetry)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val tabs = listOf("UPGRADES", "LIVERY", "DYNO")
                            tabs.forEach { tabName ->
                                val isActive = activeTab == tabName
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clickable { activeTab = tabName },
                                    color = if (isActive) NeonCyan else CarbonCard,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) NeonCyan else CarbonCardBorder)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = tabName,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                color = if (isActive) CarbonDark else TextWhite
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Tab Content
                    when (activeTab) {
                        "UPGRADES" -> {
                            if (currentBikeStats.upgradeLevels.unlocked) {
                                item {
                                    Text(
                                        text = "PERFORMANCE UPGRADE PARTS",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = NeonOrange,
                                            letterSpacing = 1.2.sp
                                        )
                                    )
                                }

                                val upgradeTypes = listOf(
                                    Triple(BikeUpgradeType.ENGINE, "Engine Tuning", currentBikeStats.upgradeLevels.engineLevel),
                                    Triple(BikeUpgradeType.TIRES, "Racing Tires", currentBikeStats.upgradeLevels.tireLevel),
                                    Triple(BikeUpgradeType.BRAKES, "Carbon Brakes", currentBikeStats.upgradeLevels.brakeLevel),
                                    Triple(BikeUpgradeType.HANDLING, "Suspension & Aero", currentBikeStats.upgradeLevels.handlingLevel),
                                    Triple(BikeUpgradeType.NITRO, "Twin Nitro Ingot", currentBikeStats.upgradeLevels.nitroLevel)
                                )

                                items(upgradeTypes.size) { uIdx ->
                                    val (type, name, currentLevel) = upgradeTypes[uIdx]
                                    val isMax = currentLevel >= GameBalanceConfig.MAX_UPGRADE_LEVEL
                                    val cost = if (!isMax) GameBalanceConfig.getUpgradeCost(type, currentLevel) else 0L
                                    val canAfford = (player?.coins ?: 0L) >= cost

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = CarbonSurface,
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = name,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextWhite
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    for (lvl in 1..GameBalanceConfig.MAX_UPGRADE_LEVEL) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(width = 16.dp, height = 6.dp)
                                                                .clip(RoundedCornerShape(2.dp))
                                                                .background(if (lvl <= currentLevel) NeonCyan else CarbonDark)
                                                        )
                                                    }
                                                }
                                            }

                                            if (isMax) {
                                                Surface(
                                                    color = EmeraldGreen.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "MAX LEVEL",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Black,
                                                            color = EmeraldGreen
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            } else {
                                                NeonButton(
                                                    text = "UPGRADE %,d".format(cost),
                                                    icon = Icons.Default.MonetizationOn,
                                                    color = if (canAfford) ElectricYellow else TextMuted,
                                                    enabled = canAfford,
                                                    isPrimary = false,
                                                    onClick = { onUpgradeBike(currentBikeStats.bike.id, type) },
                                                    modifier = Modifier.height(40.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = CarbonSurface,
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
                                    ) {
                                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "Unlock this racing bike to access performance upgrades and tuning parts.",
                                                style = MaterialTheme.typography.bodyMedium.copy(color = TextGray),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        "LIVERY" -> {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = CarbonCard,
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Text(
                                            text = "PRIMARY LIVERY PAINT",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = NeonCyan,
                                                letterSpacing = 1.2.sp
                                            )
                                        )

                                        val colorOptions = listOf(
                                            "Cyber Cyan" to 0xFF00F0FFL,
                                            "Nebula Pink" to 0xFFFF1B7AL,
                                            "Racing Red" to 0xFFFF1744L,
                                            "Electric Gold" to 0xFFFFD600L,
                                            "Neon Orange" to 0xFFFF6B00L,
                                            "Nitro Purple" to 0xFFB5179EL,
                                            "Carbon Black" to 0xFF121212L,
                                            "Ghost White" to 0xFFF8FAFCAL
                                        )

                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            items(colorOptions) { (name, hex) ->
                                                val isSelected = customPrimaryColor == hex
                                                Surface(
                                                    modifier = Modifier
                                                        .size(56.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .border(
                                                            width = if (isSelected) 2.dp else 1.dp,
                                                            color = if (isSelected) NeonCyan else CarbonCardBorder,
                                                            shape = RoundedCornerShape(12.dp)
                                                        )
                                                        .clickable { customPrimaryColor = hex },
                                                    color = Color(hex)
                                                ) {}
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "UNDERGLOW NEON LIGHTING",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = NeonOrange,
                                                letterSpacing = 1.2.sp
                                            )
                                        )

                                        val underglowOptions = listOf(
                                            "Cyan Glow" to 0xFF00F0FFL,
                                            "Plasma Pink" to 0xFFFF1B7AL,
                                            "Emerald Beam" to 0xFF10B981L,
                                            "Amber Flame" to 0xFFF59E0BL
                                        )

                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            items(underglowOptions) { (name, hex) ->
                                                val isSelected = customUnderglowColor == hex
                                                Surface(
                                                    modifier = Modifier
                                                        .height(36.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .border(
                                                            width = if (isSelected) 2.dp else 1.dp,
                                                            color = if (isSelected) NeonCyan else CarbonCardBorder,
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable { customUnderglowColor = hex }
                                                        .padding(horizontal = 12.dp),
                                                    color = CarbonSurface
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = name,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = Color(hex),
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "RACING BADGE NUMBER",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldGreen,
                                                letterSpacing = 1.2.sp
                                            )
                                        )

                                        val numbers = listOf("07", "46", "69", "88", "99")
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(numbers) { num ->
                                                val isSelected = selectedRacingNumber == num
                                                Surface(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { selectedRacingNumber = num },
                                                    color = if (isSelected) EmeraldGreen else CarbonSurface
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = num,
                                                            style = MaterialTheme.typography.titleMedium.copy(
                                                                fontWeight = FontWeight.Black,
                                                                color = if (isSelected) CarbonDark else TextWhite
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "DYNO" -> {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = CarbonCard,
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "DYNO ENGINE TELEMETRY TEST",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = NeonCyan,
                                                letterSpacing = 1.2.sp
                                            )
                                        )

                                        // Tachometer Circular / Bar Display
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(CarbonSurface),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "${revRpm.toInt()} RPM",
                                                    style = MaterialTheme.typography.headlineLarge.copy(
                                                        fontWeight = FontWeight.Black,
                                                        color = if (revRpm > 11000f) RacingRed else NeonCyan
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                LinearProgressIndicator(
                                                    progress = { revRpm / 14000f },
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.8f)
                                                        .height(8.dp)
                                                        .clip(RoundedCornerShape(4.dp)),
                                                    color = if (revRpm > 11000f) RacingRed else NeonCyan,
                                                    trackColor = CarbonDark
                                                )
                                            }
                                        }

                                        NeonButton(
                                            text = if (isRevving) "STOP ENGINE REV" else "REV ENGINE (TAP TO TEST)",
                                            icon = Icons.Default.LocalFireDepartment,
                                            color = RacingRed,
                                            isPrimary = true,
                                            onClick = {
                                                isRevving = !isRevving
                                                revRpm = if (isRevving) 13500f else 1000f
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(46.dp)
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Advanced Telemetry Specs
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            TelemetryRow("Power-to-Weight", "4.82 HP/kg")
                                            TelemetryRow("Aero Downforce", "340 N @ 250 km/h")
                                            TelemetryRow("Braking G-Force", "1.85 G Ceramic")
                                            TelemetryRow("Cornering Agility", "96.4 Index")
                                            TelemetryRow("Turbo Boost", "1.8 Bar Twin-Scroll")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CarbonSurface, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = TextGray)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        )
    }
}
