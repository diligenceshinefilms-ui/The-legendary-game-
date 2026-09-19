package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ControlType
import com.example.core.model.GameSettings
import com.example.core.model.GraphicsQuality
import com.example.game.sensor.MotionTiltSensorManager
import com.example.ui.components.MotionTiltInclinometer
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    settings: GameSettings,
    onBack: () -> Unit,
    onSaveSettings: (GameSettings) -> Unit
) {
    val context = LocalContext.current
    var sfxVol by remember(settings) { mutableFloatStateOf(settings.sfxVolume) }
    var musicVol by remember(settings) { mutableFloatStateOf(settings.musicVolume) }
    var vibration by remember(settings) { mutableStateOf(settings.vibrationEnabled) }
    var controlType by remember(settings) { mutableStateOf(settings.controlType) }
    var autoAccel by remember(settings) { mutableStateOf(settings.autoAccelerate) }
    var graphicsQuality by remember(settings) { mutableStateOf(settings.graphicsQuality) }
    var tiltSensitivity by remember(settings) { mutableFloatStateOf(settings.tiltSensitivity) }
    var tiltDeadzone by remember(settings) { mutableFloatStateOf(settings.tiltDeadzone) }
    var invertTilt by remember(settings) { mutableStateOf(settings.invertTilt) }

    val motionSensorManager = remember {
        MotionTiltSensorManager(context).apply {
            sensitivity = tiltSensitivity
            deadzone = tiltDeadzone
            isInverted = invertTilt
        }
    }
    val tiltReading by motionSensorManager.tiltReading.collectAsState()

    DisposableEffect(controlType) {
        if (controlType == ControlType.MOTION_TILT) {
            motionSensorManager.startListening()
        } else {
            motionSensorManager.stopListening()
        }
        onDispose {
            motionSensorManager.stopListening()
        }
    }

    fun applyChange() {
        motionSensorManager.sensitivity = tiltSensitivity
        motionSensorManager.deadzone = tiltDeadzone
        motionSensorManager.isInverted = invertTilt

        onSaveSettings(
            settings.copy(
                sfxVolume = sfxVol,
                musicVolume = musicVol,
                vibrationEnabled = vibration,
                controlType = controlType,
                autoAccelerate = autoAccel,
                graphicsQuality = graphicsQuality,
                tiltSensitivity = tiltSensitivity,
                tiltDeadzone = tiltDeadzone,
                invertTilt = invertTilt
            )
        )
    }

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
                        text = "SETTINGS & CONTROLS",
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
                .testTag("settings_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. Steering & Controls Section
            item {
                Text(
                    text = "STEERING & MOTION CONTROLS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CarbonCard,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Choose Steering Mode",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        )

                        // 4 Control Type Selector Grid
                        val controlOptions = listOf(
                            ControlOption(ControlType.BUTTONS, "Touch Buttons", "Left/Right arrow tap buttons", Icons.Default.TouchApp),
                            ControlOption(ControlType.STEERING_WHEEL, "Steering Wheel", "Analog rotating carbon wheel", Icons.Default.SportsMotorsports),
                            ControlOption(ControlType.MOTION_TILT, "Motion Tilt", "Tilt phone gyroscope/accelerometer", Icons.Default.ScreenRotation),
                            ControlOption(ControlType.JOYSTICK, "Virtual Stick", "Smooth thumb joystick slider", Icons.Default.Gamepad)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            controlOptions.forEach { opt ->
                                val isSelected = controlType == opt.type
                                Surface(
                                    onClick = {
                                        controlType = opt.type
                                        applyChange()
                                    },
                                    color = if (isSelected) CarbonSurfaceHighlight else CarbonDark,
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) NeonCyan else CarbonCardBorder
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = opt.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) NeonCyan else TextMuted,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = opt.title,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) NeonCyan else TextWhite
                                                )
                                            )
                                            Text(
                                                text = opt.subtitle,
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextGray)
                                            )
                                        }
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                controlType = opt.type
                                                applyChange()
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = NeonCyan,
                                                unselectedColor = TextMuted
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Detailed Motion Tilt Settings when MOTION_TILT is active
                        if (controlType == ControlType.MOTION_TILT) {
                            HorizontalDivider(color = CarbonCardBorder)

                            Text(
                                text = "MOBILE MOTION SENSOR CALIBRATION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NeonOrange,
                                    letterSpacing = 1.sp
                                )
                            )

                            // Live Sensor Inclinometer Widget in Settings
                            MotionTiltInclinometer(
                                tiltReading = tiltReading,
                                onCalibrate = { motionSensorManager.calibrateCenter() },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Tilt Sensitivity Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Tilt Sensitivity", style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite))
                                    Text(text = "%.1fx".format(tiltSensitivity), style = MaterialTheme.typography.bodyMedium.copy(color = NeonOrange, fontWeight = FontWeight.Bold))
                                }
                                Slider(
                                    value = tiltSensitivity,
                                    onValueChange = {
                                        tiltSensitivity = it
                                        applyChange()
                                    },
                                    valueRange = 0.5f..2.5f,
                                    colors = SliderDefaults.colors(thumbColor = NeonOrange, activeTrackColor = NeonOrange)
                                )
                            }

                            // Tilt Deadzone Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Center Deadzone", style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite))
                                    Text(text = "${(tiltDeadzone * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium.copy(color = NeonOrange, fontWeight = FontWeight.Bold))
                                }
                                Slider(
                                    value = tiltDeadzone,
                                    onValueChange = {
                                        tiltDeadzone = it
                                        applyChange()
                                    },
                                    valueRange = 0.0f..0.20f,
                                    colors = SliderDefaults.colors(thumbColor = NeonOrange, activeTrackColor = NeonOrange)
                                )
                            }

                            // Invert Tilt Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Invert Steering Axis", style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite))
                                    Text(text = "Reverses left/right tilt direction", style = MaterialTheme.typography.bodySmall.copy(color = TextGray))
                                }
                                Switch(
                                    checked = invertTilt,
                                    onCheckedChange = {
                                        invertTilt = it
                                        applyChange()
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = NeonOrange, checkedTrackColor = NeonOrange.copy(alpha = 0.5f))
                                )
                            }
                        }

                        HorizontalDivider(color = CarbonCardBorder)

                        // Auto Acceleration Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Auto Acceleration", style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite))
                                Text(text = "Focus purely on steering and nitro timing", style = MaterialTheme.typography.bodySmall.copy(color = TextGray))
                            }
                            Switch(
                                checked = autoAccel,
                                onCheckedChange = { autoAccel = it; applyChange() },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }

            // 2. Audio Section
            item {
                Text(
                    text = "AUDIO & SOUND FX",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeonOrange,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CarbonCard,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // SFX Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Sound Effects", style = MaterialTheme.typography.bodyLarge.copy(color = TextWhite))
                                Text(text = "${(sfxVol * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium.copy(color = NeonCyan, fontWeight = FontWeight.Bold))
                            }
                            Slider(
                                value = sfxVol,
                                onValueChange = { sfxVol = it; applyChange() },
                                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                            )
                        }

                        // Music Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Music Volume", style = MaterialTheme.typography.bodyLarge.copy(color = TextWhite))
                                Text(text = "${(musicVol * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium.copy(color = NeonOrange, fontWeight = FontWeight.Bold))
                            }
                            Slider(
                                value = musicVol,
                                onValueChange = { musicVol = it; applyChange() },
                                colors = SliderDefaults.colors(thumbColor = NeonOrange, activeTrackColor = NeonOrange)
                            )
                        }

                        // Vibration Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Haptic Vibration", style = MaterialTheme.typography.bodyLarge.copy(color = TextWhite))
                                Text(text = "Engine rumble & collision pulses", style = MaterialTheme.typography.bodySmall.copy(color = TextGray))
                            }
                            Switch(
                                checked = vibration,
                                onCheckedChange = { vibration = it; applyChange() },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }

            // 3. Graphics Section
            item {
                Text(
                    text = "GRAPHICS QUALITY",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CarbonCard,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GraphicsQuality.values().forEach { gq ->
                            val isSel = graphicsQuality == gq
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(CutCornerShape(8.dp))
                                    .background(if (isSel) EmeraldGreen else CarbonSurface)
                                    .border(1.dp, if (isSel) Color.White else CarbonCardBorder, CutCornerShape(8.dp))
                                    .clickable { graphicsQuality = gq; applyChange() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = gq.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (isSel) CarbonDark else TextWhite
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ControlOption(
    val type: ControlType,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)
