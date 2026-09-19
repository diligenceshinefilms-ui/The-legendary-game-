package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.model.EffectiveBikeStats
import com.example.core.model.Player
import com.example.ui.components.CurrencyHeader
import com.example.ui.components.NeonButton
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    player: Player?,
    bikes: List<EffectiveBikeStats>,
    onQuickRace: () -> Unit,
    onGarage: () -> Unit,
    onChampionship: () -> Unit,
    onTimeTrial: () -> Unit,
    onPractice: () -> Unit,
    onLeaderboard: () -> Unit,
    onAchievements: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    onDailyChallenge: () -> Unit,
    onMultiplayer: () -> Unit,
    onAuth: () -> Unit
) {
    val selectedBike = bikes.firstOrNull { it.bike.id == player?.selectedBikeId } ?: bikes.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. Top Bar: Player Stats & Balance
        item {
            CurrencyHeader(
                player = player,
                onProfileClick = onProfile
            )
        }

        // 1.5 Brand Logo & Motto (LEGEND RACER - RIDE FAST. BECOME A LEGEND.)
        item {
            com.example.ui.components.LegendRacerBrandLogo(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // 2. Hero Featured Motorcycle Showcase (Galaxy Superbike Edition)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(GalaxyPink, GalaxyViolet, GalaxyCyan)),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                // Hero Background Image Asset (Galaxy Themed Superbike)
                Image(
                    painter = painterResource(id = R.drawable.galaxy_bike_hero),
                    contentDescription = "Galaxy Superbike Hero",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark Cosmic Nebula Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    GalaxyVoid.copy(alpha = 0.5f),
                                    GalaxyVoid.copy(alpha = 0.95f)
                                )
                            )
                        )
                )

                // Top Badge: GALAXY EDITION
                Surface(
                    color = GalaxyPink.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(bottomEnd = 12.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "⚡ GALAXY BIKE EDITION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            fontSize = 9.sp,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Bike Specs Overlay in Bottom of Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "CURRENT RIDE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GalaxyCyan,
                                    letterSpacing = 1.5.sp
                                )
                            )
                            Text(
                                text = selectedBike?.bike?.name ?: "Galaxy Superbike",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    color = TextWhite
                                )
                            )
                        }

                        // Top Speed Pill
                        Surface(
                            color = GalaxyCard.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GalaxyPink)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = GalaxyPink,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${selectedBike?.topSpeedKmh?.toInt() ?: 295} KM/H",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = TextWhite
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Primary Action Buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeonButton(
                    text = stringResource(R.string.menu_play),
                    icon = Icons.Default.SportsScore,
                    color = NeonCyan,
                    isPrimary = true,
                    onClick = onQuickRace,
                    modifier = Modifier.weight(1.3f),
                    testTag = "btn_race_now"
                )

                NeonButton(
                    text = stringResource(R.string.menu_garage),
                    icon = Icons.Default.Build,
                    color = NeonOrange,
                    isPrimary = false,
                    onClick = onGarage,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_garage"
                )
            }
        }

        // 3.5 Guest Cloud Sync Banner (Register / Login UI Prompt)
        if (player?.isGuest == true) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAuth() },
                    color = NeonOrange.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonOrange.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = NeonOrange,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "PLAYING AS GUEST",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = NeonOrange
                                    )
                                )
                                Text(
                                    text = "Tap to Register or Login to save your progress in the cloud.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextWhite),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = NeonOrange
                        )
                    }
                }
            }
        }

        // 4. Secondary Game Modes Grid
        item {
            Text(
                text = "RACING MODES",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.2.sp
                ),
                modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ModeCard(
                    title = "Championship",
                    subtitle = "Season Cup",
                    icon = Icons.Default.EmojiEvents,
                    color = ElectricYellow,
                    onClick = onChampionship,
                    modifier = Modifier.weight(1f),
                    testTag = "mode_championship"
                )
                ModeCard(
                    title = "Time Trial",
                    subtitle = "Lap Attack",
                    icon = Icons.Default.Timer,
                    color = NitroPurple,
                    onClick = onTimeTrial,
                    modifier = Modifier.weight(1f),
                    testTag = "mode_time_trial"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ModeCard(
                    title = "Practice",
                    subtitle = "Solo Circuit",
                    icon = Icons.Default.SportsMotorsports,
                    color = EmeraldGreen,
                    onClick = onPractice,
                    modifier = Modifier.weight(1f),
                    testTag = "mode_practice"
                )
                ModeCard(
                    title = "Daily Quest",
                    subtitle = "Bonus Bounty",
                    icon = Icons.Default.CalendarToday,
                    color = NeonOrange,
                    onClick = onDailyChallenge,
                    modifier = Modifier.weight(1f),
                    testTag = "mode_daily"
                )
            }
        }

        // 5. Secondary Quick Links (Leaderboard, Achievements, Settings, Multiplayer)
        item {
            Text(
                text = "COMMUNITY & CAREER",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.2.sp
                ),
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickNavRow(
                    title = "Global Leaderboards",
                    desc = "Compare fastest lap records worldwide",
                    icon = Icons.Default.Leaderboard,
                    color = NeonCyan,
                    onClick = onLeaderboard
                )
                QuickNavRow(
                    title = "Achievements & Trophies",
                    desc = "Unlock rewards & track career milestones",
                    icon = Icons.Default.MilitaryTech,
                    color = ElectricYellow,
                    onClick = onAchievements
                )
                QuickNavRow(
                    title = "Online Multiplayer",
                    desc = "Live lobbies & matchmaking preview",
                    icon = Icons.Default.Groups,
                    color = NitroPurple,
                    onClick = onMultiplayer
                )
                QuickNavRow(
                    title = "Settings & Audio",
                    desc = "Controls, sound volume & cloud account",
                    icon = Icons.Default.Settings,
                    color = TextGray,
                    onClick = onSettings
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Surface(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = CarbonCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CutCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f))
                    .border(1.dp, color.copy(alpha = 0.6f), CutCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextGray,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun QuickNavRow(
    title: String,
    desc: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = CarbonSurface,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, CarbonCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    )
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
