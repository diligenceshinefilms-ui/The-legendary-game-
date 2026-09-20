package com.example.game.engine

import com.example.core.audio.GameAudioManager
import com.example.core.model.*
import com.example.game.ai.OpponentAI
import com.example.game.physics.BikeInput
import com.example.game.physics.BikePhysics
import com.example.game.tracks.TrackCatalog
import com.example.game.tracks.TrackLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

class GameEngine(
    val track: Track,
    val playerBikeStats: EffectiveBikeStats,
    val totalLaps: Int = 999, // Continuous mode: race doesn't stop until user requests stop
    val mode: RaceMode = RaceMode.QUICK_RACE,
    val audioManager: GameAudioManager? = null,
    val playerLivery: PlayerLivery? = null
) {
    val trackLayout: TrackLayout = TrackCatalog.getTrackLayout(track.id)

    private val _engineState = MutableStateFlow(GameEngineState.COUNTDOWN)
    val engineState: StateFlow<GameEngineState> = _engineState.asStateFlow()

    private val _racers = MutableStateFlow<List<RacerState>>(emptyList())
    val racers: StateFlow<List<RacerState>> = _racers.asStateFlow()

    private val _playerRank = MutableStateFlow(1)
    val playerRank: StateFlow<Int> = _playerRank.asStateFlow()

    private val _playerLap = MutableStateFlow(1)
    val playerLap: StateFlow<Int> = _playerLap.asStateFlow()

    private val _nitroFuel = MutableStateFlow(100f)
    val nitroFuel: StateFlow<Float> = _nitroFuel.asStateFlow()

    private val _raceTimeMs = MutableStateFlow(0L)
    val raceTimeMs: StateFlow<Long> = _raceTimeMs.asStateFlow()

    private val _bestLapMs = MutableStateFlow(0L)
    val bestLapMs: StateFlow<Long> = _bestLapMs.asStateFlow()

    private val _currentLapTimeMs = MutableStateFlow(0L)
    val currentLapTimeMs: StateFlow<Long> = _currentLapTimeMs.asStateFlow()

    private val _topSpeedKmh = MutableStateFlow(0f)
    val topSpeedKmh: StateFlow<Float> = _topSpeedKmh.asStateFlow()

    private val _totalDistanceMeters = MutableStateFlow(0f)
    val totalDistanceMeters: StateFlow<Float> = _totalDistanceMeters.asStateFlow()

    // Authoritative player speed state controlled by Speed Up / Speed Down buttons (20 km/h steps)
    private val _playerSpeedKmh = MutableStateFlow(0f)
    val playerSpeedKmh: StateFlow<Float> = _playerSpeedKmh.asStateFlow()

    private val _countdownValue = MutableStateFlow(3)
    val countdownValue: StateFlow<Int> = _countdownValue.asStateFlow()

    private val _missionFailReason = MutableStateFlow<String?>(null)
    val missionFailReason: StateFlow<String?> = _missionFailReason.asStateFlow()

    private val _obstacles = MutableStateFlow<List<HighwayObstacle>>(emptyList())
    val obstacles: StateFlow<List<HighwayObstacle>> = _obstacles.asStateFlow()

    private val _trainAlertActive = MutableStateFlow(false)
    val trainAlertActive: StateFlow<Boolean> = _trainAlertActive.asStateFlow()

    private val _nearMissBonus = MutableStateFlow<String?>(null)
    val nearMissBonus: StateFlow<String?> = _nearMissBonus.asStateFlow()

    private val _nearMissCombo = MutableStateFlow(0)
    val nearMissCombo: StateFlow<Int> = _nearMissCombo.asStateFlow()

    private val _isDrafting = MutableStateFlow(false)
    val isDrafting: StateFlow<Boolean> = _isDrafting.asStateFlow()

    private val _draftIntensity = MutableStateFlow(0f)
    val draftIntensity: StateFlow<Float> = _draftIntensity.asStateFlow()

    var activeBikeStats: EffectiveBikeStats = playerBikeStats
        private set

    private var countdownTimer = 3.99f
    private var lastCountdownFloor = 3
    private var isNitroEngaged = false
    private val aiControllers = mutableListOf<OpponentAI>()
    private var currentLapStartTime = 0L
    private var lastNearMissTimestamp = 0L
    private var nearMissComboCount = 0

    // 30-Second Rolling Input & Telemetry Buffer for Post-Race Replay
    private val recordedReplayFrames = ArrayDeque<ReplayFrame>()

    fun getRecordedReplayFrames(): List<ReplayFrame> = synchronized(recordedReplayFrames) {
        recordedReplayFrames.toList()
    }

    var playerInput = BikeInput()

    init {
        initializeGrid()
        initializeObstacles()
        // Start procedural engine sound synthesizer at idle
        audioManager?.startEngineAudio()
        audioManager?.updateEngineAudio(
            speedKmh = 0f,
            gearNumber = 0,
            rpm = 1200,
            isThrottle = false,
            isBrake = false,
            isNitro = false,
            isRedlining = false
        )
    }

    private fun initializeObstacles() {
        val list = mutableListOf<HighwayObstacle>()
        val oncomingLanes = listOf(-0.72f, -0.44f, -0.16f)
        val sameDirLanes = listOf(0.16f, 0.44f, 0.72f)
        val allLanes = listOf(-0.72f, -0.44f, -0.16f, 0.16f, 0.44f, 0.72f)
        val colors = listOf(
            0xFFFFD600L, 0xFF00E5FFL, 0xFFFF1744L, 0xFF7C4DFFL,
            0xFF00E676L, 0xFFFF9100L, 0xFFECEFF1L, 0xFF37474FL,
            0xFFE91E63L, 0xFF00B0FFL, 0xFFFF6D00L, 0xFF76FF03L
        )
        
        // Spawn 32 varied vehicles across oncoming and forward highway lanes
        for (i in 0 until 32) {
            val dist = 90f + (i * 85f)
            // Approximately 40% of vehicles are oncoming in the opposite direction on the left highway lanes
            val isOncoming = (i % 5 == 0 || i % 5 == 2) && (i % 9 != 7)
            val lane = if (isOncoming) {
                oncomingLanes[i % oncomingLanes.size]
            } else {
                if (i % 9 == 7) 0f else sameDirLanes[i % sameDirLanes.size]
            }

            val type = when (i % 9) {
                0 -> ObstacleType.FOUR_WHEELER
                1 -> ObstacleType.TWO_WHEELER
                2 -> ObstacleType.TRUCK
                3 -> ObstacleType.TEMPO
                4 -> ObstacleType.TRAFFIC_TAXI
                5 -> ObstacleType.TRAFFIC_CAR
                6 -> ObstacleType.FOUR_WHEELER
                7 -> if (i % 18 == 7) ObstacleType.TRAIN_CROSSING else ObstacleType.TRUCK
                else -> ObstacleType.ROAD_CONE
            }
            
            val speed = when (type) {
                ObstacleType.TWO_WHEELER -> 75f + ((i % 3) * 12f)
                ObstacleType.FOUR_WHEELER -> 110f + ((i % 3) * 15f)
                ObstacleType.TRAFFIC_CAR -> 105f + ((i % 2) * 15f)
                ObstacleType.TRAFFIC_TAXI -> 100f + ((i % 2) * 12f)
                ObstacleType.TRUCK -> 70f + ((i % 2) * 10f)
                ObstacleType.TEMPO -> 60f + ((i % 2) * 10f)
                ObstacleType.TRAIN_CROSSING, ObstacleType.ROAD_CONE, ObstacleType.BARRIER, ObstacleType.OIL_SPILL, ObstacleType.DEBRIS -> 0f
            }

            list.add(
                HighwayObstacle(
                    id = "obs_$i",
                    type = type,
                    lane = if (type == ObstacleType.TRAIN_CROSSING) 0f else lane,
                    distanceMeters = dist,
                    speedKmh = speed,
                    primaryColorHex = colors[i % colors.size],
                    vehicleSubModel = i % 3,
                    isOncoming = isOncoming
                )
            )
        }
        _obstacles.value = list
    }

    private fun initializeGrid() {
        aiControllers.clear()
        val waypoints = trackLayout.waypoints
        val startWp = waypoints[0]
        val nextWp = waypoints[1]
        val startDir = (nextWp - startWp).normalized()
        val startAngle = atan2(startDir.y, startDir.x)
        val normal = Vector2D(-startDir.y, startDir.x)

        val grid = mutableListOf<RacerState>()
        val opponentNames = listOf("Vortex", "GhostRider", "Blaze", "Titan", "Phantom")
        val opponentColors = listOf(
            Pair(0xFFFF6B00L, 0xFFFFD600L),
            Pair(0xFFB5179EL, 0xFF00F0FFL),
            Pair(0xFFFF1744L, 0xFFFFFFFFL),
            Pair(0xFF00E676L, 0xFFFF6B00L),
            Pair(0xFFFFD600L, 0xFF00F0FFL)
        )

        // Player starting dead center on highway lane
        val pColor = playerLivery?.primaryColorHex ?: playerBikeStats.bike.primaryColorHex
        val sColor = playerLivery?.secondaryColorHex ?: playerBikeStats.bike.secondaryColorHex
        val uColor = playerLivery?.underglowColorHex ?: pColor
        val num = playerLivery?.racingNumber ?: "46"
        val decal = playerLivery?.vinylAccentStyle?.name ?: "CYBER_FLAMES"
        val rimTape = playerLivery?.rimTapeColorHex ?: pColor

        grid.add(
            RacerState(
                id = "player",
                name = "You",
                isPlayer = true,
                posX = 0.0f,
                posY = 0.0f,
                totalRaceDistance = 0.0f,
                angleRad = startAngle,
                primaryColorHex = pColor,
                secondaryColorHex = sColor,
                underglowColorHex = uColor,
                racingNumber = num,
                vinylAccentStyle = decal,
                rimTapeColorHex = rimTape,
                bikeName = playerBikeStats.bike.name
            )
        )

        // 5 AI Opponents staggered along highway lanes
        val oppLanes = listOf(-0.45f, 0.45f, -0.25f, 0.25f, -0.65f)
        for (i in 0 until 5) {
            val oppId = "opp_$i"
            val colors = opponentColors[i % opponentColors.size]
            val lane = oppLanes[i % oppLanes.size]
            val distanceAhead = (i + 1) * 32f

            grid.add(
                RacerState(
                    id = oppId,
                    name = opponentNames[i % opponentNames.size],
                    isPlayer = false,
                    posX = lane,
                    posY = distanceAhead,
                    totalRaceDistance = distanceAhead,
                    angleRad = startAngle,
                    primaryColorHex = colors.first,
                    secondaryColorHex = colors.second,
                    bikeName = "Legend Opponent"
                )
            )
            aiControllers.add(OpponentAI(oppId, track.difficulty))
        }

        _racers.value = grid
    }

    fun update(dtSeconds: Float) {
        val state = _engineState.value
        if (state == GameEngineState.PAUSED || state == GameEngineState.FINISHED || state == GameEngineState.RESULTS) return

        if (state == GameEngineState.COUNTDOWN) {
            countdownTimer -= dtSeconds
            val floorVal = countdownTimer.toInt().coerceAtLeast(0)
            _countdownValue.value = floorVal

            if (floorVal != lastCountdownFloor && floorVal in 1..3) {
                lastCountdownFloor = floorVal
                audioManager?.playCountdownBeep(isFinal = false)
            }

            // Revving engine sound during countdown throttle rev
            val isRevving = playerInput.throttle > 0.05f
            val idleRpm = if (isRevving) 5500 else 1400
            audioManager?.updateEngineAudio(
                speedKmh = 0f,
                gearNumber = if (isRevving) 1 else 0,
                rpm = idleRpm,
                isThrottle = isRevving,
                isBrake = false,
                isNitro = false,
                isRedlining = false
            )

            if (countdownTimer <= 0.0f) {
                _engineState.value = GameEngineState.RACING
                audioManager?.playCountdownBeep(isFinal = true)
                currentLapStartTime = System.currentTimeMillis()
            }
            return
        }

        if (state == GameEngineState.RACING) {
            val dt = dtSeconds.coerceIn(0.001f, 0.05f)
            val currentRacers = _racers.value.toMutableList()
            val playerRacer = currentRacers.firstOrNull { it.isPlayer }

            // 1. Update Nitro fuel
            if (playerInput.nitro && _nitroFuel.value > 1.0f) {
                val drainRate = 100f / playerBikeStats.nitroDurationSec
                _nitroFuel.value = (_nitroFuel.value - drainRate * dt).coerceAtLeast(0f)
                if (!isNitroEngaged) {
                    isNitroEngaged = true
                    audioManager?.playNitroBoost()
                }
            } else {
                isNitroEngaged = false
                // Slow passive refill when not in use
                _nitroFuel.value = (_nitroFuel.value + 4f * dt).coerceAtMost(100f)
            }

            // 1.1 Compute Dynamic Slipstream Vacuum Drafting Force
            var maxDraftIntensity = 0f
            if (playerRacer != null && playerRacer.speedKmh > 50f) {
                val pDist = playerRacer.totalRaceDistance
                val pLane = playerRacer.posX

                // Check lead traffic obstacles within 4m to 32m ahead
                for (obs in _obstacles.value) {
                    if (obs.isHit) continue
                    val longDiff = obs.distanceMeters - pDist
                    val latDiff = abs(pLane - obs.lane)
                    if (longDiff in 4.0f..32.0f && latDiff <= 0.18f) {
                        val proximityFactor = ((32.0f - longDiff) / (32.0f - 4.0f)).coerceIn(0f, 1f)
                        val alignFactor = (1.0f - (latDiff / 0.18f)).coerceIn(0f, 1f)
                        val intensity = proximityFactor * alignFactor
                        if (intensity > maxDraftIntensity) {
                            maxDraftIntensity = intensity
                        }
                    }
                }

                // Check lead opponent racers within 4m to 34m ahead
                for (opp in currentRacers.filter { !it.isPlayer && it.finishedTimeMs == null }) {
                    val longDiff = opp.totalRaceDistance - pDist
                    val latDiff = abs(pLane - opp.posX)
                    if (longDiff in 4.0f..34.0f && latDiff <= 0.18f) {
                        val proximityFactor = ((34.0f - longDiff) / (34.0f - 4.0f)).coerceIn(0f, 1f)
                        val alignFactor = (1.0f - (latDiff / 0.18f)).coerceIn(0f, 1f)
                        val intensity = proximityFactor * alignFactor
                        if (intensity > maxDraftIntensity) {
                            maxDraftIntensity = intensity
                        }
                    }
                }
            }

            val wasDrafting = _isDrafting.value
            if (maxDraftIntensity > 0.05f) {
                if (!wasDrafting) {
                    audioManager?.playDraftingLockOn()
                }
                _isDrafting.value = true
                _draftIntensity.value = maxDraftIntensity
                // Suction vacuum acceleration boost and aerodynamic nitro recovery
                val draftSpeedBoost = 18f * maxDraftIntensity * dt
                _playerSpeedKmh.value = (_playerSpeedKmh.value + draftSpeedBoost).coerceAtMost(playerBikeStats.topSpeedKmh * 1.12f)
                _nitroFuel.value = (_nitroFuel.value + (14f * maxDraftIntensity * dt)).coerceAtMost(100f)
            } else {
                _isDrafting.value = false
                _draftIntensity.value = 0f
            }

            // 2. Physics & AI updates for all racers
            val basePlayerSpeed = _playerSpeedKmh.value
            val effectivePlayerSpeed = if (isNitroEngaged) {
                (basePlayerSpeed + 35f).coerceAtMost(playerBikeStats.topSpeedKmh * playerBikeStats.nitroMultiplier)
            } else if (_isDrafting.value) {
                (basePlayerSpeed + (16f * _draftIntensity.value)).coerceAtMost(playerBikeStats.topSpeedKmh * 1.12f)
            } else {
                basePlayerSpeed
            }

            var updatedRacers = currentRacers.map { racer ->
                if (racer.finishedTimeMs != null) {
                    // Decelerate safely
                    racer.copy(speedKmh = (racer.speedKmh - 30f * dt).coerceAtLeast(0f))
                } else if (racer.isPlayer) {
                    val inputWithNitro = playerInput.copy(nitro = isNitroEngaged)
                    val playerToUpdate = racer.copy(
                        speedKmh = effectivePlayerSpeed,
                        isNitroActive = isNitroEngaged
                    )
                    val updated = BikePhysics.updateRacer(playerToUpdate, inputWithNitro, playerBikeStats, trackLayout, dt)

                    // Record frame for 30-second post-race playback
                    val currentMs = System.currentTimeMillis()
                    synchronized(recordedReplayFrames) {
                        recordedReplayFrames.add(
                            ReplayFrame(
                                timestampMs = currentMs,
                                steer = inputWithNitro.steer,
                                throttle = inputWithNitro.throttle,
                                brake = inputWithNitro.brake,
                                nitro = isNitroEngaged,
                                posX = updated.posX,
                                totalRaceDistance = updated.totalRaceDistance,
                                speedKmh = updated.speedKmh,
                                leanAngleRad = updated.leanAngleRad
                            )
                        )
                        while (recordedReplayFrames.isNotEmpty() && (currentMs - recordedReplayFrames.first().timestampMs > 30_000L)) {
                            recordedReplayFrames.removeFirst()
                        }
                    }

                    // Track Top Speed and Total Distance
                    if (updated.speedKmh > _topSpeedKmh.value) {
                        _topSpeedKmh.value = updated.speedKmh
                    }
                    _totalDistanceMeters.value = updated.totalRaceDistance

                    // Real-Time Dynamic Sound Effect: Pitch, Volume, RPM & Gear Integration
                    val telemetry = TelemetryCalculator.calculate(updated.speedKmh, isNitroEngaged)
                    audioManager?.updateEngineAudio(
                        speedKmh = updated.speedKmh,
                        gearNumber = telemetry.gearNumber,
                        rpm = telemetry.rpm,
                        isThrottle = updated.speedKmh > 0f,
                        isBrake = false,
                        isNitro = isNitroEngaged,
                        isRedlining = telemetry.isRedlining
                    )

                    if (telemetry.isRedlining) {
                        audioManager?.triggerRedlineHaptic()
                    }

                    updated
                } else {
                    val ai = aiControllers.firstOrNull { it.racerId == racer.id }
                    val aiInput = ai?.computeInput(racer, playerBikeStats, trackLayout, playerRacer, dt)
                        ?: BikeInput(throttle = 0.8f)
                    BikePhysics.updateRacer(racer, aiInput, playerBikeStats, trackLayout, dt)
                }
            }

            // 2.1 Update moving obstacles along highway
            var pRacer = updatedRacers.firstOrNull { it.isPlayer }
            if (pRacer != null) {
                val currentObs = _obstacles.value
                val highwayLanes = listOf(-0.72f, -0.44f, -0.16f, 0.16f, 0.44f, 0.72f)
                var isNearTrainCrossing = false
                val now = System.currentTimeMillis()

                // Combo timeout check (combo resets after 6 seconds of no near-miss passes)
                if (nearMissComboCount > 0 && now - lastNearMissTimestamp > 6000L) {
                    nearMissComboCount = 0
                    _nearMissCombo.value = 0
                }

                var updatedObs = currentObs.map { obs ->
                    if (obs.type == ObstacleType.TRAIN_CROSSING) {
                        val relDist = obs.distanceMeters - pRacer!!.totalRaceDistance
                        if (relDist in -40f..350f) {
                            isNearTrainCrossing = true
                        }
                        // Animate train progress when player is within 400m
                        val newProgress = if (relDist in -60f..400f) {
                            (obs.trainProgress + dt * 0.35f) % 1.0f
                        } else {
                            obs.trainProgress
                        }
                        var newDist = obs.distanceMeters
                        if (newDist < pRacer!!.totalRaceDistance - 80f) {
                            newDist = pRacer!!.totalRaceDistance + 800f + (kotlin.random.Random.nextFloat() * 600f)
                        }
                        obs.copy(distanceMeters = newDist, trainProgress = newProgress)
                    } else {
                        var newDist = if (obs.isOncoming) {
                            // Oncoming traffic travels in the opposite direction towards the player
                            obs.distanceMeters - ((obs.speedKmh * 1000f / 3600f) * dt)
                        } else {
                            // Forward traffic travels along with the player
                            obs.distanceMeters + ((obs.speedKmh * 1000f / 3600f) * dt)
                        }
                        var newIsHit = obs.isHit
                        var newLane = obs.lane
                        // If honked at, smoothly steer toward outer road shoulder
                        if (obs.isHonked) {
                            val targetLane = if (obs.lane >= 0f) 0.72f else -0.72f
                            newLane += (targetLane - obs.lane) * (dt * 3.5f)
                        }
                        // Dynamic Highway Traffic Recycling: Perpetual stream of traffic ahead
                        if (newDist < pRacer!!.totalRaceDistance - 50f) {
                            if (obs.isOncoming) {
                                newDist = pRacer!!.totalRaceDistance + 280f + (kotlin.random.Random.nextFloat() * 240f)
                                val oncomingLanes = listOf(-0.72f, -0.44f, -0.16f)
                                newLane = oncomingLanes[kotlin.random.Random.nextInt(oncomingLanes.size)]
                            } else {
                                newDist = pRacer!!.totalRaceDistance + 220f + (kotlin.random.Random.nextFloat() * 200f)
                                val sameDirLanes = listOf(0.16f, 0.44f, 0.72f)
                                newLane = sameDirLanes[kotlin.random.Random.nextInt(sameDirLanes.size)]
                            }
                            newIsHit = false // Reset when wrapping around
                        }
                        obs.copy(distanceMeters = newDist, isHit = newIsHit, lane = newLane)
                    }
                }
                _trainAlertActive.value = isNearTrainCrossing

                // 2.2 Collision Check & Intense Close-Call Overtaking Mechanics
                val playerLatPos = pRacer!!.posX
                val hitObstacleIds = mutableSetOf<String>()

                for (obs in updatedObs) {
                    if (obs.isHit) continue // Skip already hit obstacles
                    val longDiff = abs(pRacer!!.totalRaceDistance - obs.distanceMeters)
                    val latDiff = abs(playerLatPos - obs.lane)

                    if (obs.type == ObstacleType.TRAIN_CROSSING) {
                        // Train crossing collision when train is passing through
                        if (longDiff < 9.0f && obs.trainProgress in 0.20f..0.80f && pRacer!!.speedKmh > 15f) {
                            _missionFailReason.value = "COLLISION AT RAILWAY CROSSING: IMPACT WITH PASSING HIGH-SPEED FREIGHT TRAIN!"
                            _engineState.value = GameEngineState.MISSION_FAILED
                            audioManager?.playCrash()
                            audioManager?.stopEngineAudio()
                            return
                        }
                    } else {
                        // Dynamic Close-Call Overtaking & Lane-Splitting Detection
                        if (longDiff < 4.8f && latDiff in 0.10f..0.32f && pRacer!!.speedKmh > 45f && now - lastNearMissTimestamp > 400L) {
                            lastNearMissTimestamp = now
                            // Check if player is threading between two traffic vehicles in adjacent lanes (Lane-Splitting)
                            val isLaneSplit = updatedObs.any { other ->
                                other.id != obs.id &&
                                abs(pRacer!!.totalRaceDistance - other.distanceMeters) < 6.0f &&
                                abs(playerLatPos - other.lane) in 0.10f..0.34f &&
                                ((playerLatPos - obs.lane) * (playerLatPos - other.lane) < 0f)
                            }

                            val vehicleLabel = when (obs.type) {
                                ObstacleType.TWO_WHEELER -> "TWO WHEELER"
                                ObstacleType.TRUCK -> "HEAVY TRUCK"
                                ObstacleType.TEMPO -> "TEMPO"
                                ObstacleType.TRAFFIC_TAXI -> "TAXI"
                                else -> "TRAFFIC"
                            }

                            if (obs.isOncoming) {
                                nearMissComboCount = (nearMissComboCount + 2).coerceAtMost(30)
                                _nearMissBonus.value = "⚡ DARING ONCOMING PASS: $vehicleLabel! COMBO x$nearMissComboCount (+180)"
                                _nitroFuel.value = (_nitroFuel.value + 24f).coerceAtMost(100f)
                            } else if (isLaneSplit) {
                                nearMissComboCount = (nearMissComboCount + 2).coerceAtMost(25)
                                _nearMissBonus.value = "🔥 SPLIT-SECOND DOUBLE OVERTAKE! COMBO x$nearMissComboCount (+250)"
                                _nitroFuel.value = (_nitroFuel.value + 28f).coerceAtMost(100f)
                            } else {
                                nearMissComboCount = (nearMissComboCount + 1).coerceAtMost(25)
                                _nearMissBonus.value = "⚡ CLOSE CALL PASS: $vehicleLabel! COMBO x$nearMissComboCount (+100)"
                                _nitroFuel.value = (_nitroFuel.value + 16f).coerceAtMost(100f)
                            }
                            _nearMissCombo.value = nearMissComboCount
                            audioManager?.playNearMissOvertake(isDoubleSplit = isLaneSplit || obs.isOncoming)
                        }

                        // Physical collision detection with enlarged vehicle hitboxes
                        val collisionRadiusLong = when (obs.type) {
                            ObstacleType.TRUCK -> 9.8f
                            ObstacleType.FOUR_WHEELER, ObstacleType.TRAFFIC_CAR, ObstacleType.TRAFFIC_TAXI -> 6.6f
                            ObstacleType.TEMPO -> 5.8f
                            ObstacleType.TWO_WHEELER -> 4.8f
                            ObstacleType.BARRIER -> 4.5f
                            ObstacleType.ROAD_CONE -> 3.5f
                            else -> 5.5f
                        }
                        val collisionRadiusLat = when (obs.type) {
                            ObstacleType.TRUCK -> 0.28f
                            ObstacleType.FOUR_WHEELER, ObstacleType.TRAFFIC_CAR, ObstacleType.TRAFFIC_TAXI -> 0.24f
                            ObstacleType.TEMPO -> 0.21f
                            ObstacleType.TWO_WHEELER -> 0.17f
                            ObstacleType.BARRIER -> 0.32f
                            ObstacleType.ROAD_CONE -> 0.18f
                            else -> 0.20f
                        }

                        if (longDiff < collisionRadiusLong && latDiff < collisionRadiusLat && pRacer!!.speedKmh > 10f) {
                            if (obs.type == ObstacleType.OIL_SPILL) {
                                pRacer = pRacer!!.copy(speedKmh = (pRacer!!.speedKmh * 0.70f).coerceAtLeast(0f))
                                audioManager?.playCollision(isScrape = true)
                                hitObstacleIds.add(obs.id)
                            } else if (obs.type == ObstacleType.DEBRIS) {
                                pRacer = pRacer!!.copy(speedKmh = (pRacer!!.speedKmh * 0.40f).coerceAtLeast(0f))
                                audioManager?.playCollision(isScrape = true)
                                hitObstacleIds.add(obs.id)
                            } else {
                                val typeName = when (obs.type) {
                                    ObstacleType.TWO_WHEELER -> "TWO-WHEELER COMMUTER"
                                    ObstacleType.TRUCK -> "18-WHEELER FREIGHT TRUCK"
                                    ObstacleType.TEMPO -> "DELIVERY TEMPO"
                                    ObstacleType.FOUR_WHEELER, ObstacleType.TRAFFIC_CAR -> "HIGHWAY VEHICLE"
                                    ObstacleType.TRAFFIC_TAXI -> "CITY TAXI"
                                    ObstacleType.BARRIER -> "ROADWORK BARRIER"
                                    ObstacleType.ROAD_CONE -> "TRAFFIC SAFETY PYLON"
                                    else -> obs.type.name.replace("_", " ")
                                }
                                if (obs.isOncoming) {
                                    val combinedSpeed = (pRacer!!.speedKmh + obs.speedKmh).toInt()
                                    _missionFailReason.value = "HEAD-ON COLLISION: IMPACT WITH ONCOMING $typeName AT $combinedSpeed KM/H COMBINED VELOCITY!"
                                } else {
                                    _missionFailReason.value = "CRITICAL COLLISION: HIT $typeName AT ${pRacer!!.speedKmh.toInt()} KM/H!"
                                }
                                _engineState.value = GameEngineState.MISSION_FAILED
                                audioManager?.playCrash()
                                audioManager?.stopEngineAudio()
                                return
                            }
                        }
                    }
                }
                
                if (hitObstacleIds.isNotEmpty()) {
                    updatedObs = updatedObs.map { if (it.id in hitObstacleIds) it.copy(isHit = true) else it }
                }
                _obstacles.value = updatedObs
                
                // Update player racer in the list if modified
                updatedRacers = updatedRacers.map { if (it.isPlayer) pRacer!! else it }

                // B. Check Roadside Barrier & Guardrails / Trees / Buildings
                if (abs(playerLatPos) > 0.96f && pRacer!!.speedKmh > 25f) {
                    _missionFailReason.value = "CRITICAL IMPACT: CRASHED INTO ROADSIDE BARRIER & STRUCTURES AT ${pRacer!!.speedKmh.toInt()} KM/H!"
                    _engineState.value = GameEngineState.MISSION_FAILED
                    audioManager?.playCrash()
                    audioManager?.stopEngineAudio()
                    return
                }

                // C. Check High-Speed Racer & Opponent Vehicle Collision
                for (opp in updatedRacers.filter { !it.isPlayer && it.finishedTimeMs == null }) {
                    val oppLongDiff = abs(pRacer!!.totalRaceDistance - opp.totalRaceDistance)
                    val oppLatDiff = abs(pRacer!!.posX - opp.posX)
                    if (oppLongDiff < 5.0f && oppLatDiff < 0.22f && pRacer!!.speedKmh > 15f) {
                        _missionFailReason.value = "CRITICAL COLLISION: IMPACT WITH RIVAL RACER ${opp.name} AT ${pRacer!!.speedKmh.toInt()} KM/H!"
                        _engineState.value = GameEngineState.MISSION_FAILED
                        audioManager?.playCrash()
                        audioManager?.stopEngineAudio()
                        return
                    }
                }
            }

            // 3. Collision Resolution
            val collidedRacers = BikePhysics.handleRacerCollisions(updatedRacers)

            // 4. Checkpoint & Lap Detection (Continuous Racing Loop)
            val gates = trackLayout.checkpointGates
            val finalRacers = collidedRacers.map { racer ->
                if (racer.finishedTimeMs != null) return@map racer

                val currentPos = Vector2D(racer.posX, racer.posY)
                val targetGate = gates[racer.nextCheckpointIndex]

                val distToGate = currentPos.distanceTo(targetGate.center)
                if (distToGate < (trackLayout.roadWidth * 0.9f)) {
                    val nextCp = (racer.nextCheckpointIndex + 1) % gates.size
                    var newLap = racer.currentLap

                    if (targetGate.isFinishLine && racer.nextCheckpointIndex == 0) {
                        // Lap completed
                        newLap++
                        if (racer.isPlayer) {
                            audioManager?.playCheckpoint()
                            val now = System.currentTimeMillis()
                            val lapDuration = now - currentLapStartTime
                            currentLapStartTime = now
                            if (_bestLapMs.value == 0L || lapDuration < _bestLapMs.value) {
                                _bestLapMs.value = lapDuration
                            }
                            _playerLap.value = newLap
                        }
                    } else if (racer.isPlayer) {
                        audioManager?.playCheckpoint()
                    }

                    racer.copy(
                        currentLap = newLap,
                        nextCheckpointIndex = nextCp
                    )
                } else {
                    racer
                }
            }

            // 5. Compute Real-Time Position / Leaderboard Ranks
            val sortedByProgress = finalRacers.sortedByDescending { r ->
                (r.currentLap * 100000L) + (r.nextCheckpointIndex * 1000L) + (r.totalRaceDistance.toLong() % 1000L)
            }

            val pRank = sortedByProgress.indexOfFirst { it.isPlayer } + 1
            _playerRank.value = pRank.coerceAtLeast(1)

            _racers.value = finalRacers
            _raceTimeMs.value += (dt * 1000).toLong()
            _currentLapTimeMs.value = (System.currentTimeMillis() - currentLapStartTime).coerceAtLeast(0L)
        }
    }

    /**
     * Stops the race upon user request and marks the race as finished.
     */
    fun stopRace() {
        if (_engineState.value == GameEngineState.RACING || _engineState.value == GameEngineState.PAUSED) {
            _engineState.value = GameEngineState.FINISHED
            audioManager?.stopEngineAudio()
            audioManager?.playVictory()
        }
    }

    fun pause() {
        if (_engineState.value == GameEngineState.RACING) {
            _engineState.value = GameEngineState.PAUSED
            audioManager?.pauseEngineAudio()
        }
    }

    fun resume() {
        if (_engineState.value == GameEngineState.PAUSED) {
            _engineState.value = GameEngineState.RACING
            audioManager?.resumeEngineAudio()
        }
    }

    /**
     * Honks motorcycle horn at oncoming traffic.
     * Vehicles in the player's path will immediately signal and move over to the shoulder!
     */
    fun honk() {
        audioManager?.playCountdownBeep(isFinal = false)
        val pRacer = _racers.value.firstOrNull { it.isPlayer } ?: return
        val currentObs = _obstacles.value
        val playerSteerFraction = (pRacer.leanAngleRad * 1.8f).coerceIn(-1f, 1f)
        
        _obstacles.value = currentObs.map { obs ->
            val relDist = obs.distanceMeters - pRacer.totalRaceDistance
            val latDiff = abs(playerSteerFraction - obs.lane)
            if (relDist in 5f..160f && latDiff < 0.45f) {
                obs.copy(isHonked = true)
            } else {
                obs
            }
        }
    }

    /**
     * Changes active player bike on-the-fly during gameplay using EffectiveBikeStats.
     */
    fun changePlayerBike(newStats: EffectiveBikeStats) {
        activeBikeStats = newStats
        _racers.value = _racers.value.map { racer ->
            if (racer.isPlayer) {
                racer.copy(
                    bikeName = newStats.bike.name,
                    primaryColorHex = playerLivery?.primaryColorHex ?: newStats.bike.primaryColorHex,
                    secondaryColorHex = playerLivery?.secondaryColorHex ?: newStats.bike.secondaryColorHex,
                    underglowColorHex = playerLivery?.underglowColorHex ?: newStats.bike.primaryColorHex,
                    racingNumber = playerLivery?.racingNumber ?: "46",
                    vinylAccentStyle = playerLivery?.vinylAccentStyle?.name ?: "CYBER_FLAMES",
                    rimTapeColorHex = playerLivery?.rimTapeColorHex ?: newStats.bike.primaryColorHex
                )
            } else {
                racer
            }
        }
        // Clamp current speed if it exceeds new bike top speed
        if (_playerSpeedKmh.value > newStats.topSpeedKmh) {
            setPlayerSpeed(newStats.topSpeedKmh)
        }
    }

    /**
     * Updates player livery in real-time.
     */
    fun updatePlayerLivery(livery: PlayerLivery) {
        _racers.value = _racers.value.map { racer ->
            if (racer.isPlayer) {
                racer.copy(
                    primaryColorHex = livery.primaryColorHex,
                    secondaryColorHex = livery.secondaryColorHex,
                    underglowColorHex = livery.underglowColorHex,
                    racingNumber = livery.racingNumber,
                    vinylAccentStyle = livery.vinylAccentStyle.name,
                    rimTapeColorHex = livery.rimTapeColorHex
                )
            } else {
                racer
            }
        }
    }

    /**
     * Overloaded helper to switch bikes directly on-the-fly with custom parameters.
     */
    fun changePlayerBike(
        bikeId: String,
        bikeName: String,
        maxSpeed: Float,
        primaryColorHex: Long,
        secondaryColorHex: Long,
        handlingMultiplier: Float = 1.0f,
        accelMultiplier: Float = 1.0f
    ) {
        val customBike = Bike(
            id = bikeId,
            name = bikeName,
            description = "High-performance racing bike custom tuned for highway speed.",
            baseTopSpeed = maxSpeed.toInt(),
            baseAcceleration = (accelMultiplier * 85).toInt().coerceIn(10, 100),
            baseHandling = (handlingMultiplier * 85).toInt().coerceIn(10, 100),
            baseBraking = 85,
            baseNitro = 90,
            price = 0L,
            unlockLevel = 1,
            primaryColorHex = primaryColorHex,
            secondaryColorHex = secondaryColorHex
        )
        val customPlayerBike = PlayerBike(
            id = "pb_$bikeId",
            playerId = "player_1",
            bikeId = bikeId,
            unlocked = true
        )
        val customStats = EffectiveBikeStats(
            bike = customBike,
            topSpeedKmh = maxSpeed,
            acceleration = accelMultiplier,
            handling = handlingMultiplier,
            braking = 1.0f,
            nitroMultiplier = 1.4f,
            nitroDurationSec = 4.0f,
            upgradeLevels = customPlayerBike
        )
        changePlayerBike(customStats)
    }

    fun clearNearMissBonus() {
        _nearMissBonus.value = null
    }

    /**
     * Deterministic speed controls:
     * - First press -> 20 km/h
     * - Second press -> 40 km/h
     * - Third -> 60 km/h
     * - Fourth -> 80 km/h
     * - Increases in increments of 20 up to configured maximum speed.
     */
    fun speedUp() {
        if (_engineState.value == GameEngineState.COUNTDOWN) {
            // Start race immediately on Speed Up press
            _engineState.value = GameEngineState.RACING
            countdownTimer = 0f
            _countdownValue.value = 0
            audioManager?.playCountdownBeep(isFinal = true)
            currentLapStartTime = System.currentTimeMillis()
        }
        val current = _playerSpeedKmh.value
        val maxSpeed = activeBikeStats.topSpeedKmh
        val currentStep = (current / 20f).roundToInt()
        val nextSpeed = ((currentStep + 1) * 20f).coerceAtMost(maxSpeed)
        setPlayerSpeed(nextSpeed)
    }

    /**
     * Deterministic speed-down controls:
     * - Decreases by 20 km/h per press: 80 -> 60 -> 40 -> 20 -> 0.
     * - Never goes below 0.
     */
    fun speedDown() {
        val current = _playerSpeedKmh.value
        val currentStep = (current / 20f).roundToInt()
        val nextSpeed = ((currentStep - 1) * 20f).coerceAtLeast(0f)
        setPlayerSpeed(nextSpeed)
    }

    /**
     * Sets authoritative player speed immediately.
     */
    fun setPlayerSpeed(targetSpeed: Float) {
        val maxSpeed = activeBikeStats.topSpeedKmh
        val clamped = targetSpeed.coerceIn(0f, maxSpeed)
        _playerSpeedKmh.value = clamped

        // Immediately update player in racers list
        _racers.value = _racers.value.map { racer ->
            if (racer.isPlayer) {
                racer.copy(
                    speedKmh = clamped,
                    throttleRatio = if (clamped > 0f) (clamped / maxSpeed).coerceIn(0.2f, 1f) else 0f
                )
            } else {
                racer
            }
        }

        if (clamped > _topSpeedKmh.value) {
            _topSpeedKmh.value = clamped
        }

        // Immediately update dynamic engine audio
        val telemetry = TelemetryCalculator.calculate(clamped, isNitroEngaged)
        audioManager?.updateEngineAudio(
            speedKmh = clamped,
            gearNumber = telemetry.gearNumber,
            rpm = telemetry.rpm,
            isThrottle = clamped > 0f,
            isBrake = false,
            isNitro = isNitroEngaged,
            isRedlining = telemetry.isRedlining
        )
    }

    fun restart() {
        countdownTimer = 3.99f
        lastCountdownFloor = 3
        _missionFailReason.value = null
        _engineState.value = GameEngineState.COUNTDOWN
        _raceTimeMs.value = 0L
        _currentLapTimeMs.value = 0L
        _bestLapMs.value = 0L
        _topSpeedKmh.value = 0f
        _totalDistanceMeters.value = 0f
        _playerSpeedKmh.value = 0f
        _playerLap.value = 1
        _playerRank.value = 1
        _nitroFuel.value = 100f
        audioManager?.startEngineAudio()
        initializeGrid()
        initializeObstacles()
    }
}
