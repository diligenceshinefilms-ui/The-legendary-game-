package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import com.example.core.model.DailyChallenge
import com.example.core.model.EffectiveBikeStats
import com.example.core.model.Track
import com.example.ui.components.NeonButton
import com.example.ui.theme.*

@Composable
fun DailyChallengeScreen(
    challenge: DailyChallenge?,
    tracks: List<Track>,
    playerBike: EffectiveBikeStats?,
    onBack: () -> Unit,
    onStartChallenge: (Track, EffectiveBikeStats) -> Unit
) {
    val challengeTrack = tracks.firstOrNull { it.id == challenge?.trackId } ?: tracks.firstOrNull()
    val isCompleted = challenge?.isCompleted ?: false

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
                        text = "DAILY BOUNTY QUEST",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .testTag("daily_challenge_screen"),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CarbonCard,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonOrange)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TODAY'S BOUNTY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = NeonOrange,
                                letterSpacing = 1.5.sp
                            )
                        )
                        if (isCompleted) {
                            Surface(
                                color = EmeraldGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "CLAIMED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = EmeraldGreen
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = challenge?.trackName ?: "City Rush Speed Trap",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = TextWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val targetMs = challenge?.targetTimeMs ?: 65000L
                    val min = targetMs / 60000
                    val sec = (targetMs % 60000) / 1000
                    val ms = (targetMs % 1000) / 10

                    Text(
                        text = "Beat target lap time under %02d:%02d.%02d to claim rewards!".format(min, sec, ms),
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextGray)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Bounty Rewards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = CarbonDark,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = ElectricYellow)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = "+%,d".format(challenge?.rewardCoins ?: 750), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = ElectricYellow))
                                    Text(text = "COINS", style = MaterialTheme.typography.labelSmall.copy(color = TextGray))
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = CarbonDark,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = "+%,d".format(challenge?.rewardXp ?: 300), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = NeonCyan))
                                    Text(text = "XP", style = MaterialTheme.typography.labelSmall.copy(color = TextGray))
                                }
                            }
                        }
                    }
                }
            }

            NeonButton(
                text = if (isCompleted) "PLAY AGAIN (COMPLETED)" else "START BOUNTY ATTEMPT",
                icon = Icons.Default.PlayArrow,
                color = NeonOrange,
                enabled = challengeTrack != null && playerBike != null,
                onClick = {
                    if (challengeTrack != null && playerBike != null) {
                        onStartChallenge(challengeTrack, playerBike)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
