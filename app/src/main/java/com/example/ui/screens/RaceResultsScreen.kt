package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.usecase.RaceRewardSummary
import com.example.ui.components.NeonButton
import com.example.ui.components.PositionBadge
import com.example.ui.theme.*

@Composable
fun RaceResultsScreen(
    summary: RaceRewardSummary?,
    onRestartRace: () -> Unit,
    onGarage: () -> Unit,
    onHome: () -> Unit
) {
    val isWin = summary?.isVictory ?: false
    val pos = summary?.raceResult?.position ?: 1
    val result = summary?.raceResult

    Scaffold(
        containerColor = CarbonDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .testTag("results_screen"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Title & Finish Position
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = if (isWin) "VICTORY!" else "RACE FINISHED",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        color = if (isWin) ElectricYellow else TextWhite
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                PositionBadge(position = pos, modifier = Modifier.scale(1.3f))

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = result?.trackName ?: "Grand Prix Circuit",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                )
            }

            // Middle Section: Race Stats & Earnings Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Timing Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CarbonCard,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "TOTAL TIME",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextGray
                                )
                            )
                            val t = result?.timeMs ?: 0L
                            val min = t / 60000
                            val sec = (t % 60000) / 1000
                            val ms = (t % 1000) / 10
                            Text(
                                text = "%02d:%02d.%02d".format(min, sec, ms),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    color = TextWhite
                                )
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "BEST LAP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextGray
                                )
                            )
                            val bl = result?.bestLapMs ?: 0L
                            val min = bl / 60000
                            val sec = (bl % 60000) / 1000
                            val ms = (bl % 1000) / 10
                            Text(
                                text = if (bl > 0) "%02d:%02d.%02d".format(min, sec, ms) else "--:--.--",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    color = NeonCyan
                                )
                            )
                        }
                    }
                }

                // Rewards Earned Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CarbonSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Coins reward
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = ElectricYellow,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "+%,d".format(summary?.coinsEarned ?: 0),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = ElectricYellow
                                    )
                                )
                                Text(
                                    text = "COINS",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextGray)
                                )
                            }
                        }

                        // XP reward
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "+%,d".format(summary?.xpEarned ?: 0),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = NeonCyan
                                    )
                                )
                                Text(
                                    text = "XP",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextGray)
                                )
                            }
                        }
                    }
                }

                // Level Up celebration pill if applicable
                if (summary?.isLevelUp == true) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = NeonCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.Upgrade, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LEVEL UP! YOU ARE NOW LEVEL ${summary.newLevel}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = NeonCyan
                                )
                            )
                        }
                    }
                }
            }

            // Bottom Navigation Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NeonButton(
                    text = "RACE AGAIN",
                    icon = Icons.Default.Refresh,
                    color = NeonCyan,
                    onClick = onRestartRace,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeonButton(
                        text = "GARAGE",
                        icon = Icons.Default.Build,
                        color = NeonOrange,
                        isPrimary = false,
                        onClick = onGarage,
                        modifier = Modifier.weight(1f)
                    )
                    NeonButton(
                        text = "MAIN MENU",
                        icon = Icons.Default.Home,
                        color = TextWhite,
                        isPrimary = false,
                        onClick = onHome,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
