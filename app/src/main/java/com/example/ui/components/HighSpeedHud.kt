package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.BikeTelemetry
import com.example.game.engine.TelemetryCalculator
import com.example.game.environment.CityMilestoneCatalog
import com.example.ui.theme.*

/**
 * High-Speed Heads-Up Display (HUD) showing current speed, current gear,
 * RPM tachometer arc, nitro level, distance telemetry, and active City Milestone badge.
 */
@Composable
fun HighSpeedDashboardHud(
    speedKmh: Float,
    isNitroActive: Boolean,
    nitroFuel: Float,
    topSpeedKmh: Float,
    totalDistanceMeters: Float,
    onStopRace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val telemetry = remember(speedKmh, isNitroActive) {
        TelemetryCalculator.calculate(speedKmh, isNitroActive)
    }

    val currentCity = remember(totalDistanceMeters) {
        CityMilestoneCatalog.getCityForDistance(totalDistanceMeters)
    }

    // Gear shift change animation state
    val gearScale = remember { Animatable(1.0f) }
    val gearFlashAlpha = remember { Animatable(0.0f) }
    var lastGear by remember { mutableStateOf(telemetry.gear) }

    LaunchedEffect(telemetry.gear) {
        if (telemetry.gear != lastGear) {
            lastGear = telemetry.gear
            gearScale.snapTo(1.45f)
            gearFlashAlpha.snapTo(1.0f)
            gearScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
            gearFlashAlpha.animateTo(
                targetValue = 0.0f,
                animationSpec = tween(350)
            )
        }
    }

    // Infinite redline pulse animation when rev limiter is reached
    val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
    val redlineAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "redline_flash"
    )

    // Animated glow color for speed
    val speedGlowColor = when {
        telemetry.speedKmh > 260f -> GalaxyRed
        telemetry.speedKmh > 180f -> GalaxyPink
        telemetry.speedKmh > 90f -> GalaxyCyan
        else -> TextWhite
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("high_speed_hud"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 0. Active City Expressway Sub-bar (Displays City Tag & Famous Tagline)
        Surface(
            color = CarbonDark.copy(alpha = 0.90f),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, currentCity.primaryColor.copy(alpha = 0.6f)),
            modifier = Modifier
                .padding(bottom = 2.dp)
                .testTag("city_highway_badge")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = currentCity.emoji,
                    fontSize = 12.sp
                )
                Text(
                    text = "${currentCity.name} EXPRESSWAY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = currentCity.primaryColor,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 8.sp)
                )
                Text(
                    text = currentCity.tagline,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = currentCity.secondaryColor,
                        fontSize = 8.5.sp
                    )
                )
            }
        }

        // 1. Tachometer LED Bar & Shift Lights
        RpmTachometerBar(
            rpmFraction = telemetry.rpmFraction,
            rpm = telemetry.rpm,
            isRedlining = telemetry.isRedlining,
            redlineAlpha = redlineAlpha,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 2. Main Center Gauges Row: Speedometer + Current Gear + Nitro Tank + Stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Glowing Digital Gear Shifting Indicator UI Component
            GlowingDigitalGearIndicator(
                speedKmh = speedKmh,
                isNitroActive = isNitroActive,
                compact = true,
                modifier = Modifier.padding(end = 4.dp)
            )

            // Center: Real Bike TFT Console Digital Speedometer
            RealBikeDigitalSpeedometer(
                speedKmh = speedKmh,
                topSpeedKmh = topSpeedKmh,
                isNitroActive = isNitroActive
            )

            // Right: Nitro Tank & Distance Telemetry
            Surface(
                color = CarbonDark.copy(alpha = 0.95f),
                shape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NitroPurple),
                modifier = Modifier.testTag("nitro_and_stats")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Nitro Fuel Progress
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = NitroPurple,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "NITRO ${(nitroFuel).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = NitroPurple,
                                fontSize = 9.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = { (nitroFuel / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .width(80.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NitroPurple,
                        trackColor = CarbonSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Distance & Top Speed
                    val km = totalDistanceMeters / 1000f
                    Text(
                        text = "%.2f KM".format(km),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = TextWhite,
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = "TOP: ${topSpeedKmh.toInt()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NeonOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Segmented LED Rev Meter (Cyan -> Amber -> Red)
 */
@Composable
private fun RpmTachometerBar(
    rpmFraction: Float,
    rpm: Int,
    isRedlining: Boolean,
    redlineAlpha: Float,
    modifier: Modifier = Modifier
) {
    val segments = 24
    val activeSegments = (rpmFraction * segments).toInt().coerceIn(0, segments)

    Row(
        modifier = modifier
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(CarbonCard)
            .border(0.8.dp, CarbonCardBorder, RoundedCornerShape(5.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until segments) {
            val isActive = i < activeSegments
            val segColor = when {
                i >= 20 -> RacingRed // Redline (last 4 segments)
                i >= 14 -> NeonOrange // Mid-high (segments 14-19)
                i >= 8 -> ElectricYellow // Mid (segments 8-13)
                else -> NeonCyan // Low-mid
            }

            val finalColor = if (isActive) {
                if (i >= 20 && isRedlining) segColor.copy(alpha = redlineAlpha) else segColor
            } else {
                CarbonSurface.copy(alpha = 0.5f)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.dp))
                    .background(finalColor)
            )
        }
    }
}

/**
 * High-contrast Real-Bike Inspired TFT Digital Speedometer Console UI Component
 */
@Composable
fun RealBikeDigitalSpeedometer(
    speedKmh: Float,
    topSpeedKmh: Float,
    isNitroActive: Boolean,
    modifier: Modifier = Modifier
) {
    val speedInt = speedKmh.toInt().coerceAtLeast(0)
    val speedRatio = (speedKmh / 350f).coerceIn(0f, 1f)

    // Animated smooth speed ratio for fluid arc sweep
    val animSpeedRatio by animateFloatAsState(
        targetValue = speedRatio,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "speed_ratio"
    )

    val speedColor = when {
        speedKmh > 260f -> GalaxyRed
        speedKmh > 180f -> GalaxyPink
        speedKmh > 90f -> GalaxyOrange
        else -> GalaxyCyan
    }

    Surface(
        color = GalaxyVoid.copy(alpha = 0.95f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            if (isNitroActive) GalaxyPink else GalaxyViolet.copy(alpha = 0.85f)
        ),
        modifier = modifier.testTag("speedometer_gauge")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Real Bike TFT Dashboard Status Bar (ABS, TC, Peak Record)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Text(
                    text = "ABS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = GalaxyEmerald
                    )
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = TextMuted)
                )
                Text(
                    text = "TC:ON",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = GalaxyCyan
                    )
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = TextMuted)
                )
                Text(
                    text = "MAX: ${topSpeedKmh.toInt()}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLavender
                    )
                )
            }

            // High-Contrast LCD 888 Segment Shadow + Real-Time Velocity Digits
            Box(
                contentAlignment = Alignment.Center
            ) {
                // LCD 888 Segment Shadow for authentic motorcycle instrument panel feel
                Text(
                    text = "888",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        fontSize = 44.sp,
                        letterSpacing = (-1.5).sp,
                        color = Color.White.copy(alpha = 0.07f)
                    )
                )

                // High-Contrast Velocity Readout
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "%03d".format(speedInt),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            fontSize = 44.sp,
                            letterSpacing = (-1.5).sp,
                            color = speedColor
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(
                        modifier = Modifier.padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "KM/H",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = GalaxyCyan,
                                letterSpacing = 1.2.sp,
                                fontSize = 11.sp
                            )
                        )
                        if (isNitroActive) {
                            Text(
                                text = "NITRO 🔥",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = GalaxyPink,
                                    fontSize = 8.5.sp
                                )
                            )
                        }
                    }
                }
            }

            // Real-Time Speedometer Arc Progress Sweep Bar
            Canvas(
                modifier = Modifier
                    .width(110.dp)
                    .height(6.dp)
                    .padding(top = 2.dp)
            ) {
                val barW = size.width
                val barH = size.height

                // Track Background Bar
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.12f),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(3f, 3f)
                )

                // Fill progress according to real-time velocity ratio
                val filledW = barW * animSpeedRatio
                if (filledW > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(GalaxyCyan, GalaxyPink, GalaxyMagenta, GalaxyOrange)
                        ),
                        size = Size(filledW, barH),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            }
        }
    }
}
