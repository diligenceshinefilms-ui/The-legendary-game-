package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun ChampionshipScreen(
    championship: Championship?,
    tracks: List<Track>,
    playerBike: EffectiveBikeStats?,
    onBack: () -> Unit,
    onStartStage: (Track, EffectiveBikeStats, Int) -> Unit
) {
    val currentStageIdx = championship?.currentRaceIndex ?: 0
    val isDone = championship?.isCompleted ?: false
    val totalPoints = championship?.totalPoints ?: 0

    val currentTrack = tracks.getOrNull(currentStageIdx % tracks.size) ?: tracks.firstOrNull()

    Scaffold(
        topBar = {
            Surface(color = CarbonDark, modifier = Modifier.fillMaxWidth()) {
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(CarbonCard)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "CHAMPIONSHIP CUP",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic
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
                .padding(horizontal = 16.dp)
                .testTag("championship_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header Banner
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CarbonCard,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, ElectricYellow)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = championship?.name ?: "Apex Grand Prix Season 1",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontStyle = FontStyle.Italic,
                                        color = ElectricYellow
                                    )
                                )
                                Text(
                                    text = "5-Stage Worldwide Touring Cup",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextGray)
                                )
                            }
                            // Total Points
                            Surface(
                                color = CarbonDark,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricYellow)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "$totalPoints PTS",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = ElectricYellow
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Stage List
            val races = championship?.races ?: emptyList()
            itemsIndexed(races) { idx, r ->
                val track = tracks.firstOrNull { it.id == r.trackId }
                val isCurrent = idx == currentStageIdx && !isDone
                val isPassed = idx < currentStageIdx || isDone

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isCurrent) CarbonSurface else CarbonCard,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isCurrent) 1.5.dp else 1.dp,
                        color = if (isCurrent) NeonCyan else CarbonCardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CutCornerShape(8.dp))
                                    .background(if (isPassed) EmeraldGreen else if (isCurrent) NeonCyan else CarbonDark),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPassed) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = CarbonDark)
                                } else {
                                    Text(
                                        text = "${idx + 1}",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = if (isCurrent) CarbonDark else TextWhite
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = track?.name ?: "Stage ${idx + 1}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                )
                                Text(
                                    text = "${r.laps} Laps • ${track?.environment ?: "City"}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextGray)
                                )
                            }
                        }

                        if (isCurrent && currentTrack != null && playerBike != null) {
                            NeonButton(
                                text = "ENTER",
                                icon = Icons.Default.PlayArrow,
                                color = NeonCyan,
                                onClick = { onStartStage(currentTrack, playerBike, r.laps) },
                                modifier = Modifier.height(38.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
