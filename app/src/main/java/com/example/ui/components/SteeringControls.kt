package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.sensor.TiltSensorReading
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive Analog Steering Wheel / Handlebars with rotation drag and spring-back centering.
 */
@Composable
fun SteeringWheelControl(
    modifier: Modifier = Modifier,
    wheelSizeDp: Int = 160,
    onSteerChange: (Float) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val rotationAnim = remember { Animatable(0f) } // -90 deg (left) to +90 deg (right)
    val maxAngle = 85f

    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(wheelSizeDp.dp)
            .testTag("steering_wheel_control")
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        coroutineScope.launch {
                            rotationAnim.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                            onSteerChange(0f)
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        coroutineScope.launch {
                            rotationAnim.animateTo(0f)
                            onSteerChange(0f)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val currentAngle = rotationAnim.value
                        // Horizontal drag alters the angle
                        val deltaAngle = dragAmount.x * 0.75f
                        val newAngle = (currentAngle + deltaAngle).coerceIn(-maxAngle, maxAngle)

                        coroutineScope.launch {
                            rotationAnim.snapTo(newAngle)
                            val normalized = (newAngle / maxAngle).coerceIn(-1f, 1f)
                            onSteerChange(normalized)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) - 10f
            val angle = rotationAnim.value

            rotate(degrees = angle, pivot = center) {
                // Outer Rim (Dark Carbon)
                drawCircle(
                    color = Color(0xFF161B22),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 24f)
                )

                // Neon Outer Accent Ring
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(NeonCyan, NeonOrange, ElectricYellow, NeonCyan),
                        center = center
                    ),
                    radius = radius + 8f,
                    center = center,
                    style = Stroke(width = 3.5f)
                )

                // 12 o'clock Center Alignment Marker (Electric Yellow)
                val topMarkerY = center.y - radius
                drawRoundRect(
                    color = ElectricYellow,
                    topLeft = Offset(center.x - 8f, topMarkerY - 14f),
                    size = androidx.compose.ui.geometry.Size(16f, 26f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )

                // Left & Right Ergonomic Textured Grip Pads (9 o'clock & 3 o'clock)
                drawArc(
                    color = NeonOrange.copy(alpha = 0.85f),
                    startAngle = 150f,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = 28f, cap = StrokeCap.Round)
                )

                drawArc(
                    color = NeonCyan.copy(alpha = 0.85f),
                    startAngle = 330f,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = 28f, cap = StrokeCap.Round)
                )

                // Triple Steering Spokes
                // 1. Center Bottom Spoke (to 6 o'clock)
                drawLine(
                    color = CarbonCardBorder,
                    start = center,
                    end = Offset(center.x, center.y + radius - 10f),
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )
                // 2. Left Spoke (to ~9 o'clock)
                drawLine(
                    color = CarbonCardBorder,
                    start = center,
                    end = Offset(center.x - radius + 10f, center.y),
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )
                // 3. Right Spoke (to ~3 o'clock)
                drawLine(
                    color = CarbonCardBorder,
                    start = center,
                    end = Offset(center.x + radius - 10f, center.y),
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )

                // Center Hub
                drawCircle(
                    color = Color(0xFF0D1117),
                    radius = radius * 0.38f,
                    center = center
                )
                drawCircle(
                    color = if (isDragging) NeonCyan else NeonOrange,
                    radius = radius * 0.38f,
                    center = center,
                    style = Stroke(width = 3.5f)
                )

                // Center Apex Horn Emblem
                drawCircle(
                    color = CarbonSurfaceHighlight,
                    radius = radius * 0.24f,
                    center = center
                )
            }
        }

        // Steer angle readout pill
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 10.dp),
            color = CarbonDark.copy(alpha = 0.9f),
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
        ) {
            val steerPct = ((rotationAnim.value / maxAngle) * 100).toInt()
            Text(
                text = if (steerPct < 0) "${-steerPct}% L" else if (steerPct > 0) "${steerPct}% R" else "CENTER",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = if (steerPct != 0) NeonCyan else TextMuted,
                    fontSize = 10.sp
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Mobile Motion Tilt Mode HUD Inclinometer.
 * Displays live device roll angle, artificial horizon bar, and calibration trigger.
 */
@Composable
fun MotionTiltInclinometer(
    tiltReading: TiltSensorReading,
    onCalibrate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .testTag("motion_tilt_inclinometer"),
        color = CarbonDark.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ScreenRotation,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "MOTION TILT MODE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Horizon Inclinometer Bar
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(CarbonCard)
                    .border(1.dp, CarbonCardBorder, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Center zero notch
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(14.dp)
                        .background(TextMuted)
                )

                // Moving horizon marker based on tilt
                val offsetFraction = (tiltReading.steerValue).coerceIn(-1f, 1f)
                val isLeaning = kotlin.math.abs(tiltReading.steerValue) > 0.05f

                Box(
                    modifier = Modifier
                        .offset(x = (offsetFraction * 65).dp)
                        .size(width = 24.dp, height = 12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isLeaning) NeonOrange else NeonCyan)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.width(160.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val deg = tiltReading.rollDegrees.toInt()
                Text(
                    text = if (deg < 0) "${-deg}° LEFT" else if (deg > 0) "${deg}° RIGHT" else "0° LEVEL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (deg != 0) NeonOrange else TextWhite,
                        fontSize = 11.sp
                    )
                )

                // Quick calibrate button
                Surface(
                    onClick = onCalibrate,
                    color = CarbonSurface,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, CarbonCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "Calibrate",
                            tint = NeonCyan,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CALIBRATE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Virtual Analog Joystick Slider Thumbpad for precision thumb steering.
 */
@Composable
fun VirtualJoystickSteerControl(
    modifier: Modifier = Modifier,
    onSteerChange: (Float) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val thumbOffsetAnim = remember { Animatable(0f) } // -50dp to +50dp
    val maxTravel = 50f

    Box(
        modifier = modifier
            .width(150.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(CarbonDark.copy(alpha = 0.85f))
            .border(1.5.dp, CarbonCardBorder, RoundedCornerShape(28.dp))
            .testTag("joystick_steer_control")
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {},
                    onDragEnd = {
                        coroutineScope.launch {
                            thumbOffsetAnim.animateTo(
                                0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                            onSteerChange(0f)
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            thumbOffsetAnim.animateTo(0f)
                            onSteerChange(0f)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val current = thumbOffsetAnim.value
                        val newOffset = (current + dragAmount.x).coerceIn(-maxTravel, maxTravel)
                        coroutineScope.launch {
                            thumbOffsetAnim.snapTo(newOffset)
                            onSteerChange((newOffset / maxTravel).coerceIn(-1f, 1f))
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Track guide line
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(4.dp)
                .background(CarbonSurfaceHighlight, RoundedCornerShape(2.dp))
        )

        // Center zero notch
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 16.dp)
                .background(TextMuted)
        )

        // Draggable thumb knob
        Box(
            modifier = Modifier
                .offset(x = thumbOffsetAnim.value.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(NeonCyan, Color(0xFF007A87))
                    )
                )
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(CarbonDark)
            )
        }
    }
}
