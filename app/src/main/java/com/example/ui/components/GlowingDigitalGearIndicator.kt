package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.game.engine.ShiftPrompt
import com.example.game.engine.TelemetryCalculator
import com.example.ui.theme.*

/**
 * High-tech Glowing Digital Gear Shifting Indicator for Racing HUD.
 *
 * Dynamically reacts to motorcycle acceleration and deceleration:
 * - Sequential LED shift lights (Cyan -> Amber -> Pink -> White Flash)
 * - Dynamic shift prompts (▲ SHIFT UP / ▼ SHIFT DOWN / OPTIMAL / REDLINE)
 * - Animated scaling bounce and radial energy flare on gear shifts
 * - Visual chevron motion trails responding to acceleration & engine braking
 * - Digital LCD ghost segment backdrop for authentic high-contrast racing display
 */
@Composable
fun GlowingDigitalGearIndicator(
    speedKmh: Float,
    isAccelerating: Boolean = false,
    isDecelerating: Boolean = false,
    isNitroActive: Boolean = false,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    // 1. Calculate dynamic speed delta for acceleration/deceleration detection
    var prevSpeed by remember { mutableFloatStateOf(speedKmh) }
    var speedDelta by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(speedKmh) {
        speedDelta = speedKmh - prevSpeed
        prevSpeed = speedKmh
    }

    // 2. Compute rich telemetry
    val telemetry: BikeTelemetry = remember(speedKmh, isNitroActive, speedDelta, isAccelerating, isDecelerating) {
        TelemetryCalculator.calculate(
            speedKmh = speedKmh,
            isNitroActive = isNitroActive,
            speedDelta = speedDelta,
            isThrottle = isAccelerating,
            isBrake = isDecelerating
        )
    }

    // 3. Gear change animations: Spring bounce & Radial neon flare burst
    val gearScale = remember { Animatable(1.0f) }
    val gearFlareAlpha = remember { Animatable(0.0f) }
    var lastGear by remember { mutableStateOf(telemetry.gear) }

    LaunchedEffect(telemetry.gear) {
        if (telemetry.gear != lastGear) {
            lastGear = telemetry.gear
            gearScale.snapTo(1.42f)
            gearFlareAlpha.snapTo(1.0f)

            gearScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            gearFlareAlpha.animateTo(
                targetValue = 0.0f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }
    }

    // 4. Infinite animations for Redline strobe and Chevron cycling
    val infiniteTransition = rememberInfiniteTransition(label = "gear_indicator_fx")

    // Redline emergency shift flash
    val redlineStrobe by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "redline_strobe"
    )

    // Chevron offset for acceleration / deceleration movement
    val chevronPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "chevron_flow"
    )

    // Dynamic state colors
    val isRedline = telemetry.isRedlining || telemetry.shiftPrompt == ShiftPrompt.SHIFT_UP_NOW
    val isShiftSoon = telemetry.shiftPrompt == ShiftPrompt.SHIFT_UP_SOON
    val isDownshift = telemetry.shiftPrompt == ShiftPrompt.DOWNSHIFT_NOW || telemetry.shiftPrompt == ShiftPrompt.DOWNSHIFT_SOON

    val primaryGlowColor = when {
        isRedline -> Color(0xFFFF1744)
        isShiftSoon -> GalaxyPink
        isDownshift -> GalaxyOrange
        telemetry.gearNumber == 0 -> GalaxyOrange
        speedDelta > 0.3f || isAccelerating -> GalaxyCyan
        else -> GalaxyViolet
    }

    val borderColor = when {
        isRedline -> Color(0xFFFF1744).copy(alpha = redlineStrobe)
        isShiftSoon -> GalaxyPink
        isDownshift -> GalaxyOrange.copy(alpha = 0.9f)
        else -> primaryGlowColor.copy(alpha = 0.75f)
    }

    // Component Dimensions
    val cardWidth = if (compact) 82.dp else 102.dp
    val cardHeight = if (compact) 88.dp else 102.dp

    Box(
        modifier = modifier
            .testTag("glowing_gear_shift_indicator")
            .size(width = cardWidth, height = cardHeight),
        contentAlignment = Alignment.Center
    ) {
        // Outer Digital Flare Glow Canvas (Expands when shifting or redlining)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Shift burst ripple
            if (gearFlareAlpha.value > 0.01f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGlowColor.copy(alpha = gearFlareAlpha.value * 0.7f),
                            primaryGlowColor.copy(alpha = gearFlareAlpha.value * 0.2f),
                            Color.Transparent
                        ),
                        center = Offset(w / 2f, h / 2f),
                        radius = (w * 0.75f)
                    ),
                    radius = (w * 0.75f),
                    center = Offset(w / 2f, h / 2f)
                )
            }

            // 2. Redline perimeter ambient pulse
            if (isRedline) {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF1744).copy(alpha = 0.35f * redlineStrobe),
                            Color.Transparent
                        ),
                        center = Offset(w / 2f, h / 2f),
                        radius = w * 0.7f
                    ),
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = CornerRadius(12f, 12f)
                )
            }
        }

        // Cyber Glass Substrate Card
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = GalaxyVoid.copy(alpha = 0.94f),
            shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp, topEnd = 4.dp, bottomStart = 4.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 5.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // A. Header Row: "GEAR" Title & Sequential Shift Lights
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GEAR",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 8.sp,
                            color = TextLavender,
                            letterSpacing = 1.sp
                        )
                    )

                    // 5-Stage Sequential LED Shift Lights (MotoGP / F1 Style)
                    SequentialShiftLeds(
                        rpmFraction = telemetry.rpmFraction,
                        isRedlining = isRedline,
                        strobeAlpha = redlineStrobe
                    )
                }

                // B. Center Digital Numeral with LCD 88 Ghost Segment & Acceleration/Deceleration Chevrons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // Left Side: Deceleration Down Chevrons
                    if (isDownshift || speedDelta < -0.3f || isDecelerating) {
                        DecelerationChevronStream(
                            phase = chevronPhase,
                            color = GalaxyOrange,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                    }

                    // Ghost 88 segment backdrop for authentic digital LCD feel
                    Text(
                        text = "8",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            fontSize = if (compact) 32.sp else 38.sp,
                            color = Color.White.copy(alpha = 0.05f)
                        )
                    )

                    // Main Active Gear Numeral
                    Text(
                        text = telemetry.gear,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            fontSize = (if (compact) 32 else 38).sp * gearScale.value,
                            color = if (isRedline && redlineStrobe < 0.5f) Color.White else primaryGlowColor,
                            letterSpacing = (-1).sp
                        )
                    )

                    // Right Side: Acceleration Up Chevrons
                    if (isShiftSoon || isRedline || speedDelta > 0.3f || isAccelerating) {
                        AccelerationChevronStream(
                            phase = chevronPhase,
                            color = if (isRedline) Color(0xFFFF1744) else GalaxyPink,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                }

                // C. Linear Power-Band Gauge (Track position within current gear)
                PowerBandProgressBar(
                    fraction = telemetry.powerBandFraction,
                    isRedlining = isRedline,
                    activeColor = primaryGlowColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // D. Dynamic Shift Prompt Ribbon (Updates in real-time on throttle, speed & braking)
                DynamicShiftPromptRibbon(
                    prompt = telemetry.shiftPrompt,
                    rpm = telemetry.rpm,
                    strobeAlpha = redlineStrobe,
                    compact = compact
                )
            }
        }
    }
}

/**
 * 5-Segment Sequential F1 / MotoGP Shift Lights
 * Lights sequentially illuminate as RPM climbs from 40% to 100%:
 * LEDs 1-2: Galaxy Cyan
 * LEDs 3-4: Electric Yellow/Amber
 * LED 5: Blazing Magenta / Red
 * At Redline: All 5 flash in sync!
 */
@Composable
private fun SequentialShiftLeds(
    rpmFraction: Float,
    isRedlining: Boolean,
    strobeAlpha: Float
) {
    val activeCount = when {
        rpmFraction > 0.88f -> 5
        rpmFraction > 0.76f -> 4
        rpmFraction > 0.62f -> 3
        rpmFraction > 0.48f -> 2
        rpmFraction > 0.32f -> 1
        else -> 0
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 5) {
            val isActive = i < activeCount
            val baseColor = when (i) {
                0, 1 -> GalaxyCyan
                2, 3 -> Color(0xFFFDE047)
                else -> Color(0xFFFF1744)
            }

            val ledColor = when {
                !isActive -> Color(0xFF1E1430)
                isRedlining -> if (strobeAlpha > 0.5f) Color.White else Color(0xFFFF1744)
                else -> baseColor
            }

            Box(
                modifier = Modifier
                    .size(width = 6.dp, height = 5.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(ledColor)
                    .border(
                        0.5.dp,
                        if (isActive) baseColor.copy(alpha = 0.8f) else Color.Transparent,
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

/**
 * Animated streaming chevrons indicating acceleration & upshifting pressure
 */
@Composable
private fun AccelerationChevronStream(
    phase: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 12.dp, height = 30.dp)) {
        val h = size.height
        val w = size.width

        for (i in 0..2) {
            val yOffset = ((i * 10f) - (phase * 10f) + h) % h
            val alpha = (1f - (yOffset / h)).coerceIn(0.2f, 1f)

            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(1f, yOffset + 4f)
                lineTo(w / 2f, yOffset)
                lineTo(w - 1f, yOffset + 4f)
            }
            drawPath(
                path = path,
                color = color.copy(alpha = alpha),
                style = Stroke(width = 1.6f, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Animated streaming chevrons indicating deceleration, engine braking & downshifting
 */
@Composable
private fun DecelerationChevronStream(
    phase: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 12.dp, height = 30.dp)) {
        val h = size.height
        val w = size.width

        for (i in 0..2) {
            val yOffset = ((i * 10f) + (phase * 10f)) % h
            val alpha = (yOffset / h).coerceIn(0.2f, 1f)

            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(1f, yOffset)
                lineTo(w / 2f, yOffset + 4f)
                lineTo(w - 1f, yOffset)
            }
            drawPath(
                path = path,
                color = color.copy(alpha = alpha),
                style = Stroke(width = 1.6f, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Segmented Power Band Progress Bar with 88% Redline Shift Target Notch
 */
@Composable
private fun PowerBandProgressBar(
    fraction: Float,
    isRedlining: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Background track
        drawRoundRect(
            color = Color(0xFF1E1430),
            size = size,
            cornerRadius = CornerRadius(2f, 2f)
        )

        // Active fill
        val fillWidth = (w * fraction.coerceIn(0f, 1f)).coerceAtLeast(0f)
        if (fillWidth > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        GalaxyCyan,
                        GalaxyPink,
                        if (isRedlining) Color.White else activeColor
                    )
                ),
                size = Size(fillWidth, h),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }

        // Optimal Shift Point Marker at 88%
        val shiftMarkerX = w * 0.88f
        drawLine(
            color = Color(0xFFFF1744),
            start = Offset(shiftMarkerX, 0f),
            end = Offset(shiftMarkerX, h),
            strokeWidth = 1.5f
        )
    }
}

/**
 * Real-Time Dynamic Shift Prompt Status Ribbon
 */
@Composable
private fun DynamicShiftPromptRibbon(
    prompt: ShiftPrompt,
    rpm: Int,
    strobeAlpha: Float,
    compact: Boolean
) {
    val (text, bgColor, textColor) = when (prompt) {
        ShiftPrompt.SHIFT_UP_NOW -> Triple(
            "▲ SHIFT NOW!",
            if (strobeAlpha > 0.5f) Color(0xFFFF1744) else GalaxyVoid,
            if (strobeAlpha > 0.5f) Color.White else Color(0xFFFF1744)
        )
        ShiftPrompt.SHIFT_UP_SOON -> Triple(
            "▲ SHIFT UP",
            GalaxyPink.copy(alpha = 0.25f),
            GalaxyPink
        )
        ShiftPrompt.ACCELERATING -> Triple(
            "▲ ACCEL",
            GalaxyCyan.copy(alpha = 0.2f),
            GalaxyCyan
        )
        ShiftPrompt.DOWNSHIFT_NOW -> Triple(
            "▼ SHIFT DOWN!",
            GalaxyOrange.copy(alpha = 0.35f),
            GalaxyOrange
        )
        ShiftPrompt.DOWNSHIFT_SOON -> Triple(
            "▼ DOWN",
            GalaxyOrange.copy(alpha = 0.2f),
            GalaxyOrange
        )
        ShiftPrompt.DECELERATING -> Triple(
            "▼ DECEL",
            GalaxyViolet.copy(alpha = 0.25f),
            GalaxyCyan
        )
        ShiftPrompt.MAX_GEAR -> Triple(
            "TOP GEAR",
            GalaxyViolet.copy(alpha = 0.3f),
            GalaxyPink
        )
        ShiftPrompt.NEUTRAL -> Triple(
            "NEUTRAL",
            GalaxyOrange.copy(alpha = 0.2f),
            GalaxyOrange
        )
        ShiftPrompt.CRUISING -> Triple(
            if (compact) "OPTIMAL" else "%d RPM".format(rpm),
            GalaxySurface.copy(alpha = 0.6f),
            GalaxyCyan
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(3.dp),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, textColor.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 1.5.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = if (compact) 7.5.sp else 8.5.sp,
                color = textColor,
                letterSpacing = 0.5.sp
            ),
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
