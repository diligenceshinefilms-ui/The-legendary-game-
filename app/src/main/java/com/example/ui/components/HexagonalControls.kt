package com.example.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ControlType
import com.example.game.engine.TelemetryCalculator
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Creates a regular 6-sided Hexagon Path.
 * @param pointyTop true if vertices point up & down; false if vertices point left & right (flat top/bottom).
 */
fun createHexagonPath(size: Size, pointyTop: Boolean = false, inset: Float = 0f): Path {
    val path = Path()
    val width = size.width - (inset * 2)
    val height = size.height - (inset * 2)
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radius = minOf(width, height) / 2f

    val startAngle = if (pointyTop) -30.0 else 0.0 // 0 deg gives flat top & bottom

    for (i in 0 until 6) {
        val angleRad = Math.toRadians(startAngle + (i * 60.0))
        val x = (centerX + radius * cos(angleRad)).toFloat()
        val y = (centerY + radius * sin(angleRad)).toFloat()
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    return path
}

/**
 * High-tech Neon Hexagonal Steering Button with double chevron arrows (`<<` or `>>`).
 * Exact styling matching the Legend Racer concept HUD.
 */
@Composable
fun HexagonalSteeringButton(
    isLeft: Boolean,
    isPressed: Boolean,
    onPressChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 64.dp,
    testTag: String = if (isLeft) "btn_steer_left" else "btn_steer_right"
) {
    val glowColor = NeonCyan
    val baseDark = Color(0xFF071220)

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "hex_scale"
    )

    Box(
        modifier = modifier
            .size(buttonSize)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressChange(true)
                        tryAwaitRelease()
                        onPressChange(false)
                    }
                )
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerPath = createHexagonPath(size = size, pointyTop = false, inset = 2f)
            val innerPath = createHexagonPath(size = size, pointyTop = false, inset = 7f)

            // 1. Dark translucent background fill
            drawPath(
                path = outerPath,
                brush = Brush.radialGradient(
                    colors = if (isPressed) {
                        listOf(glowColor.copy(alpha = 0.45f), baseDark.copy(alpha = 0.95f))
                    } else {
                        listOf(baseDark.copy(alpha = 0.85f), Color(0xFF03070E).copy(alpha = 0.95f))
                    },
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.width * 0.6f
                )
            )

            // 2. Outer glowing cyan border
            drawPath(
                path = outerPath,
                color = if (isPressed) Color.White else glowColor,
                style = Stroke(width = if (isPressed) 3.5f else 2.5f)
            )

            // 3. Inner neon contour
            drawPath(
                path = innerPath,
                color = if (isPressed) glowColor else glowColor.copy(alpha = 0.4f),
                style = Stroke(width = 1.2f)
            )

            // 4. Double chevron arrows `<<` or `>>`
            val cx = size.width / 2f
            val cy = size.height / 2f
            val chevronW = 9f
            val chevronH = 14f
            val spacing = 8f
            val chevronColor = if (isPressed) Color.White else glowColor

            // Draw two chevrons
            val directions = if (isLeft) listOf(-spacing / 2f - chevronW / 2f, spacing / 2f + chevronW / 2f)
            else listOf(-spacing / 2f - chevronW / 2f, spacing / 2f + chevronW / 2f)

            for (offsetX in directions) {
                val p = Path()
                if (isLeft) {
                    p.moveTo(cx + offsetX + (chevronW / 2f), cy - chevronH)
                    p.lineTo(cx + offsetX - (chevronW / 2f), cy)
                    p.lineTo(cx + offsetX + (chevronW / 2f), cy + chevronH)
                } else {
                    p.moveTo(cx + offsetX - (chevronW / 2f), cy - chevronH)
                    p.lineTo(cx + offsetX + (chevronW / 2f), cy)
                    p.lineTo(cx + offsetX - (chevronW / 2f), cy + chevronH)
                }
                drawPath(
                    path = p,
                    color = chevronColor,
                    style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

/**
 * Large Hexagonal Nitro Boost Button with double cyan border and bold "NITRO" label.
 */
@Composable
fun HexagonalNitroButton(
    isPressed: Boolean,
    nitroFuel: Float,
    onPressChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 78.dp,
    testTag: String = "btn_nitro"
) {
    val isAvailable = nitroFuel > 2f
    val glowColor = if (isAvailable) NeonCyan else TextMuted

    // Pulsing cyan glow animation when Nitro is ready
    val infiniteTransition = rememberInfiniteTransition(label = "nitro_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nitro_pulse_alpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "nitro_scale"
    )

    Box(
        modifier = modifier
            .size(buttonSize)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .pointerInput(isAvailable) {
                if (isAvailable) {
                    detectTapGestures(
                        onPress = {
                            onPressChange(true)
                            tryAwaitRelease()
                            onPressChange(false)
                        }
                    )
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerPath = createHexagonPath(size = size, pointyTop = true, inset = 2f)
            val innerPath = createHexagonPath(size = size, pointyTop = true, inset = 7f)

            // Background radial fill
            drawPath(
                path = outerPath,
                brush = Brush.radialGradient(
                    colors = if (isPressed) {
                        listOf(Color(0xFF00E5FF).copy(alpha = 0.6f), Color(0xFF051B2E))
                    } else if (isAvailable) {
                        listOf(Color(0xFF0A2239).copy(alpha = 0.9f), Color(0xFF030A12).copy(alpha = 0.95f))
                    } else {
                        listOf(Color(0xFF141923).copy(alpha = 0.8f), Color(0xFF0A0D14))
                    },
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            )

            // Outer hexagon glowing border
            val outerBorderColor = if (isPressed) {
                Color.White
            } else if (isAvailable) {
                glowColor.copy(alpha = pulseAlpha)
            } else {
                CarbonCardBorder
            }

            drawPath(
                path = outerPath,
                color = outerBorderColor,
                style = Stroke(width = if (isPressed) 3.5f else 2.5f)
            )

            // Inner hexagon stroke
            drawPath(
                path = innerPath,
                color = if (isAvailable) glowColor.copy(alpha = 0.4f) else Color.Transparent,
                style = Stroke(width = 1.2f)
            )
        }

        // Bold "NITRO" label
        Text(
            text = "NITRO",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = 1.5.sp,
                fontSize = 15.sp,
                color = if (isPressed) Color.White else if (isAvailable) NeonCyan else TextMuted
            )
        )
    }
}

/**
 * High-tech Neon Brake Pedal / Speed Down with horizontal treads, haptics, and glowing state.
 */
@Composable
fun NeonBrakePedal(
    isPressed: Boolean = false,
    onPressChange: (Boolean) -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    width: Dp = 68.dp,
    height: Dp = 90.dp,
    testTag: String = "btn_brake"
) {
    val activeColor = RacingRed
    val idleColor = NeonCyan
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isInteractionPressed by interactionSource.collectIsPressedAsState()
    val visualPressed = isPressed || isInteractionPressed

    var lastClickTime by remember { mutableLongStateOf(0L) }
    val handleDebouncedClick: () -> Unit = {
        val now = SystemClock.uptimeMillis()
        if (now - lastClickTime > 150L) {
            lastClickTime = now
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
    }

    val scaleAnim by animateFloatAsState(
        targetValue = if (visualPressed) 0.92f else 1.0f,
        label = "brake_scale"
    )

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = handleDebouncedClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cornerRadius = CornerRadius(16f, 16f)
            val strokeW = if (visualPressed) 3.8f else 2.2f
            val borderColor = if (visualPressed) activeColor else idleColor

            // Background
            drawRoundRect(
                color = if (visualPressed) activeColor.copy(alpha = 0.45f) else Color(0xFF071220).copy(alpha = 0.88f),
                size = Size(size.width - 4f, size.height - 4f),
                topLeft = Offset(2f, 2f),
                cornerRadius = cornerRadius
            )

            // Neon Outline
            drawRoundRect(
                color = borderColor,
                size = Size(size.width - 4f, size.height - 4f),
                topLeft = Offset(2f, 2f),
                cornerRadius = cornerRadius,
                style = Stroke(width = strokeW)
            )

            // Top badge light
            drawRoundRect(
                color = if (visualPressed) activeColor else activeColor.copy(alpha = 0.6f),
                topLeft = Offset((size.width - 24f) / 2f, 8f),
                size = Size(24f, 4f),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // Horizontal traction treads (slats)
            val treadCount = 4
            val treadSpacing = (size.height - 38f) / (treadCount - 1)
            val treadWidth = size.width * 0.58f
            val startX = (size.width - treadWidth) / 2f

            for (i in 0 until treadCount) {
                val y = 20f + (i * treadSpacing)
                drawRoundRect(
                    color = if (visualPressed) Color.White else idleColor.copy(alpha = 0.85f),
                    topLeft = Offset(startX, y - 2f),
                    size = Size(treadWidth, 5.5f),
                    cornerRadius = CornerRadius(2.5f, 2.5f)
                )
            }
        }

        // Overlay Label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp)
        ) {
            Text(
                text = "SPEED -",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (visualPressed) Color.White else activeColor.copy(alpha = 0.9f)
                )
            )
            Text(
                text = "BRAKE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = if (visualPressed) Color.White else activeColor
                )
            )
        }
    }
}

/**
 * Slanted High-tech Neon Gas / Throttle Pedal / Speed Up with ribbed horizontal traction pads, haptics, and glowing state.
 */
@Composable
fun NeonGasPedal(
    isPressed: Boolean = false,
    onPressChange: (Boolean) -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    width: Dp = 68.dp,
    height: Dp = 90.dp,
    testTag: String = "btn_gas"
) {
    val activeColor = EmeraldGreen
    val idleColor = NeonCyan
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isInteractionPressed by interactionSource.collectIsPressedAsState()
    val visualPressed = isPressed || isInteractionPressed

    var lastClickTime by remember { mutableLongStateOf(0L) }
    val handleDebouncedClick: () -> Unit = {
        val now = SystemClock.uptimeMillis()
        if (now - lastClickTime > 150L) {
            lastClickTime = now
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
    }

    val scaleAnim by animateFloatAsState(
        targetValue = if (visualPressed) 0.92f else 1.0f,
        label = "gas_scale"
    )

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = handleDebouncedClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cornerRadius = CornerRadius(16f, 16f)
            val strokeW = if (visualPressed) 3.8f else 2.2f
            val borderColor = if (visualPressed) activeColor else idleColor

            // Background
            drawRoundRect(
                color = if (visualPressed) activeColor.copy(alpha = 0.45f) else Color(0xFF071220).copy(alpha = 0.88f),
                size = Size(size.width - 4f, size.height - 4f),
                topLeft = Offset(2f, 2f),
                cornerRadius = cornerRadius
            )

            // Neon Outline
            drawRoundRect(
                color = borderColor,
                size = Size(size.width - 4f, size.height - 4f),
                topLeft = Offset(2f, 2f),
                cornerRadius = cornerRadius,
                style = Stroke(width = strokeW)
            )

            // Top badge light
            drawRoundRect(
                color = if (visualPressed) activeColor else activeColor.copy(alpha = 0.6f),
                topLeft = Offset((size.width - 24f) / 2f, 8f),
                size = Size(24f, 4f),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // Ribbed horizontal traction pads
            val treadCount = 5
            val treadSpacing = (size.height - 38f) / (treadCount - 1)
            val treadWidth = size.width * 0.62f
            val startX = (size.width - treadWidth) / 2f

            for (i in 0 until treadCount) {
                val y = 20f + (i * treadSpacing)
                drawRoundRect(
                    color = if (visualPressed) Color.White else idleColor.copy(alpha = 0.85f),
                    topLeft = Offset(startX, y - 2.5f),
                    size = Size(treadWidth, 5.5f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }
        }

        // Overlay Label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp)
        ) {
            Text(
                text = "SPEED +",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (visualPressed) Color.White else activeColor.copy(alpha = 0.9f)
                )
            )
            Text(
                text = "ACCEL",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = if (visualPressed) Color.White else activeColor
                )
            )
        }
    }
}

/**
 * Top HUD Bar matching the reference concept image:
 * - Glowing Pause button `||`
 * - POSITION 1/8
 * - LAP 2/3
 * - Digital Timer 1:12.45
 * - Gold Coin Count 45,670
 */
@Composable
fun LegendRacerTopHud(
    playerRank: Int,
    totalRacers: Int = 8,
    currentLap: Int,
    totalLaps: Int = 3,
    raceTimeMs: Long,
    coins: Int = 45670,
    onPause: () -> Unit,
    onStop: () -> Unit = {},
    activeControlType: ControlType = ControlType.BUTTONS,
    onToggleControlType: () -> Unit = {},
    isFirstPersonCam: Boolean = false,
    onToggleCamera: () -> Unit = {},
    onChangeBike: () -> Unit = {},
    onHonk: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Left Side: Pause Button + Stop Button + Position + Lap
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Glowing Pause Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF081628).copy(alpha = 0.88f))
                    .border(1.5.dp, NeonCyan, RoundedCornerShape(10.dp))
                    .clickable(onClick = onPause)
                    .testTag("btn_pause"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Compact Stop Race Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RacingRed.copy(alpha = 0.88f))
                    .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onStop)
                    .testTag("btn_stop_race"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White)
                )
            }

            // Quick Honk / Horn Button
            Surface(
                onClick = onHonk,
                color = GalaxyVoid.copy(alpha = 0.88f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, ElectricYellow),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("btn_horn_hud")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Honk Horn",
                        tint = ElectricYellow,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "HORN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = ElectricYellow,
                            fontSize = 8.5.sp
                        )
                    )
                }
            }

            // Quick Change Bike Button
            Surface(
                onClick = onChangeBike,
                color = GalaxyVoid.copy(alpha = 0.88f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, GalaxyPink),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("btn_change_bike_hud")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsMotorsports,
                        contentDescription = "Change Bike",
                        tint = GalaxyPink,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "BIKE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = GalaxyPink,
                            fontSize = 8.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            // POSITION 1/8
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "POS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$playerRank",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = ElectricYellow,
                            fontSize = 22.sp
                        )
                    )
                    Text(
                        text = "/$totalRacers",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }

            // LAP 2/3
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "LAP",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$currentLap",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = ElectricYellow,
                            fontSize = 22.sp
                        )
                    )
                    Text(
                        text = "/$totalLaps",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }
        }

        // Right Side: Time 1:12.45 & Coins & Control Mode Chip
        Column(horizontalAlignment = Alignment.End) {
            val minutes = (raceTimeMs / 60000)
            val seconds = (raceTimeMs % 60000) / 1000
            val millis = (raceTimeMs % 1000) / 10
            val timeText = "%d:%02d.%02d".format(minutes, seconds, millis)

            Text(
                text = timeText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = Color.White,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp
                )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Control Mode Switcher Chip
                Surface(
                    onClick = onToggleControlType,
                    color = CarbonDark.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                    modifier = Modifier.testTag("control_mode_switcher")
                ) {
                    val label = when (activeControlType) {
                        ControlType.BUTTONS -> "BTNS"
                        ControlType.STEERING_WHEEL -> "WHEEL"
                        ControlType.MOTION_TILT -> "TILT"
                        ControlType.JOYSTICK -> "STICK"
                    }
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = NeonCyan,
                            fontSize = 8.5.sp
                        )
                    )
                }

                // Camera View Switcher Chip (Chase vs Helmet / Cockpit)
                Surface(
                    onClick = onToggleCamera,
                    color = CarbonDark.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isFirstPersonCam) GalaxyPink else NeonCyan.copy(alpha = 0.7f)),
                    modifier = Modifier.testTag("camera_mode_switcher")
                ) {
                    Text(
                        text = if (isFirstPersonCam) "HELMET" else "CHASE",
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = if (isFirstPersonCam) GalaxyPink else NeonCyan,
                            fontSize = 8.5.sp
                        )
                    )
                }

                // Coins readout with golden coin badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "%,d".format(coins),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 13.sp
                        )
                    )

                    Canvas(modifier = Modifier.size(14.dp)) {
                        val radius = size.minDimension / 2f
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color(0xFFFFEA79), ElectricYellow, Color(0xFFD48800))
                            ),
                            radius = radius
                        )
                        drawCircle(
                            color = Color(0xFFFFF3B0),
                            radius = radius * 0.75f,
                            style = Stroke(width = 1.2f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating 3D Checkpoint Marker Banner on Highway: `>>> [ CHECKPOINT 022 km ]`.
 */
@Composable
fun FloatingCheckpointBanner(
    checkpointKm: Int = 22,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cp_pulse")
    val chevronAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chevron_anim"
    )

    Row(
        modifier = modifier
            .testTag("floating_checkpoint_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Yellow glowing chevrons `>>>`
        Text(
            text = ">>>",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                color = ElectricYellow.copy(alpha = chevronAlpha),
                fontSize = 18.sp,
                letterSpacing = (-2).sp
            )
        )

        // Green/dark checkpoint badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0A291A).copy(alpha = 0.9f))
                .border(1.2.dp, EmeraldGreen, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Checkpoint icon dot
                Canvas(modifier = Modifier.size(7.dp)) {
                    drawCircle(color = EmeraldGreen)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CHECKPOINT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = EmeraldGreen,
                            fontSize = 8.sp,
                            letterSpacing = 0.8.sp
                        )
                    )
                    Text(
                        text = "%03d km".format(checkpointKm),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Modern Minimap & Road Radar indicator shown above steering buttons:
 * - Circuit track layout with racer dots
 * - Distance indicator: ▲ 2.3m
 * - Road lane curve path radar with direction marker
 */
@Composable
fun MinimapAndRadarOverlay(
    distanceToNextKm: Float = 2.3f,
    laneCurve: Float = 0f,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.testTag("minimap_radar_overlay"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // 1. Circuit Track Minimap Outline
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF06101D).copy(alpha = 0.8f))
                .border(0.8.dp, CarbonCardBorder, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                val trackPath = Path().apply {
                    val w = size.width
                    val h = size.height
                    moveTo(w * 0.15f, h * 0.85f)
                    cubicTo(w * 0.05f, h * 0.7f, w * 0.15f, h * 0.35f, w * 0.35f, h * 0.45f)
                    cubicTo(w * 0.5f, h * 0.55f, w * 0.65f, h * 0.1f, w * 0.85f, h * 0.15f)
                    cubicTo(w * 0.98f, h * 0.2f, w * 0.85f, h * 0.7f, w * 0.7f, h * 0.85f)
                    close()
                }

                // Track circuit line
                drawPath(
                    path = trackPath,
                    color = Color.White.copy(alpha = 0.5f),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Player dot (cyan)
                drawCircle(
                    color = NeonCyan,
                    radius = 4f,
                    center = Offset(size.width * 0.42f, size.height * 0.52f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2f,
                    center = Offset(size.width * 0.42f, size.height * 0.52f)
                )

                // Opponent dot (yellow/orange)
                drawCircle(
                    color = ElectricYellow,
                    radius = 3f,
                    center = Offset(size.width * 0.75f, size.height * 0.3f)
                )
                drawCircle(
                    color = NeonOrange,
                    radius = 3f,
                    center = Offset(size.width * 0.2f, size.height * 0.7f)
                )
            }

            // Distance label ▲ 2.3 km
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "▲",
                    color = ElectricYellow,
                    fontSize = 7.sp
                )
                Spacer(modifier = Modifier.width(1.dp))
                Text(
                    text = "%.1f km".format(distanceToNextKm),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 8.sp
                    )
                )
            }
        }

        // 2. Lane Path Curvature Radar
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF06101D).copy(alpha = 0.8f))
                .border(0.8.dp, CarbonCardBorder, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                val cx = size.width / 2f
                val h = size.height

                // Lane curvature path line
                val lanePath = Path().apply {
                    moveTo(cx, h)
                    cubicTo(
                        cx + (laneCurve * 15f), h * 0.65f,
                        cx + (laneCurve * 30f), h * 0.35f,
                        cx + (laneCurve * 20f), 0f
                    )
                }

                drawPath(
                    path = lanePath,
                    color = Color.White.copy(alpha = 0.7f),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                // Player arrow
                val arrowY = h * 0.65f
                val arrowX = cx + (laneCurve * 15f)
                drawCircle(
                    color = EmeraldGreen,
                    radius = 3.5f,
                    center = Offset(arrowX, arrowY)
                )
            }

            Text(
                text = "▲",
                color = EmeraldGreen,
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/**
 * Sleek Center Cockpit Dash:
 * - Arched Tachometer arc (cyan into orange/red)
 * - Big 248 KM/H Digital Speedometer
 * - Glowing Nitro Bar
 */
@Composable
fun ConceptCenterSpeedometer(
    speedKmh: Float,
    isNitroActive: Boolean,
    nitroFuel: Float,
    modifier: Modifier = Modifier
) {
    val telemetry = remember(speedKmh, isNitroActive) {
        TelemetryCalculator.calculate(speedKmh, isNitroActive)
    }

    // Animated gear change bounce
    val gearScale = remember { Animatable(1.0f) }
    var lastGear by remember { mutableStateOf(telemetry.gear) }

    LaunchedEffect(telemetry.gear) {
        if (telemetry.gear != lastGear) {
            lastGear = telemetry.gear
            gearScale.snapTo(1.35f)
            gearScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
    }

    val speedFraction = (speedKmh / 320f).coerceIn(0f, 1f)
    val speedInt = speedKmh.toInt().coerceAtLeast(0)

    val speedColor = when {
        speedKmh > 260f -> GalaxyRed
        speedKmh > 180f -> GalaxyPink
        speedKmh > 90f -> GalaxyOrange
        else -> GalaxyCyan
    }

    Surface(
        color = GalaxyVoid.copy(alpha = 0.94f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isNitroActive) GalaxyPink else GalaxyViolet.copy(alpha = 0.7f)
        ),
        modifier = modifier.testTag("concept_speedometer")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Real Bike TFT Status Bar (ABS, TC, RACE MAP, Shift Indicator)
            Row(
                modifier = Modifier
                    .width(140.dp)
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gear Indicator Pill
                Surface(
                    color = if (telemetry.isRedlining) GalaxyRed.copy(alpha = 0.35f) else GalaxySurface.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (telemetry.isRedlining) GalaxyRed else GalaxyPink.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = if (telemetry.isRedlining) "GEAR ${telemetry.gear} ▲" else "GEAR ${telemetry.gear}",
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            fontSize = 8.5.sp,
                            color = when {
                                telemetry.isRedlining -> GalaxyRed
                                telemetry.gear == "N" -> GalaxyOrange
                                else -> GalaxyPink
                            }
                        )
                    )
                }

                // TC & ABS Status
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ABS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 7.5.sp,
                            color = GalaxyEmerald
                        )
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, color = TextMuted)
                    )
                    Text(
                        text = "TC:ON",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 7.5.sp,
                            color = GalaxyCyan
                        )
                    )
                }
            }

            // Arched Tachometer Rev Arc Above Speed
            Canvas(
                modifier = Modifier
                    .width(140.dp)
                    .height(16.dp)
            ) {
                val w = size.width
                val h = size.height

                // Background arc guide line
                val bgPath = Path().apply {
                    moveTo(4f, h - 2f)
                    quadraticBezierTo(w / 2f, 2f, w - 4f, h - 2f)
                }

                drawPath(
                    path = bgPath,
                    color = Color(0xFF26183E),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )

                // Active rev line in Galaxy neon gradient
                val revWidth = (w - 8f) * speedFraction
                if (revWidth > 4f) {
                    val activePath = Path().apply {
                        moveTo(4f, h - 2f)
                        val midPointFraction = speedFraction
                        val endX = 4f + revWidth
                        val endY = (h - 2f) - (sin(midPointFraction * PI.toFloat()) * (h - 4f))
                        quadraticBezierTo(endX / 2f, 2f, endX, endY)
                    }
                    drawPath(
                        path = activePath,
                        brush = Brush.horizontalGradient(
                            listOf(GalaxyCyan, GalaxyPink, GalaxyMagenta, GalaxyOrange)
                        ),
                        style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                    )
                }
            }

            // High-Contrast Digital Speedometer with LCD 888 Segment Shadow
            Box(
                contentAlignment = Alignment.Center
            ) {
                // LCD 888 ghost backdrop for authentic dashboard feel
                Text(
                    text = "888",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        fontSize = 38.sp,
                        letterSpacing = (-1.5).sp,
                        color = Color.White.copy(alpha = 0.06f)
                    )
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$speedInt",
                        modifier = Modifier.testTag("speed_value_text"),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            fontSize = 38.sp,
                            letterSpacing = (-1.5).sp,
                            color = speedColor
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(
                        modifier = Modifier.padding(bottom = 5.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "KM/H",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = GalaxyCyan,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }

            // Galaxy NITRO Progress Bar below speed
            Row(
                modifier = Modifier
                    .width(135.dp)
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isNitroActive) "NITRO 🔥" else "NITRO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        color = if (isNitroActive) GalaxyPink else GalaxyCyan,
                        fontSize = 8.sp,
                        letterSpacing = 0.8.sp
                    )
                )

                // Galaxy glowing progress bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF201335))
                        .border(0.6.dp, GalaxyPink.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (nitroFuel / 100f).coerceIn(0f, 1f))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(GalaxyCyan, GalaxyPink, GalaxyOrange)
                                )
                            )
                    )
                }
            }
        }
    }
}

/**
 * Iconic "LEGEND RACER" Title Logo and Slogan from user reference mockup.
 */
@Composable
fun LegendRacerBrandLogo(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.testTag("legend_racer_brand_logo"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Streamlined Galaxy Superbike & Cosmic Wing Emblem (Inspired by reference mockup heart/wings motif)
        Canvas(modifier = Modifier.size(width = 110.dp, height = 52.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Outer Cosmic Glow Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GalaxyPink.copy(alpha = 0.35f), GalaxyViolet.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = 48f
                ),
                radius = 48f,
                center = Offset(cx, cy)
            )

            val bikePath = Path().apply {
                // Front aerodynamic nose & fork
                moveTo(cx - 42f, cy + 10f)
                lineTo(cx - 26f, cy - 10f)
                // Swept bubble windscreen & fuel tank
                lineTo(cx - 10f, cy - 18f)
                lineTo(cx + 10f, cy - 14f)
                // Sleek race tail cowl & rear winglet
                lineTo(cx + 34f, cy - 18f)
                lineTo(cx + 42f, cy - 8f)
                lineTo(cx + 26f, cy + 3f)
                // Rear swingarm
                lineTo(cx + 36f, cy + 12f)
                // Belly pan fairing
                lineTo(cx + 6f, cy + 10f)
                lineTo(cx - 20f, cy + 12f)
                close()
            }

            // High-Contrast Mag Alloy Wheels with glowing rim tape
            drawCircle(
                color = GalaxyCyan,
                radius = 11f,
                center = Offset(cx - 30f, cy + 10f),
                style = Stroke(width = 3.5f)
            )
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = Offset(cx - 30f, cy + 10f)
            )

            drawCircle(
                color = GalaxyPink,
                radius = 11f,
                center = Offset(cx + 28f, cy + 10f),
                style = Stroke(width = 3.5f)
            )
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = Offset(cx + 28f, cy + 10f)
            )

            // Galaxy Pearlescent Gradient Body Fairing
            drawPath(
                path = bikePath,
                brush = Brush.horizontalGradient(
                    listOf(GalaxyOrange, GalaxyPink, GalaxyMagenta, GalaxyViolet)
                )
            )

            // Speed Wing / Starlight Sparkle
            drawLine(
                brush = Brush.horizontalGradient(listOf(Color.White, GalaxyCyan)),
                start = Offset(cx - 18f, cy - 12f),
                end = Offset(cx + 22f, cy - 10f),
                strokeWidth = 2.2f,
                cap = StrokeCap.Round
            )
        }

        // Title: GALAXY RACER / LEGEND
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "GALAXY",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 4.sp,
                    fontSize = 26.sp,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "BIKE",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 4.sp,
                    fontSize = 28.sp,
                    color = GalaxyPink
                )
            )
        }

        // Tagline Pill with Galaxy Gradient Border
        Surface(
            color = GalaxyCard.copy(alpha = 0.85f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GalaxyViolet.copy(alpha = 0.6f)),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = "✨ COSMIC HIGHWAY • RIDE BEYOND THE STARS ✨",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    fontSize = 8.5.sp,
                    color = TextLavender
                )
            )
        }
    }
}
