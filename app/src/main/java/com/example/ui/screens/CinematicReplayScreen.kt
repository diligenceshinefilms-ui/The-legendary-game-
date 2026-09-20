package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.core.model.EffectiveBikeStats
import com.example.core.model.RacerState
import com.example.core.model.ReplayFrame
import com.example.core.model.Track
import com.example.game.renderer.GameCanvasRenderer
import com.example.game.tracks.TrackCatalog
import com.example.ui.components.NeonButton
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

enum class CinematicCameraMode(val title: String, val icon: @Composable () -> Unit) {
    DYNAMIC_DIRECTOR("DIRECTOR CUT", { Icon(Icons.Default.MovieFilter, contentDescription = null) }),
    TRACKSIDE_SWEEP("TRACKSIDE", { Icon(Icons.Default.Videocam, contentDescription = null) }),
    HELI_CHASE("HELI CAM", { Icon(Icons.Default.Flight, contentDescription = null) }),
    ACTION_EXHAUST("ACTION REAR", { Icon(Icons.Default.Speed, contentDescription = null) }),
    COCKPIT_HELMET("HELMET COCKPIT", { Icon(Icons.Default.TwoWheeler, contentDescription = null) })
}

@Composable
fun CinematicReplayScreen(
    replayFrames: List<ReplayFrame>,
    track: Track?,
    playerBike: EffectiveBikeStats?,
    onBackToResults: () -> Unit
) {
    if (replayFrames.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CarbonDark)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.VideocamOff,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "NO REPLAY RECORDED FOR THIS RACE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                NeonButton(
                    text = "RETURN TO RESULTS",
                    icon = Icons.Default.ArrowBack,
                    color = NeonCyan,
                    onClick = onBackToResults
                )
            }
        }
        return
    }

    val totalDurationMs = remember(replayFrames) {
        val first = replayFrames.first().timestampMs
        val last = replayFrames.last().timestampMs
        (last - first).coerceAtLeast(1000L)
    }

    var isPlaying by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentProgress by remember { mutableFloatStateOf(0.0f) }
    var selectedCamMode by remember { mutableStateOf(CinematicCameraMode.DYNAMIC_DIRECTOR) }

    // Dynamic Director Cut Switcher Timer
    var directorCamSubIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedCamMode, isPlaying) {
        if (selectedCamMode == CinematicCameraMode.DYNAMIC_DIRECTOR && isPlaying) {
            while (isActive) {
                delay(4000)
                directorCamSubIndex = (directorCamSubIndex + 1) % 4
            }
        }
    }

    // Effective active camera preset
    val effectiveCamMode = remember(selectedCamMode, directorCamSubIndex) {
        if (selectedCamMode == CinematicCameraMode.DYNAMIC_DIRECTOR) {
            when (directorCamSubIndex) {
                0 -> CinematicCameraMode.TRACKSIDE_SWEEP
                1 -> CinematicCameraMode.ACTION_EXHAUST
                2 -> CinematicCameraMode.HELI_CHASE
                else -> CinematicCameraMode.COCKPIT_HELMET
            }
        } else {
            selectedCamMode
        }
    }

    // Playback loop ticker
    LaunchedEffect(isPlaying, playbackSpeed, totalDurationMs) {
        if (isPlaying) {
            val tickStep = 0.016f / (totalDurationMs / 1000f)
            while (isActive) {
                delay(16)
                currentProgress += (tickStep * playbackSpeed)
                if (currentProgress >= 1.0f) {
                    currentProgress = 0.0f // Loop seamlessly
                }
            }
        }
    }

    // Calculate active frame based on progress
    val activeFrameIndex = (currentProgress * (replayFrames.size - 1)).toInt().coerceIn(0, replayFrames.size - 1)
    val currentFrame = replayFrames[activeFrameIndex]

    val activeTrackLayout = remember(track) {
        TrackCatalog.getTrackLayout(track?.id ?: "track_city_rush")
    }

    // Construct player racer state for this replay snapshot
    val simulatedPlayerState = remember(currentFrame, playerBike) {
        listOf(
            RacerState(
                id = "player_replay",
                name = playerBike?.bike?.name ?: "Player Superbike",
                isPlayer = true,
                posX = currentFrame.posX,
                posY = currentFrame.totalRaceDistance,
                totalRaceDistance = currentFrame.totalRaceDistance,
                speedKmh = currentFrame.speedKmh,
                leanAngleRad = currentFrame.leanAngleRad,
                primaryColorHex = playerBike?.bike?.primaryColorHex ?: 0xFFFF1744L,
                secondaryColorHex = playerBike?.bike?.secondaryColorHex ?: 0xFF00E5FFL,
                isNitroActive = currentFrame.nitro,
                bikeName = playerBike?.bike?.name ?: "Superbike"
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .testTag("cinematic_replay_screen")
    ) {
        // 1. Hardware Accelerated Replay Canvas
        GameCanvasRenderer(
            track = activeTrackLayout,
            racers = simulatedPlayerState,
            obstacles = emptyList(), // Cinematic focus on player superbike trajectory
            nearMissBonus = if (currentFrame.speedKmh > 180f) "CINEMATIC REPLAY" else null,
            nearMissCombo = 3,
            isFirstPersonCam = (effectiveCamMode == CinematicCameraMode.COCKPIT_HELMET),
            modifier = Modifier.fillMaxSize()
        )

        // 2. Cinematic Letterbox Bars
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Letterbox Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "REC 30S CINEMATIC REPLAY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Color.White
                            )
                        )
                    }

                    Surface(
                        color = NeonCyan.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
                    ) {
                        Text(
                            text = "CAM: ${effectiveCamMode.title}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Bottom Letterbox Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(Color.Black.copy(alpha = 0.85f))
            )
        }

        // 3. Top Action Overlay (Exit Button & Telemetry Badge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToResults,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CutCornerShape(8.dp))
                    .background(CarbonCard.copy(alpha = 0.9f))
                    .border(1.dp, CarbonCardBorder, CutCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit Replay",
                    tint = TextWhite
                )
            }

            // Real-time telemetry badge
            Surface(
                color = CarbonDark.copy(alpha = 0.88f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%,d".format(currentFrame.speedKmh.toInt()),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontStyle = FontStyle.Italic,
                                color = ElectricYellow
                            )
                        )
                        Text(
                            text = "KM/H",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextGray,
                                fontSize = 9.sp
                            )
                        )
                    }

                    // Steer telemetry bar
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (currentFrame.steer < -0.1f) "LEFT" else if (currentFrame.steer > 0.1f) "RIGHT" else "STRAIGHT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                fontSize = 10.sp
                            )
                        )
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(4.dp)
                                .background(CarbonSurface, CircleShape)
                        ) {
                            val steerOffsetDp = (((currentFrame.steer.coerceIn(-1f, 1f) + 1f) / 2f) * 44f).dp
                            Box(
                                modifier = Modifier
                                    .offset(x = steerOffsetDp)
                                    .size(6.dp)
                                    .background(NeonCyan, CircleShape)
                            )
                        }
                    }

                    // Nitro indicator
                    if (currentFrame.nitro) {
                        Surface(
                            color = NeonOrange,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "NITRO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = CarbonDark,
                                    fontSize = 9.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Bottom Controls HUD Card (Timeline Scrubber, Camera Selector & Play/Pause Controls)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 12.dp, end = 12.dp),
            color = CarbonDark.copy(alpha = 0.92f),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Scrubber Timeline Bar & Time Markers
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val currentMs = (currentProgress * totalDurationMs).toLong()
                        val elapsedSec = (currentMs / 1000)
                        val elapsedMsRem = (currentMs % 1000) / 10
                        val totalSec = totalDurationMs / 1000

                        Text(
                            text = "%02d.%02ds".format(elapsedSec, elapsedMsRem),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        )
                        Text(
                            text = "30s REPLAY WINDOW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextGray,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "%02ds".format(totalSec),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = currentProgress,
                        onValueChange = { currentProgress = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = CarbonSurface
                        )
                    )
                }

                // Camera Angle Selector Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CinematicCameraMode.values().forEach { cam ->
                        val isSelected = (selectedCamMode == cam)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedCamMode = cam },
                            color = if (isSelected) NeonCyan.copy(alpha = 0.25f) else CarbonSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) NeonCyan else CarbonCardBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CompositionLocalProvider(
                                    LocalContentColor provides if (isSelected) NeonCyan else TextGray
                                ) {
                                    cam.icon()
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = cam.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                        fontSize = 8.sp,
                                        color = if (isSelected) NeonCyan else TextGray
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Play / Pause & Speed Multiplier Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed Selector (0.5x, 1.0x, 2.0x)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.5f, 1.0f, 2.0f).forEach { spd ->
                            val isSpdSel = (playbackSpeed == spd)
                            Surface(
                                modifier = Modifier.clickable { playbackSpeed = spd },
                                color = if (isSpdSel) ElectricYellow.copy(alpha = 0.25f) else CarbonSurface,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSpdSel) ElectricYellow else CarbonCardBorder
                                )
                            ) {
                                Text(
                                    text = "${spd}X",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSpdSel) ElectricYellow else TextGray
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Play / Pause Toggle Button
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonCyan, NeonOrange)))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = CarbonDark,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
