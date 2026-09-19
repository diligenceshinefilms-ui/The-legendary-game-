package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun TimeTrialScreen(
    tracks: List<Track>,
    playerBike: EffectiveBikeStats?,
    onBack: () -> Unit,
    onStartTimeTrial: (Track, EffectiveBikeStats) -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val currentTrack = tracks.getOrNull(selectedIndex) ?: tracks.firstOrNull()

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
                        text = "TIME TRIAL ATTACK",
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
                .testTag("time_trial_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CarbonCard,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NitroPurple)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = NitroPurple, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SOLO LAP ATTACK",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = NitroPurple)
                            )
                            Text(
                                text = "Push your machine to the limit without opponent traffic. Set clean split records.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextGray)
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "CHOOSE CIRCUIT",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                )
            }

            itemsIndexed(tracks) { idx, track ->
                val isSelected = idx == selectedIndex
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) NitroPurple else CarbonCardBorder,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedIndex = idx },
                    color = if (isSelected) CarbonSurface else CarbonCard
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = track.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = TextWhite
                                )
                            )
                            Text(
                                text = "${track.lengthMeters}m • ${track.difficulty.name}",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextGray)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = if (isSelected) NitroPurple else TextMuted
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                NeonButton(
                    text = "START TIME ATTACK",
                    icon = Icons.Default.Timer,
                    color = NitroPurple,
                    enabled = currentTrack != null && playerBike != null,
                    onClick = {
                        if (currentTrack != null && playerBike != null) {
                            onStartTimeTrial(currentTrack, playerBike)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
