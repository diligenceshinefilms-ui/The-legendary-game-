package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.LeaderboardEntry
import com.example.core.model.Player
import com.example.core.model.Track
import com.example.ui.components.NeonButton
import com.example.ui.theme.*

@Composable
fun LeaderboardScreen(
    tracks: List<Track>,
    leaderboardEntries: List<LeaderboardEntry>,
    registeredPlayer: Player? = null,
    onUpdateRegisteredName: (String) -> Unit = {},
    onBack: () -> Unit,
    onTrackSelect: (String) -> Unit,
    onRefresh: (String) -> Unit
) {
    var selectedTrackId by remember { mutableStateOf(tracks.firstOrNull()?.id ?: "track_city_rush") }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var newNameInput by remember { mutableStateOf(registeredPlayer?.displayName ?: "Ashok Gharge") }

    // Sort entries strictly by HIGHEST KM first, then by best time
    val sortedEntries = remember(leaderboardEntries) {
        leaderboardEntries.sortedWith(
            compareByDescending<LeaderboardEntry> { it.distanceKm }
                .thenBy { it.timeMs }
        )
    }

    Scaffold(
        topBar = {
            Surface(color = CarbonDark, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CarbonCard)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "GLOBAL RANKINGS",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic
                                ),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Higher KM Rider at #1 Rank",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ElectricYellow,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = { onRefresh(selectedTrackId) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CarbonCard)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = NeonCyan)
                    }
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
                .testTag("leaderboard_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. Registered User Profile Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CarbonSurface,
                    shape = CutCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan.copy(alpha = 0.2f))
                                        .border(1.dp, NeonCyan, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Racer Profile",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = registeredPlayer?.displayName ?: "Ashok Gharge",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                color = TextWhite
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(EmeraldGreen.copy(alpha = 0.2f))
                                                .border(0.8.dp, EmeraldGreen, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "REGISTERED PILOT",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = EmeraldGreen,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Official Racing Account",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextGray,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    newNameInput = registeredPlayer?.displayName ?: "Ashok Gharge"
                                    showEditNameDialog = true
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CarbonCard)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Name",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Distance Stats
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CarbonDark)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL ENDURANCE DISTANCE",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextGray, fontSize = 9.sp)
                                )
                                Text(
                                    text = "%.1f KM".format(registeredPlayer?.totalDistanceKm ?: 1248.5f),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = ElectricYellow
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "GLOBAL RANKING",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextGray, fontSize = 9.sp)
                                )
                                Text(
                                    text = "#1 RANK",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = EmeraldGreen
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Track Filter Carousel
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(tracks) { t ->
                        val isSel = t.id == selectedTrackId
                        Box(
                            modifier = Modifier
                                .clip(CutCornerShape(8.dp))
                                .background(if (isSel) NeonCyan else CarbonCard)
                                .border(1.dp, if (isSel) Color.White else CarbonCardBorder, CutCornerShape(8.dp))
                                .clickable {
                                    selectedTrackId = t.id
                                    onTrackSelect(t.id)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = t.name.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (isSel) CarbonDark else TextWhite
                                )
                            )
                        }
                    }
                }
            }

            // Ranking criteria explanation banner
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RIDER / BIKE",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextGray, fontSize = 10.sp)
                    )
                    Text(
                        text = "DISTANCE (KM) / BEST LAP",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextGray, fontSize = 10.sp)
                    )
                }
            }

            // Entries List
            if (sortedEntries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No records logged yet. Be the first to set a distance record!",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextGray)
                        )
                    }
                }
            } else {
                itemsIndexed(sortedEntries) { idx, entry ->
                    val isTop3 = idx < 3
                    val rankColor = when (idx) {
                        0 -> ElectricYellow
                        1 -> Color(0xFFE2E8F0)
                        2 -> NeonOrange
                        else -> TextMuted
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (entry.isCurrentPlayer) CarbonSurface else CarbonCard,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (entry.isCurrentPlayer) 2.dp else 0.8.dp,
                            color = if (entry.isCurrentPlayer) NeonCyan else CarbonCardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CutCornerShape(6.dp))
                                        .background(if (isTop3) rankColor.copy(alpha = 0.2f) else CarbonDark)
                                        .border(1.dp, if (isTop3) rankColor else Color.Transparent, CutCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${idx + 1}",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = rankColor
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = entry.playerName,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (entry.isCurrentPlayer) NeonCyan else TextWhite
                                            )
                                        )
                                        if (entry.isCurrentPlayer) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(NeonCyan.copy(alpha = 0.15f))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "YOU",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = NeonCyan,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${entry.bikeName} • ⚡ ${entry.topSpeedKmh.toInt()} KM/H",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextGray, fontSize = 11.sp)
                                    )
                                }
                            }

                            // Distance and Time Readout
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "%.1f KM".format(entry.distanceKm),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (idx == 0) ElectricYellow else TextWhite
                                    )
                                )

                                val t = entry.timeMs
                                val min = t / 60000
                                val sec = (t % 60000) / 1000
                                val ms = (t % 1000) / 10
                                Text(
                                    text = "%02d:%02d.%02d".format(min, sec, ms),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text("Edit Registered User Name", fontWeight = FontWeight.Bold, color = TextWhite)
            },
            text = {
                Column {
                    Text(
                        "Update the pilot name that appears at Rank #1 on the leaderboards:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextGray)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newNameInput,
                        onValueChange = { newNameInput = it },
                        singleLine = true,
                        label = { Text("Registered User Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newNameInput.isNotBlank()) {
                            onUpdateRegisteredName(newNameInput.trim())
                            showEditNameDialog = false
                        }
                    }
                ) {
                    Text("SAVE", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("CANCEL", color = TextGray)
                }
            },
            containerColor = CarbonCard
        )
    }
}
