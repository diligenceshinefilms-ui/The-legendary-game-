package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.core.model.Player
import com.example.ui.components.NeonButton
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    player: Player?,
    onBack: () -> Unit,
    onAuthClick: () -> Unit
) {
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
                        text = "RACER PASSPORT",
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
                .testTag("profile_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Profile Card Header
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CarbonCard,
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CutCornerShape(14.dp))
                                    .background(NeonCyan)
                                    .border(2.dp, NeonOrange, CutCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "LV ${player?.level ?: 1}",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = CarbonDark
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = player?.displayName ?: "Racer Apex",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontStyle = FontStyle.Italic,
                                        color = TextWhite
                                    )
                                )
                                Text(
                                    text = if (player?.isGuest == true) "Offline Guest Profile" else "Cloud Synchronized",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (player?.isGuest == true) TextMuted else EmeraldGreen
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // XP Progress Bar
                        val xpProg = (player?.xp?.toFloat() ?: 0f) / (player?.xpToNextLevel ?: 1000L).toFloat()
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "XP PROGRESSION",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextGray)
                                )
                                Text(
                                    text = "${player?.xp ?: 0} / ${player?.xpToNextLevel ?: 1000} XP",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = NeonCyan)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { xpProg.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = NeonCyan,
                                trackColor = CarbonDark
                            )
                        }
                    }
                }
            }

            // Career Stats Breakdown
            item {
                Text(
                    text = "CAREER STATISTICS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeonOrange,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                val totalRaces = player?.racesCompleted ?: 0
                val wins = player?.wins ?: 0
                val winRate = if (totalRaces > 0) ((wins.toFloat() / totalRaces.toFloat()) * 100).toInt() else 0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBox(title = "TOTAL RACES", value = "$totalRaces", modifier = Modifier.weight(1f))
                    StatBox(title = "VICTORIES", value = "$wins", modifier = Modifier.weight(1f), color = ElectricYellow)
                    StatBox(title = "WIN RATE", value = "$winRate%", modifier = Modifier.weight(1f), color = NeonCyan)
                }
            }

            // Cloud Account Link Banner
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CarbonSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CLOUD PROFILE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        )
                        Text(
                            text = "Sign in or register to sync your garage, balance, and lap records across devices.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextGray)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        NeonButton(
                            text = if (player?.isGuest == true) "SIGN IN / REGISTER" else "CLOUD STATUS: CONNECTED",
                            icon = Icons.Default.CloudSync,
                            color = NeonCyan,
                            isPrimary = false,
                            onClick = onAuthClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = TextWhite
) {
    Surface(
        modifier = modifier.height(80.dp),
        color = CarbonCard,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp))
            Text(text = value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = color))
        }
    }
}
