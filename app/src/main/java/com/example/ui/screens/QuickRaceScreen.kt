package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.*
import com.example.ui.components.NeonButton
import com.example.ui.theme.*

@Composable
fun QuickRaceScreen(
    player: Player?,
    tracks: List<Track>,
    bikes: List<EffectiveBikeStats>,
    onBack: () -> Unit,
    onStartRace: (Track, EffectiveBikeStats, Int) -> Unit
) {
    var selectedTrackIndex by remember { mutableIntStateOf(0) }
    var selectedLaps by remember { mutableIntStateOf(3) }

    val activeTrack = tracks.getOrNull(selectedTrackIndex) ?: tracks.firstOrNull()
    val activeBike = bikes.firstOrNull { it.bike.id == player?.selectedBikeId } ?: bikes.firstOrNull()

    val isLocked = (player?.level ?: 1) < (activeTrack?.unlockLevel ?: 1)

    Scaffold(
        topBar = {
            Surface(
                color = CarbonDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CarbonCard)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "RACE SETUP",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        },
        containerColor = CarbonDark
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Track Selector Carousel
            item {
                Text(
                    text = "SELECT CIRCUIT",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(tracks.indices.toList()) { idx ->
                        val track = tracks[idx]
                        val isSelected = idx == selectedTrackIndex
                        val locked = (player?.level ?: 1) < track.unlockLevel

                        Surface(
                            modifier = Modifier
                                .width(220.dp)
                                .height(130.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) NeonCyan else CarbonCardBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedTrackIndex = idx },
                            color = if (isSelected) CarbonSurface else CarbonCard
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = track.difficulty.name,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = when (track.difficulty) {
                                                    Difficulty.EASY -> EmeraldGreen
                                                    Difficulty.NORMAL -> NeonOrange
                                                    Difficulty.HARD -> RacingRed
                                                }
                                            )
                                        )
                                        if (locked) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "LV ${track.unlockLevel}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = TextMuted,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = track.name,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Black,
                                                fontStyle = FontStyle.Italic,
                                                color = TextWhite
                                            )
                                        )
                                        Text(
                                            text = "${track.lengthMeters}m • ${track.environment}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextGray
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Track Details Banner
            if (activeTrack != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CarbonCard,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = activeTrack.name.uppercase(),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    color = NeonCyan
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeTrack.description,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextGray
                                )
                            )
                        }
                    }
                }
            }

            // 3. Lap Selector
            item {
                Text(
                    text = "RACE DISTANCE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeonOrange,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(1, 2, 3, 5).forEach { laps ->
                        val isSel = selectedLaps == laps
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(CutCornerShape(8.dp))
                                .background(if (isSel) NeonOrange else CarbonCard)
                                .border(1.dp, if (isSel) Color.White else CarbonCardBorder, CutCornerShape(8.dp))
                                .clickable { selectedLaps = laps },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$laps ${if (laps == 1) "LAP" else "LAPS"}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (isSel) CarbonDark else TextWhite
                                )
                            )
                        }
                    }
                }
            }

            // 4. Assigned Motorcycle Preview
            if (activeBike != null) {
                item {
                    Text(
                        text = "SELECTED MOTORCYCLE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CarbonCard,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CutCornerShape(8.dp))
                                        .background(Color(activeBike.bike.primaryColorHex)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TwoWheeler,
                                        contentDescription = null,
                                        tint = CarbonDark,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = activeBike.bike.name,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite
                                        )
                                    )
                                    Text(
                                        text = "${activeBike.topSpeedKmh.toInt()} KM/H • ${activeBike.acceleration.toInt()} ACCEL",
                                        style = MaterialTheme.typography.bodySmall.copy(color = NeonCyan)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Start Race Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                NeonButton(
                    text = if (isLocked) "LOCKED (REACH LEVEL ${activeTrack?.unlockLevel})" else "START RACE",
                    icon = Icons.Default.PlayArrow,
                    color = if (isLocked) TextMuted else NeonCyan,
                    enabled = !isLocked && activeTrack != null && activeBike != null,
                    onClick = {
                        if (activeTrack != null && activeBike != null) {
                            onStartRace(activeTrack, activeBike, selectedLaps)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "btn_start_race"
                )
            }
        }
    }
}
