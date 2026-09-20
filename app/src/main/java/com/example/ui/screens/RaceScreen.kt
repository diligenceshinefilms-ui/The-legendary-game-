package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ControlType
import com.example.core.model.GameEngineState
import com.example.core.model.GameSettings
import com.example.core.model.RacerState
import com.example.game.engine.GameEngine
import com.example.game.environment.CityMilestoneCatalog
import com.example.game.renderer.GameCanvasRenderer
import com.example.game.sensor.MotionTiltSensorManager
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun RaceScreen(
    engine: GameEngine,
    settings: GameSettings = GameSettings(),
    playerCoins: Long = 45670L,
    onFinishRace: () -> Unit,
    onExitToMenu: () -> Unit,
    onInputUpdate: (steer: Float, throttle: Float, brake: Float, nitro: Boolean) -> Unit
) {
    val context = LocalContext.current
    val engineState by engine.engineState.collectAsState()
    val racers by engine.racers.collectAsState()
    val playerRank by engine.playerRank.collectAsState()
    val playerLap by engine.playerLap.collectAsState()
    val raceTimeMs by engine.raceTimeMs.collectAsState()
    val nitroFuel by engine.nitroFuel.collectAsState()
    val countdownVal by engine.countdownValue.collectAsState()
    val topSpeedKmh by engine.topSpeedKmh.collectAsState()
    val totalDistanceMeters by engine.totalDistanceMeters.collectAsState()
    val obstacles by engine.obstacles.collectAsState()
    val missionFailReason by engine.missionFailReason.collectAsState()
    val nearMissBonus by engine.nearMissBonus.collectAsState()
    val nearMissCombo by engine.nearMissCombo.collectAsState()
    val isDrafting by engine.isDrafting.collectAsState()
    val draftIntensity by engine.draftIntensity.collectAsState()

    // Control Type selection (defaults to settings, quickly toggleable in-game)
    var activeControlType by remember(settings.controlType) { mutableStateOf(settings.controlType) }

    // Buttons Control State
    var isSteerLeft by remember { mutableStateOf(false) }
    var isSteerRight by remember { mutableStateOf(false) }

    // Analog Wheel Control State
    var analogWheelSteer by remember { mutableFloatStateOf(0f) }

    // Joystick Control State
    var joystickSteer by remember { mutableFloatStateOf(0f) }

    // Pedals State
    var isGasPedalDown by remember { mutableStateOf(false) }
    var isBrakePedalDown by remember { mutableStateOf(false) }
    var targetThrottle by remember { mutableStateOf(0.35f) } // Base throttle so it auto runs untouched
    var isNitroPressed by remember { mutableStateOf(false) }
    var isFirstPersonCam by remember { mutableStateOf(false) }
    var showBikeSelectorModal by remember { mutableStateOf(false) }

    // Motion Tilt Sensor Manager
    val motionSensorManager = remember {
        MotionTiltSensorManager(context).apply {
            sensitivity = settings.tiltSensitivity
            deadzone = settings.tiltDeadzone
            isInverted = settings.invertTilt
        }
    }
    val tiltReading by motionSensorManager.tiltReading.collectAsState()

    // Manage Audio Synthesizer Lifecycle
    DisposableEffect(engine) {
        engine.audioManager?.startEngineAudio()
        onDispose {
            engine.audioManager?.stopEngineAudio()
        }
    }

    // Manage Sensor Listener Lifecycle
    DisposableEffect(activeControlType, engineState) {
        if (activeControlType == ControlType.MOTION_TILT && (engineState == GameEngineState.RACING || engineState == GameEngineState.COUNTDOWN)) {
            motionSensorManager.startListening()
        } else {
            motionSensorManager.stopListening()
        }
        onDispose {
            motionSensorManager.stopListening()
        }
    }

    val playerRacer = racers.firstOrNull { it.isPlayer }

    // Dispatch synchronized inputs to GameEngine
    LaunchedEffect(
        activeControlType,
        isSteerLeft,
        isSteerRight,
        analogWheelSteer,
        joystickSteer,
        tiltReading.steerValue,
        targetThrottle,
        isBrakePedalDown,
        isNitroPressed,
        settings.autoAccelerate
    ) {
        val steer = when (activeControlType) {
            ControlType.BUTTONS -> {
                when {
                    isSteerLeft && !isSteerRight -> -1.0f
                    isSteerRight && !isSteerLeft -> 1.0f
                    else -> 0.0f
                }
            }
            ControlType.STEERING_WHEEL -> analogWheelSteer.coerceIn(-1.0f, 1.0f)
            ControlType.MOTION_TILT -> tiltReading.steerValue.coerceIn(-1.0f, 1.0f)
            ControlType.JOYSTICK -> joystickSteer.coerceIn(-1.0f, 1.0f)
        }

        val throttle = when {
            isBrakePedalDown -> 0.0f
            isGasPedalDown -> 1.0f
            settings.autoAccelerate -> 0.85f
            else -> 0.0f
        }
        val brake = if (isBrakePedalDown) 1.0f else 0.0f
        val nitro = isNitroPressed

        onInputUpdate(steer, throttle, brake, nitro)
    }

    LaunchedEffect(engineState) {
        if (engineState == GameEngineState.FINISHED) {
            onFinishRace()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .testTag("race_screen")
    ) {
        // 1. Hardware Accelerated Game Canvas (Half screen Road, Half screen Bike)
        GameCanvasRenderer(
            track = engine.trackLayout,
            racers = racers,
            obstacles = obstacles,
            nearMissBonus = nearMissBonus,
            nearMissCombo = nearMissCombo,
            isFirstPersonCam = isFirstPersonCam,
            isDrafting = isDrafting,
            draftIntensity = draftIntensity,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Header HUD Bar - Concept Matching Layout (Pause, Stop, Position, Lap, Timer, Coins, Control Switcher, Camera View Switcher, Horn, Bike)
        LegendRacerTopHud(
            playerRank = playerRank,
            totalRacers = racers.size.coerceAtLeast(8),
            currentLap = playerLap,
            totalLaps = 3,
            raceTimeMs = raceTimeMs,
            coins = playerCoins.toInt(),
            onPause = { engine.pause() },
            onStop = { engine.stopRace() },
            activeControlType = activeControlType,
            onToggleControlType = {
                activeControlType = when (activeControlType) {
                    ControlType.BUTTONS -> ControlType.STEERING_WHEEL
                    ControlType.STEERING_WHEEL -> ControlType.MOTION_TILT
                    ControlType.MOTION_TILT -> ControlType.JOYSTICK
                    ControlType.JOYSTICK -> ControlType.BUTTONS
                }
            },
            isFirstPersonCam = isFirstPersonCam,
            onToggleCamera = { isFirstPersonCam = !isFirstPersonCam },
            onChangeBike = { showBikeSelectorModal = true },
            onHonk = { engine.honk() },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 2.3 Floating 3D Highway Checkpoint Banner (From Reference Image: `>>> [ CHECKPOINT 022 km ]`)
        val nextCheckpointKm = remember(totalDistanceMeters) {
            val currentKm = (totalDistanceMeters / 1000f).toInt()
            if (currentKm < 22) 22 else ((currentKm / 10 + 1) * 10)
        }
        val roadCurvature = remember(totalDistanceMeters) {
            CityMilestoneCatalog.getRoadCurvature(totalDistanceMeters)
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 70.dp)
        ) {
            FloatingCheckpointBanner(checkpointKm = nextCheckpointKm)
        }

        // 2.5 Animated City Crossing / 10KM Milestone Prompt Banner
        val currentCity = remember(totalDistanceMeters) {
            CityMilestoneCatalog.getCityForDistance(totalDistanceMeters)
        }
        val isCrossingMilestone = remember(totalDistanceMeters) {
            CityMilestoneCatalog.isCrossing10KmMilestone(totalDistanceMeters) || totalDistanceMeters < 350f
        }
        val milestoneKm = ((totalDistanceMeters / 10_000f).toInt() * 10).coerceAtLeast(10)

        androidx.compose.animation.AnimatedVisibility(
            visible = isCrossingMilestone && engineState == GameEngineState.RACING,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 96.dp)
        ) {
            CityCrossingPromptBanner(
                city = currentCity,
                milestoneKm = milestoneKm
            )
        }

        // 2.6 Dynamic Turn Steering Advisory Banner (warns of oncoming procedural curve geometry)
        val turnWarning = remember(totalDistanceMeters) {
            CityMilestoneCatalog.getTurnWarning(totalDistanceMeters)
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = turnWarning != null && !isCrossingMilestone && engineState == GameEngineState.RACING,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 98.dp)
        ) {
            if (turnWarning != null) {
                Surface(
                    color = GalaxyVoid.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (turnWarning.intensity > 0.7f) NeonOrange else NeonCyan
                    ),
                    modifier = Modifier.testTag("turn_advisory_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = turnWarning.chevronSymbol,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = if (turnWarning.intensity > 0.7f) NeonOrange else NeonCyan
                            )
                        )
                        Text(
                            text = turnWarning.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = TextWhite,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = turnWarning.chevronSymbol,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = if (turnWarning.intensity > 0.7f) NeonOrange else NeonCyan
                            )
                        )
                    }
                }
            }
        }

        // 3. Bottom Cockpit Dashboard Area (Occupies lower half of screen)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Speedometer & Dashboard Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Above Left Controls: Dynamic Real-Time Circuit & Opponent Radar Mini-map
                DynamicRacingMinimap(
                    playerRacer = playerRacer,
                    racers = racers,
                    obstacles = obstacles,
                    roadCurvature = roadCurvature,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                // Center: Speedometer HUD (Arched Rev Arc, Digital 248 KM/H, Nitro Bar) with Dynamic Slipstream Badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isDrafting,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
                    ) {
                        Surface(
                            color = GalaxyVoid.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                Brush.horizontalGradient(
                                    listOf(NeonCyan, NitroPurple, NeonCyan)
                                )
                            ),
                            shadowElevation = 6.dp,
                            modifier = Modifier.testTag("slipstream_draft_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "⚡ SLIPSTREAM DRAFT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontStyle = FontStyle.Italic,
                                        color = NeonCyan,
                                        fontSize = 8.5.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Text(
                                    text = "+${(draftIntensity * 100).toInt()}% PULL",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = ElectricYellow,
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }
                    }

                    ConceptCenterSpeedometer(
                        speedKmh = playerRacer?.speedKmh ?: 0f,
                        isNitroActive = (playerRacer?.isNitroActive == true) || isNitroPressed,
                        nitroFuel = nitroFuel
                    )
                }

                // Right: Active City Expressway Tag & Dynamic Glowing Gear Shift Indicator
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    // Active City Expressway Tag
                    Surface(
                        color = GalaxyVoid.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GalaxyPink.copy(alpha = 0.7f)),
                        modifier = Modifier.testTag("city_highway_badge")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(currentCity.emoji, fontSize = 8.5.sp)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = currentCity.name.take(6).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = currentCity.primaryColor,
                                        fontSize = 8.sp
                                    )
                                )
                            }
                            Text(
                                text = "%.1f KM".format(totalDistanceMeters / 1000f),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    fontSize = 7.5.sp
                                )
                            )
                        }
                    }

                    // High-tech Glowing Digital Gear Shifting Indicator for Racing HUD
                    GlowingDigitalGearIndicator(
                        speedKmh = playerRacer?.speedKmh ?: 0f,
                        isAccelerating = isGasPedalDown || settings.autoAccelerate,
                        isDecelerating = isBrakePedalDown,
                        isNitroActive = (playerRacer?.isNitroActive == true) || isNitroPressed,
                        compact = true
                    )
                }
            }

            // Interactive Steering and Pedals Touch Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Left Side: Active Steering Mode Controls
                Box(
                    modifier = Modifier.padding(bottom = 2.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    when (activeControlType) {
                        ControlType.BUTTONS -> {
                            // Two High-Tech Glowing Hexagonal Steering Buttons: `<<` and `>>`
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                HexagonalSteeringButton(
                                    isLeft = true,
                                    isPressed = isSteerLeft,
                                    onPressChange = { isSteerLeft = it },
                                    buttonSize = 64.dp,
                                    testTag = "btn_steer_left"
                                )
                                HexagonalSteeringButton(
                                    isLeft = false,
                                    isPressed = isSteerRight,
                                    onPressChange = { isSteerRight = it },
                                    buttonSize = 64.dp,
                                    testTag = "btn_steer_right"
                                )
                            }
                        }
                        ControlType.STEERING_WHEEL -> {
                            SteeringWheelControl(
                                wheelSizeDp = 135,
                                onSteerChange = { analogWheelSteer = it }
                            )
                        }
                        ControlType.MOTION_TILT -> {
                            // On-screen Real-Time Tilt Spirit Level & Calibration Gauge
                            Surface(
                                onClick = { motionSensorManager.calibrateCenter() },
                                color = CarbonDark.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (kotlin.math.abs(tiltReading.steerValue) > 0.1f) NeonCyan else NeonOrange
                                ),
                                modifier = Modifier
                                    .testTag("motion_tilt_gauge")
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = { offset ->
                                                val halfWidth = size.width / 2f
                                                val steer = ((offset.x - halfWidth) / halfWidth).coerceIn(-1f, 1f)
                                                motionSensorManager.setManualTouchSteer(steer)
                                                tryAwaitRelease()
                                                motionSensorManager.setManualTouchSteer(0f)
                                            }
                                        )
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ScreenRotation,
                                            contentDescription = null,
                                            tint = if (tiltReading.steerValue > 0.1f) NeonCyan else if (tiltReading.steerValue < -0.1f) NitroPurple else NeonOrange,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = when {
                                                tiltReading.steerValue > 0.15f -> "TURNING RIGHT >>"
                                                tiltReading.steerValue < -0.15f -> "<< TURNING LEFT"
                                                else -> "TILT PHONE TO STEER"
                                            },
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                color = TextWhite,
                                                fontSize = 11.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Real-Time Lean Angle Bar
                                    Box(
                                        modifier = Modifier
                                            .width(130.dp)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color.Black.copy(alpha = 0.6f))
                                    ) {
                                        // Center reference notch
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .width(2.dp)
                                                .height(6.dp)
                                                .background(Color.White.copy(alpha = 0.6f))
                                        )
                                        // Dynamic steer indicator
                                        val fillFraction = (tiltReading.steerValue.coerceIn(-1f, 1f) + 1f) / 2f
                                        Box(
                                            modifier = Modifier
                                                .align(if (tiltReading.steerValue >= 0) Alignment.CenterStart else Alignment.CenterEnd)
                                                .padding(
                                                    start = if (tiltReading.steerValue >= 0) 65.dp else 0.dp,
                                                    end = if (tiltReading.steerValue < 0) 65.dp else 0.dp
                                                )
                                                .width((kotlin.math.abs(tiltReading.steerValue) * 65).dp)
                                                .height(6.dp)
                                                .background(if (tiltReading.steerValue >= 0) NeonCyan else NitroPurple)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${tiltReading.rollDegrees.toInt()}° • Tap to Calibrate Center",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextMuted,
                                            fontSize = 8.5.sp
                                        )
                                    )
                                }
                            }
                        }
                        ControlType.JOYSTICK -> {
                            VirtualJoystickSteerControl(
                                onSteerChange = { joystickSteer = it }
                            )
                        }
                    }
                }

                // Right Side: Hexagonal Nitro Button + Neon Brake Pedal + Neon Gas Pedal
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Large Hexagonal NITRO Boost Button
                    HexagonalNitroButton(
                        isPressed = isNitroPressed,
                        nitroFuel = nitroFuel,
                        onPressChange = { isNitroPressed = it },
                        buttonSize = 74.dp,
                        testTag = "btn_nitro"
                    )

                    // High-Tech Neon Brake Pedal / Speed Down
                    NeonBrakePedal(
                        isPressed = isBrakePedalDown,
                        onPressChange = { down -> 
                            isBrakePedalDown = down
                            if (down) engine.speedDown()
                        },
                        onClick = { engine.speedDown() },
                        width = 68.dp,
                        height = 90.dp,
                        testTag = "btn_brake"
                    )

                    // Slanted High-Tech Neon Throttle / Speed Up Pedal
                    NeonGasPedal(
                        isPressed = isGasPedalDown,
                        onPressChange = { down -> 
                            isGasPedalDown = down
                            if (down) engine.speedUp()
                        },
                        onClick = { engine.speedUp() },
                        width = 68.dp,
                        height = 90.dp,
                        testTag = "btn_gas"
                    )
                }
            }
        }

        // 4. Countdown 3-2-1-GO Overlay
        if (engineState == GameEngineState.COUNTDOWN) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LegendRacerBrandLogo(modifier = Modifier.padding(bottom = 16.dp))

                    val label = if (countdownVal == 0) "GO!" else "$countdownVal"
                    val textColor = if (countdownVal == 0) EmeraldGreen else ElectricYellow

                    Text(
                        text = label,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 110.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = textColor
                        )
                    )
                }
            }
        }

        // 5. Pause Dialog Overlay
        if (engineState == GameEngineState.PAUSED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .width(340.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    color = GalaxyCard,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GalaxyPink)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LegendRacerBrandLogo(modifier = Modifier.padding(bottom = 4.dp))

                        Text(
                            text = "RACE PAUSED",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontStyle = FontStyle.Italic,
                                color = TextWhite,
                                fontSize = 20.sp
                            )
                        )

                        // Quick Steering Mode in Pause Menu
                        Text(
                            text = "STEERING CONTROL MODE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = GalaxyCyan)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Pair(ControlType.BUTTONS, "Buttons"),
                                Pair(ControlType.STEERING_WHEEL, "Wheel"),
                                Pair(ControlType.MOTION_TILT, "Motion Tilt"),
                                Pair(ControlType.JOYSTICK, "Stick")
                            ).forEach { (type, label) ->
                                val isSel = activeControlType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CutCornerShape(6.dp))
                                        .background(if (isSel) NeonCyan else CarbonDark)
                                        .border(1.dp, if (isSel) Color.White else CarbonCardBorder, CutCornerShape(6.dp))
                                        .clickable { activeControlType = type }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = if (isSel) CarbonDark else TextWhite,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        NeonButton(
                            text = "RESUME",
                            icon = Icons.Default.PlayArrow,
                            color = NeonCyan,
                            onClick = { engine.resume() },
                            modifier = Modifier.fillMaxWidth()
                        )

                        NeonButton(
                            text = "CHANGE BIKE",
                            icon = Icons.Default.SportsMotorsports,
                            color = GalaxyPink,
                            onClick = { showBikeSelectorModal = true },
                            modifier = Modifier.fillMaxWidth().testTag("btn_pause_change_bike")
                        )

                        NeonButton(
                            text = "STOP & FINISH",
                            icon = Icons.Default.Stop,
                            color = RacingRed,
                            isPrimary = true,
                            onClick = { engine.stopRace() },
                            modifier = Modifier.fillMaxWidth()
                        )

                        NeonButton(
                            text = "RESTART RACE",
                            icon = Icons.Default.Refresh,
                            color = NeonOrange,
                            isPrimary = false,
                            onClick = { engine.restart() },
                            modifier = Modifier.fillMaxWidth()
                        )

                        NeonButton(
                            text = "EXIT TO MENU",
                            icon = Icons.Default.ExitToApp,
                            color = TextGray,
                            isPrimary = false,
                            onClick = onExitToMenu,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 6. Mission Failed Overlay (Hit any object -> Mission Fail!)
        if (engineState == GameEngineState.MISSION_FAILED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .testTag("mission_failed_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .width(360.dp)
                        .clip(CutCornerShape(16.dp))
                        .border(2.dp, RacingRed, CutCornerShape(16.dp)),
                    color = CarbonCard
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(RacingRed.copy(alpha = 0.2f))
                                .border(1.5.dp, RacingRed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Crash Hazard",
                                tint = RacingRed,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "MISSION FAILED",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = RacingRed,
                                letterSpacing = 2.sp
                            )
                        )

                        Text(
                            text = missionFailReason ?: "CRITICAL IMPACT DETECTED!",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextWhite,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats Card
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp)),
                            color = CarbonDark
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "RUN DISTANCE",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextGray, fontSize = 9.sp)
                                    )
                                    Text(
                                        text = "%.2f KM".format(totalDistanceMeters / 1000f),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = ElectricYellow,
                                            fontWeight = FontWeight.Black
                                        )
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "IMPACT SPEED",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextGray, fontSize = 9.sp)
                                    )
                                    Text(
                                        text = "${topSpeedKmh.toInt()} KM/H",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = NeonCyan,
                                            fontWeight = FontWeight.Black
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Distance recorded to your Leaderboard ranking!",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        NeonButton(
                            text = "TRY AGAIN",
                            icon = Icons.Default.Refresh,
                            color = NeonOrange,
                            isPrimary = true,
                            onClick = { engine.restart() },
                            modifier = Modifier.fillMaxWidth().testTag("btn_try_again")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        NeonButton(
                            text = "VIEW RANKINGS & LEADERBOARD",
                            icon = Icons.Default.Leaderboard,
                            color = ElectricYellow,
                            isPrimary = false,
                            onClick = onExitToMenu,
                            modifier = Modifier.fillMaxWidth().testTag("btn_view_rankings")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        NeonButton(
                            text = "EXIT TO MENU",
                            icon = Icons.Default.ExitToApp,
                            color = TextGray,
                            isPrimary = false,
                            onClick = onExitToMenu,
                            modifier = Modifier.fillMaxWidth().testTag("btn_exit_failed")
                        )
                    }
                }
            }
        }

        // 7. Free 5-Bike Customization & Switcher Dialog Overlay
        if (showBikeSelectorModal) {
            FreeBikeSelectionModal(
                activeBikeId = playerRacer?.bikeName ?: "bike_galaxy_1000",
                onSelectBike = { bike ->
                    engine.changePlayerBike(
                        bikeId = bike.id,
                        bikeName = bike.name,
                        maxSpeed = bike.topSpeedKmh,
                        primaryColorHex = bike.primaryColorHex,
                        secondaryColorHex = bike.secondaryColorHex,
                        handlingMultiplier = bike.handlingMultiplier,
                        accelMultiplier = bike.accelMultiplier
                    )
                    showBikeSelectorModal = false
                },
                onDismiss = { showBikeSelectorModal = false }
            )
        }
    }
}

data class FreeBikeDesignItem(
    val id: String,
    val name: String,
    val category: String,
    val topSpeedKmh: Float,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val handlingMultiplier: Float,
    val accelMultiplier: Float,
    val badge: String,
    val description: String
)

val FREE_BIKE_DESIGNS = listOf(
    FreeBikeDesignItem(
        id = "bike_turbina_electric",
        name = "TURBINA Hyper-Electric EX-1",
        category = "HYPER ELECTRIC",
        topSpeedKmh = 350f,
        primaryColorHex = 0xFF0B797D,
        secondaryColorHex = 0xFFFFD600,
        handlingMultiplier = 1.25f,
        accelMultiplier = 1.35f,
        badge = "8K CGI MASTER",
        description = "Aerodynamic monocoque electric superbike in deep metallic teal with gold TURBINA decals, 3-loop trefoil rims & P ZERO tires."
    ),
    FreeBikeDesignItem(
        id = "bike_galaxy_1000",
        name = "Galaxy Superbike 1000RR",
        category = "HYPER SPORT",
        topSpeedKmh = 320f,
        primaryColorHex = 0xFFFF0055,
        secondaryColorHex = 0xFF00F0FF,
        handlingMultiplier = 1.0f,
        accelMultiplier = 1.0f,
        badge = "NEON FLAGSHIP",
        description = "Signature neon cosmic superbike with high rev balance and plasma exhaust."
    ),
    FreeBikeDesignItem(
        id = "bike_streetfighter_1200",
        name = "Streetfighter Naked 1200",
        category = "NAKED MUSCLE",
        topSpeedKmh = 295f,
        primaryColorHex = 0xFFFF6600,
        secondaryColorHex = 0xFF1E293B,
        handlingMultiplier = 1.15f,
        accelMultiplier = 1.1f,
        badge = "URBAN BEAST",
        description = "Aggressive exposed trellis frame with razor-sharp cornering agility."
    ),
    FreeBikeDesignItem(
        id = "bike_hayabusa_hyper",
        name = "Hayabusa Hyper Tourer",
        category = "HYPER SPEED",
        topSpeedKmh = 340f,
        primaryColorHex = 0xFFF8FAFC,
        secondaryColorHex = 0xFFFFD700,
        handlingMultiplier = 0.95f,
        accelMultiplier = 1.25f,
        badge = "HIGHWAY ROCKET",
        description = "Aerodynamic supersonic grand tourer built for maximum terminal velocity."
    ),
    FreeBikeDesignItem(
        id = "bike_cyber_pulse",
        name = "Cyber Pulse Neon Concept",
        category = "EXPERIMENTAL",
        topSpeedKmh = 330f,
        primaryColorHex = 0xFF39FF14,
        secondaryColorHex = 0xFF00E5FF,
        handlingMultiplier = 1.1f,
        accelMultiplier = 1.15f,
        badge = "MATRIX PROTOTYPE",
        description = "Advanced electric pulse-drive powertrain with instant torque delivery."
    ),
    FreeBikeDesignItem(
        id = "bike_cafe_racer_800",
        name = "Cafe Racer Custom 800",
        category = "RETRO CLASSIC",
        topSpeedKmh = 280f,
        primaryColorHex = 0xFFDC2626,
        secondaryColorHex = 0xFF64748B,
        handlingMultiplier = 1.2f,
        accelMultiplier = 1.05f,
        badge = "CUSTOM CAFE",
        description = "Classic cafe racer stance with lightweight clip-ons for quick lane changes."
    )
)

@Composable
fun FreeBikeSelectionModal(
    activeBikeId: String,
    onSelectBike: (FreeBikeDesignItem) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIndex by remember {
        val idx = FREE_BIKE_DESIGNS.indexOfFirst { it.id == activeBikeId || it.name == activeBikeId }
        mutableIntStateOf(if (idx >= 0) idx else 0)
    }
    val selectedBike = FREE_BIKE_DESIGNS[selectedIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(onClick = onDismiss)
            .testTag("bike_selection_modal"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(360.dp)
                .clickable(enabled = false) {}
                .clip(CutCornerShape(16.dp))
                .border(2.dp, GalaxyPink, CutCornerShape(16.dp)),
            color = GalaxyCard
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SportsMotorsports,
                            contentDescription = "Bike Icon",
                            tint = GalaxyPink,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CHANGE BIKE",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontStyle = FontStyle.Italic,
                                color = TextWhite,
                                fontSize = 18.sp
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextGray
                        )
                    }
                }

                Text(
                    text = "All 5 Racing Bike Designs are 100% FREE! Select a bike to apply immediately.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = GalaxyCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 5 Bike Thumbnails
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FREE_BIKE_DESIGNS.forEachIndexed { index, bike ->
                        val isSel = index == selectedIndex
                        val pColor = Color(bike.primaryColorHex)
                        val sColor = Color(bike.secondaryColorHex)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) CarbonSurface else CarbonDark)
                                .border(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) GalaxyPink else CarbonCardBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedIndex = index }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Color swatch
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(pColor)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(sColor)
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "0${index + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (isSel) ElectricYellow else TextGray,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Selected Bike Card Details
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    color = CarbonDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(selectedBike.primaryColorHex).copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedBike.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    color = TextWhite,
                                    fontSize = 14.sp
                                )
                            )
                            Surface(
                                color = Color(selectedBike.primaryColorHex).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(selectedBike.primaryColorHex))
                            ) {
                                Text(
                                    text = "FREE",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = EmeraldGreen,
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }

                        Text(
                            text = selectedBike.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontSize = 10.5.sp
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("TOP SPEED", style = MaterialTheme.typography.labelSmall.copy(color = TextGray, fontSize = 8.sp))
                                Text("${selectedBike.topSpeedKmh.toInt()} KM/H", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, color = ElectricYellow, fontSize = 11.sp))
                            }
                            Column {
                                Text("HANDLING", style = MaterialTheme.typography.labelSmall.copy(color = TextGray, fontSize = 8.sp))
                                Text("${(selectedBike.handlingMultiplier * 100).toInt()}%", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, color = NeonCyan, fontSize = 11.sp))
                            }
                            Column {
                                Text("ACCELERATION", style = MaterialTheme.typography.labelSmall.copy(color = TextGray, fontSize = 8.sp))
                                Text("${(selectedBike.accelMultiplier * 100).toInt()}%", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, color = GalaxyPink, fontSize = 11.sp))
                            }
                        }
                    }
                }

                // Equip Button
                NeonButton(
                    text = "EQUIP & RIDE NOW",
                    icon = Icons.Default.CheckCircle,
                    color = GalaxyPink,
                    isPrimary = true,
                    onClick = { onSelectBike(selectedBike) },
                    modifier = Modifier.fillMaxWidth().testTag("btn_equip_free_bike")
                )
            }
        }
    }
}

@Composable
private fun TouchControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPressed: Boolean,
    activeColor: Color,
    onPressChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Box(
        modifier = modifier
            .clip(CutCornerShape(12.dp))
            .background(if (isPressed) activeColor else CarbonDark.copy(alpha = 0.85f))
            .border(
                width = if (isPressed) 2.dp else 1.dp,
                color = if (isPressed) Color.White else activeColor.copy(alpha = 0.6f),
                shape = CutCornerShape(12.dp)
            )
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPressed) CarbonDark else activeColor,
            modifier = Modifier.size(26.dp)
        )
    }
}
