package com.example.game.renderer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.core.model.HighwayObstacle
import com.example.core.model.ObstacleType
import com.example.core.model.RacerState
import com.example.game.environment.CityMilestone
import com.example.game.environment.CityMilestoneCatalog
import com.example.game.tracks.TrackLayout
import com.example.ui.theme.*
import kotlin.math.*
import kotlin.random.Random

data class VisualCanvasParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val maxLife: Float,
    var currentLife: Float,
    var size: Float,
    val type: ParticleVisualType,
    var rotation: Float = 0f,
    val rotationSpeed: Float = 0f
)

enum class ParticleVisualType {
    SPARK,
    SLIPSTREAM_STREAK,
    COMBO_STAR,
    SMOKE_PUFF,
    DEBRIS_CHUNK,
    SHOCKWAVE_RING
}

@Composable
fun GameCanvasRenderer(
    track: TrackLayout,
    racers: List<RacerState>,
    obstacles: List<HighwayObstacle> = emptyList(),
    nearMissBonus: String? = null,
    nearMissCombo: Int = 0,
    isFirstPersonCam: Boolean = false,
    modifier: Modifier = Modifier
) {
    val player = racers.firstOrNull { it.isPlayer } ?: racers.firstOrNull()

    // Smooth continuous animation ticker for road scrolling, scenery, and parallax
    var roadScrollAccumulator by remember { mutableFloatStateOf(0f) }
    var continuousDistance by remember { mutableFloatStateOf(0f) }
    var engineRumbleTimer by remember { mutableFloatStateOf(0f) }

    // Visual Particle Engine State
    val particles = remember { mutableStateListOf<VisualCanvasParticle>() }
    var prevHitIds by remember { mutableStateOf(setOf<String>()) }
    var lastSlipstreamSpawnTime by remember { mutableLongStateOf(0L) }

    // Track recently overtaken bikes for dynamic "OVERTAKE" pop-up alerts
    var lastOvertakeTime by remember { mutableLongStateOf(0L) }
    var lastOvertakeName by remember { mutableStateOf("") }

    val currentRacers by rememberUpdatedState(racers)
    val currentObstacles by rememberUpdatedState(obstacles)

    // 1. Particle Simulation Tick & Continuous Animation Loop (60 FPS)
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = now
                val currentPlayer = currentRacers.firstOrNull { it.isPlayer } ?: currentRacers.firstOrNull()
                val speed = currentPlayer?.speedKmh ?: 0f
                if (speed > 0f) {
                    val distanceStep = (speed * 1000f / 3600f) * dt
                    continuousDistance += distanceStep
                    roadScrollAccumulator = (roadScrollAccumulator + (speed * 4.2f * dt)) % 1000f
                    engineRumbleTimer = (engineRumbleTimer + dt * (speed / 15f)) % (2 * PI.toFloat())
                }

                // Update active particles with physical dynamics
                if (particles.isNotEmpty()) {
                    val iterator = particles.listIterator()
                    while (iterator.hasNext()) {
                        val p = iterator.next()
                        p.currentLife -= dt
                        if (p.currentLife <= 0f) {
                            iterator.remove()
                            continue
                        }

                        p.x += p.vx * dt
                        p.y += p.vy * dt
                        p.rotation += p.rotationSpeed * dt

                        when (p.type) {
                            ParticleVisualType.SPARK -> {
                                p.vy += 850f * dt // Gravity
                                p.vx *= (1f - dt * 2.5f) // Aerodynamic air drag
                            }
                            ParticleVisualType.DEBRIS_CHUNK -> {
                                p.vy += 650f * dt
                                p.vx *= (1f - dt * 1.8f)
                            }
                            ParticleVisualType.SLIPSTREAM_STREAK -> {
                                p.vy += (speed * 2.2f + 400f) * dt // Speed rush
                                p.size *= (1f + dt * 1.2f)
                            }
                            ParticleVisualType.SMOKE_PUFF -> {
                                p.vy -= 40f * dt // Smoke rising
                                p.size += 35f * dt // Billowing expansion
                            }
                            ParticleVisualType.SHOCKWAVE_RING -> {
                                p.size += 220f * dt // Expanding shockwave
                            }
                            ParticleVisualType.COMBO_STAR -> {
                                p.vy -= 120f * dt // Rising float
                                p.vx += sin(now * 0.00000001f) * 20f * dt
                            }
                        }
                    }
                }
            }
        }
    }

    // 2. React to Near-Miss Overtakes with Intense Visual Particle Bursts
    LaunchedEffect(nearMissBonus) {
        if (nearMissBonus != null && player != null) {
            val isLaneSplit = nearMissBonus.contains("DOUBLE") || nearMissBonus.contains("SPLIT")
            val burstCount = if (isLaneSplit) 36 else 22
            val bikeX = player.posX
            // Spawn vibrant golden and cyan combo burst particles
            val primaryColors = listOf(
                Color(0xFFFFD600), Color(0xFF00E5FF), Color(0xFFFF0055),
                Color(0xFF76FF03), Color(0xFFFFFFFF), Color(0xFFFF9100)
            )

            // Expanding Shockwave Ring
            particles.add(
                VisualCanvasParticle(
                    x = 0f, // Relative to bike center in draw
                    y = 0f,
                    vx = 0f,
                    vy = 0f,
                    color = if (isLaneSplit) Color(0xFFFFD600) else Color(0xFF00E5FF),
                    maxLife = 0.45f,
                    currentLife = 0.45f,
                    size = 18f,
                    type = ParticleVisualType.SHOCKWAVE_RING
                )
            )

            // Bursting stars and dynamic sparks
            for (i in 0 until burstCount) {
                val angle = (i.toFloat() / burstCount) * 2f * PI.toFloat() + (Random.nextFloat() * 0.4f)
                val speed = Random.nextFloat() * 320f + 120f
                val vx = cos(angle) * speed
                val vy = sin(angle) * speed - 100f
                val life = Random.nextFloat() * 0.5f + 0.35f
                val col = primaryColors[i % primaryColors.size]

                particles.add(
                    VisualCanvasParticle(
                        x = (Random.nextFloat() - 0.5f) * 30f,
                        y = (Random.nextFloat() - 0.5f) * 40f,
                        vx = vx,
                        vy = vy,
                        color = col,
                        maxLife = life,
                        currentLife = life,
                        size = Random.nextFloat() * 7f + 4f,
                        type = if (i % 2 == 0) ParticleVisualType.COMBO_STAR else ParticleVisualType.SPARK,
                        rotationSpeed = (Random.nextFloat() - 0.5f) * 12f
                    )
                )
            }
        }
    }

    // 3. React to Minor Impacts / Collisions with Friction Sparks and Debris
    LaunchedEffect(obstacles) {
        val currentHits = obstacles.filter { it.isHit }.map { it.id }.toSet()
        val newlyHit = currentHits - prevHitIds
        if (newlyHit.isNotEmpty() && player != null) {
            prevHitIds = currentHits
            // Spawn intense fiery shower of friction sparks and debris chunks
            val sparkColors = listOf(Color(0xFFFFD600), Color(0xFFFF5722), Color(0xFFFF1744), Color(0xFFFFFFFF))
            for (i in 0 until 35) {
                val angle = -PI.toFloat() * (Random.nextFloat() * 0.8f + 0.1f) // Upward and outward arc
                val speed = Random.nextFloat() * 450f + 150f
                val vx = cos(angle) * speed + (if (player.posX > 0f) -120f else 120f)
                val vy = sin(angle) * speed
                val life = Random.nextFloat() * 0.45f + 0.25f

                particles.add(
                    VisualCanvasParticle(
                        x = (Random.nextFloat() - 0.5f) * 25f,
                        y = 10f + (Random.nextFloat() - 0.5f) * 20f,
                        vx = vx,
                        vy = vy,
                        color = sparkColors[i % sparkColors.size],
                        maxLife = life,
                        currentLife = life,
                        size = Random.nextFloat() * 5.5f + 2.5f,
                        type = ParticleVisualType.SPARK
                    )
                )
            }
            // Add smoke puffs
            for (i in 0 until 8) {
                particles.add(
                    VisualCanvasParticle(
                        x = (Random.nextFloat() - 0.5f) * 20f,
                        y = 15f,
                        vx = (Random.nextFloat() - 0.5f) * 80f,
                        vy = -(Random.nextFloat() * 60f + 30f),
                        color = Color(0xFF64748B).copy(alpha = 0.6f),
                        maxLife = 0.6f,
                        currentLife = 0.6f,
                        size = 14f,
                        type = ParticleVisualType.SMOKE_PUFF
                    )
                )
            }
        }
    }

    // Keep continuousDistance synchronized if player distance resets (e.g. restart) or drifts
    LaunchedEffect(player?.totalRaceDistance) {
        val pDist = player?.totalRaceDistance ?: 0f
        if (abs(pDist - continuousDistance) > 3.0f || pDist == 0f) {
            continuousDistance = pDist
        }
    }

    val totalDistance = continuousDistance

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val fullRoadBottomY = height * 1.05f // Full screen road going all the way to the bottom of the screen

        if (player == null) return@Canvas

        val playerSpeed = player.speedKmh
        val isNitro = player.isNitroActive
        val currentCity = CityMilestoneCatalog.getCityForDistance(totalDistance)
        val roadCurvature = CityMilestoneCatalog.getRoadCurvature(totalDistance)
        val isMilestoneCrossing = CityMilestoneCatalog.isCrossing10KmMilestone(totalDistance)

        // Real Riding Physics: Camera Horizon Roll & Cockpit Banking into Turns
        val cameraBankAngle = (player.leanAngleRad * 6.2f) + (roadCurvature * 3.2f)

        // Real Riding Physics: Dynamic Suspension Pitch (Brake Dive vs Acceleration Squat)
        val pitchDiveOffset = (player.pitchAngleRad * height * 0.14f)

        // Real Riding Physics: Engine RPM Micro-Jitter & Speed Vibration
        val vibrationIntensity = (playerSpeed / 250f).coerceIn(0f, 1f)
        val rumbleOffsetY = if (playerSpeed > 60f) sin(engineRumbleTimer * 12f) * (1.8f * vibrationIntensity) else 0f
        val rumbleOffsetX = if (playerSpeed > 140f) cos(engineRumbleTimer * 16f) * (1.2f * vibrationIntensity) else 0f

        // 1. World Transform with Dynamic Horizon Banking & Weight Transfer Pitch
        withTransform({
            rotate(
                degrees = cameraBankAngle,
                pivot = Offset(width / 2f, height * 0.45f)
            )
            translate(left = rumbleOffsetX, top = rumbleOffsetY + pitchDiveOffset)
        }) {
            // 1.1 Multi-Layered Background Parallax (Sky, Far Mountains, Cyber Skyline)
            drawParallaxCitySkyline(
                width = width,
                horizonY = height * 0.12f,
                roadCurvature = roadCurvature,
                playerSteer = player.leanAngleRad,
                scrollOffset = roadScrollAccumulator,
                city = currentCity
            )

            // 1.2 3D Curved Highway Road with 5km Dynamic Turn Progression
            drawCurvedHighwayRoad(
                width = width,
                horizonY = height * 0.12f,
                bottomY = fullRoadBottomY,
                scrollOffset = roadScrollAccumulator,
                curvature = roadCurvature,
                speedKmh = playerSpeed,
                asphaltColor = Color(track.asphaltColorHex),
                curbColor1 = currentCity.primaryColor,
                curbColor2 = currentCity.secondaryColor,
                city = currentCity,
                totalDistance = totalDistance
            )

            // 1.3 Roadside Scenery (Trees 🌴, Buildings 🏢, Billboards 🪧, Streetlights 💡)
            // 1.4 Highway Roadside Scenery (Posts, Streetlights, Billboards & Skylines)
            drawRoadsideScenery(
                width = width,
                horizonY = height * 0.12f,
                bottomY = fullRoadBottomY,
                scrollOffset = roadScrollAccumulator,
                curvature = roadCurvature,
                city = currentCity,
                totalDistance = totalDistance
            )

            // 1.4.1 Realistic 3D Roadside Animated Trees (Both Shoulders, Wind Reaction, LOD)
            drawRoadsideAnimatedTrees(
                width = width,
                horizonY = height * 0.12f,
                bottomY = fullRoadBottomY,
                curvature = roadCurvature,
                totalDistance = totalDistance,
                playerSpeedKmh = playerSpeed
            )

            // 1.4 10KM City Welcome Overhead Gantry Gate
            if (isMilestoneCrossing) {
                drawCityWelcomeOverheadGate(
                    width = width,
                    horizonY = height * 0.12f,
                    bottomY = fullRoadBottomY,
                    scrollOffset = roadScrollAccumulator,
                    curvature = roadCurvature,
                    city = currentCity,
                    totalDistance = totalDistance
                )
            }

            // 1.5 Real-Time Competitor Bikes & Moving Highway Traffic (Pass other bikes back!)
            val (hasOvertake, overtakename) = drawHighwayRacersAndTraffic(
                racers = racers.filter { !it.isPlayer },
                player = player,
                obstacles = obstacles,
                width = width,
                horizonY = height * 0.12f,
                bottomY = fullRoadBottomY,
                curvature = roadCurvature,
                scrollOffset = roadScrollAccumulator,
                playerSpeedKmh = playerSpeed
            )

            if (hasOvertake) {
                lastOvertakeTime = System.currentTimeMillis()
                lastOvertakeName = overtakename
            }
        }

        // 2. High-Speed Motion Warp Lines & Wind Streaks (Outside banking for steady HUD alignment)
        if (playerSpeed > 80f) {
            drawHighSpeedWarpLines(
                width = width,
                horizonY = height * 0.12f,
                bottomY = fullRoadBottomY,
                speedKmh = playerSpeed,
                isNitro = isNitro,
                scrollOffset = roadScrollAccumulator,
                curvature = roadCurvature,
                totalDistance = totalDistance
            )
        }

        // 3. Dynamic "OVERTAKE / PASS!" Visual Pop Effect when speeding past rival bikes
        val now = System.currentTimeMillis()
        if (now - lastOvertakeTime < 1200L) {
            val progress = (now - lastOvertakeTime) / 1200f
            val alpha = (1f - progress).coerceIn(0f, 1f)
            val popY = height * 0.42f - (progress * 40f)

            drawOvertakeBanner(
                width = width,
                y = popY,
                alpha = alpha,
                racerName = lastOvertakeName,
                isNitro = isNitro
            )
        }

        val bikeCenterScreenX = (width / 2f) + (player.posX.coerceIn(-0.96f, 0.96f) * width * 0.98f * 0.92f) + (player.leanAngleRad * 20f) + rumbleOffsetX
        val bikeCenterScreenY = if (isFirstPersonCam) height * 0.88f + rumbleOffsetY else height * 0.70f + rumbleOffsetY

        // Check continuous proximity slipstream draft particles when threading alongside traffic
        if (playerSpeed > 45f && obstacles.isNotEmpty()) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastSlipstreamSpawnTime > 70L) {
                obstacles.forEach { obs ->
                    val distDiff = obs.distanceMeters - player.totalRaceDistance
                    val latDiff = abs(player.posX - obs.lane)
                    if (distDiff in -8f..18f && latDiff in 0.12f..0.38f) {
                        lastSlipstreamSpawnTime = currentTime
                        val side = if (player.posX > obs.lane) 1f else -1f
                        // Spawn aerodynamic slipstream draft trail
                        particles.add(
                            VisualCanvasParticle(
                                x = (side * 36f) + (Random.nextFloat() - 0.5f) * 14f,
                                y = (Random.nextFloat() - 0.5f) * 35f,
                                vx = side * (Random.nextFloat() * 40f + 25f),
                                vy = -(playerSpeed * 2.2f + 220f),
                                color = if (Random.nextBoolean()) Color(0xFF00E5FF) else Color(0xFFFFD600),
                                maxLife = 0.32f,
                                currentLife = 0.32f,
                                size = Random.nextFloat() * 3.5f + 2.5f,
                                type = ParticleVisualType.SLIPSTREAM_STREAK
                            )
                        )
                    }
                }
            }
        }

        // 4. Prominent Player Superbike & Seated Rider / First-Person Helmet Cockpit View
        if (isFirstPersonCam) {
            drawFirstPersonCockpitView(
                player = player,
                racers = racers,
                obstacles = obstacles,
                width = width,
                height = height,
                bikeY = height * 0.88f + rumbleOffsetY,
                isNitro = isNitro,
                rumbleOffsetX = rumbleOffsetX
            )
        } else {
            drawRealistic3DSuperbikeAndRider(
                player = player,
                racers = racers,
                obstacles = obstacles,
                width = width,
                bikeY = height * 0.70f + rumbleOffsetY,
                isNitro = isNitro,
                rumbleOffsetX = rumbleOffsetX
            )
        }

        // 5. Visual Particle Effects Layer (Sparks, Slipstreams, Shockwaves, Combo Stars, Smoke)
        drawActiveVisualParticles(
            particles = particles,
            bikeCenterX = bikeCenterScreenX,
            bikeCenterY = bikeCenterScreenY,
            isFirstPerson = isFirstPersonCam
        )
    }
}

/**
 * 1. Multi-Layered Parallax Background
 */
private fun DrawScope.drawParallaxCitySkyline(
    width: Float,
    horizonY: Float,
    roadCurvature: Float,
    playerSteer: Float,
    scrollOffset: Float,
    city: CityMilestone
) {
    val parallaxShiftFar = (roadCurvature * 35f) + (playerSteer * 25f)
    val parallaxShiftMid = (roadCurvature * 70f) + (playerSteer * 50f)

    // Sky gradient with Galaxy cosmic theme tint
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                GalaxyVoid,
                Color(0xFF0F0C1E),
                city.primaryColor.copy(alpha = 0.35f),
                GalaxyViolet.copy(alpha = 0.45f)
            ),
            startY = 0f,
            endY = horizonY + 30f
        ),
        topLeft = Offset(-width * 0.2f, -size.height * 0.2f),
        size = Size(width * 1.4f, horizonY + 30f + size.height * 0.2f)
    )

    // Galaxy Starfield & Nebulae
    val starCount = 36
    for (s in 0 until starCount) {
        val sSeed = s * 73
        val sx = ((-width * 0.2f) + (sSeed % (width * 1.4f).toInt()) + (parallaxShiftFar * 0.25f)) % (width * 1.4f)
        val sy = ((sSeed * 17) % (horizonY * 0.85f).toInt()).toFloat()
        val sAlpha = 0.4f + ((sin((System.currentTimeMillis() * 0.003f) + s) + 1f) * 0.3f)
        val sColor = when (s % 3) {
            0 -> GalaxyPink.copy(alpha = sAlpha)
            1 -> GalaxyCyan.copy(alpha = sAlpha)
            else -> Color.White.copy(alpha = sAlpha)
        }
        val sRadius = if (s % 5 == 0) 2.2f else 1.2f
        drawCircle(color = sColor, radius = sRadius, center = Offset(sx, sy))
    }

    // Glowing Neon Celestial Sun / Galaxy Moon
    val sunRadius = 44f
    val sunCenter = Offset((width / 2f) + parallaxShiftFar * 0.5f, horizonY + 2f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(GalaxyPink, GalaxyViolet, Color.Transparent),
            center = sunCenter,
            radius = sunRadius * 1.8f
        ),
        radius = sunRadius * 1.8f,
        center = sunCenter
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(GalaxyCyan, GalaxyPink, Color.Transparent),
            center = sunCenter,
            radius = sunRadius * 0.9f
        ),
        radius = sunRadius * 0.9f,
        center = sunCenter
    )
    drawCircle(
        color = Color(0xFFFDE047),
        radius = sunRadius * 0.45f,
        center = sunCenter
    )

    // Layer 1: Distant Mountain Silhouette
    val mountainPath = Path().apply {
        moveTo(-width * 0.2f, horizonY)
        val mCount = 10
        val segW = (width * 1.4f) / mCount
        for (i in 0..mCount) {
            val peakX = -width * 0.2f + (i * segW) + parallaxShiftFar
            val peakY = if (i % 2 == 0) horizonY - 24f else horizonY - 12f
            lineTo(peakX, peakY)
        }
        lineTo(width * 1.2f, horizonY)
        close()
    }
    drawPath(path = mountainPath, color = Color(0xFF131A2E).copy(alpha = 0.92f))

    // Layer 2: Midground City Skyline
    val buildingCount = 16
    val bWidth = (width * 1.4f) / 11f
    for (i in 0 until buildingCount) {
        val bX = (-width * 0.2f + (i * bWidth - (bWidth * 2))) + (parallaxShiftMid % bWidth)
        val bHeight = 26f + ((i * 19) % 48f)
        val bY = horizonY - bHeight
        val buildingW = bWidth * 0.85f

        drawRect(
            color = Color(0xFF0D1322),
            topLeft = Offset(bX, bY),
            size = Size(buildingW, bHeight + 5f)
        )

        drawLine(
            color = if (i % 2 == 0) city.primaryColor.copy(alpha = 0.75f) else city.secondaryColor.copy(alpha = 0.75f),
            start = Offset(bX, bY),
            end = Offset(bX + buildingW, bY),
            strokeWidth = 2f
        )

        val rows = (bHeight / 7f).toInt().coerceAtLeast(1)
        val cols = 3
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if ((i + r + c) % 3 == 0) {
                    val winX = bX + 4f + (c * (buildingW / cols))
                    val winY = bY + 4f + (r * 6f)
                    drawRect(
                        color = if ((r + c) % 2 == 0) city.secondaryColor.copy(alpha = 0.85f) else Color(0xFF00F0FF).copy(alpha = 0.85f),
                        topLeft = Offset(winX, winY),
                        size = Size(3.5f, 3.5f)
                    )
                }
            }
        }

        if (i % 3 == 0) {
            drawLine(
                color = Color(0xFFFF1744),
                start = Offset(bX + buildingW / 2f, bY),
                end = Offset(bX + buildingW / 2f, bY - 14f),
                strokeWidth = 1.5f
            )
            drawCircle(
                color = Color(0xFFFF1744),
                radius = 2.5f,
                center = Offset(bX + buildingW / 2f, bY - 14f)
            )
        }
    }

    drawLine(
        color = city.primaryColor.copy(alpha = 0.85f),
        start = Offset(-width * 0.2f, horizonY),
        end = Offset(width * 1.2f, horizonY),
        strokeWidth = 2.5f
    )
}

/**
 * 2. True 3D Perspective Curved Highway Road
 * Mathematically projects road slices based on world distance (Z) and camera height,
 * ensuring road markings and asphalt texture move underneath the motorcycle naturally with 1/Z speed.
 */
private fun DrawScope.drawCurvedHighwayRoad(
    width: Float,
    horizonY: Float,
    bottomY: Float,
    scrollOffset: Float,
    curvature: Float,
    speedKmh: Float,
    asphaltColor: Color,
    curbColor1: Color,
    curbColor2: Color,
    city: CityMilestone,
    totalDistance: Float
) {
    val bottomCenterX = width / 2f
    val topRoadHalfW = 65f
    val bottomRoadHalfW = width * 0.98f

    // Camera parameters for physical 3D perspective
    val camHeight = 1.35f
    val focalLength = (bottomY - horizonY) * 0.65f

    val segments = 46
    val slicePointsLeft = ArrayList<Offset>(segments + 1)
    val slicePointsRight = ArrayList<Offset>(segments + 1)
    val sliceCenters = ArrayList<Offset>(segments + 1)
    val sliceHalfWidths = ArrayList<Float>(segments + 1)
    val sliceWorldDists = ArrayList<Float>(segments + 1)

    // 1. Calculate perspective road slices with procedural curve geometry
    for (i in 0..segments) {
        val s = i.toFloat() / segments
        val zScreen = s * s // Quadratic depth density
        val y = horizonY + (bottomY - horizonY) * zScreen

        // Calculate world distance in meters ahead of camera
        val zWorld = (camHeight * focalLength) / (y - horizonY).coerceAtLeast(1.5f)
        val worldDist = totalDistance + zWorld

        val curveOffset = CityMilestoneCatalog.getProceduralRoadOffset(totalDistance, zWorld, width)
        val cX = bottomCenterX + curveOffset
        val halfW = topRoadHalfW + (bottomRoadHalfW - topRoadHalfW) * zScreen

        slicePointsLeft.add(Offset(cX - halfW, y))
        slicePointsRight.add(Offset(cX + halfW, y))
        sliceCenters.add(Offset(cX, y))
        sliceHalfWidths.add(halfW)
        sliceWorldDists.add(worldDist)
    }

    // 2. Base Dark Asphalt Highway Surface
    val fullRoadPath = Path().apply {
        moveTo(slicePointsLeft.first().x, slicePointsLeft.first().y)
        for (pt in slicePointsLeft) lineTo(pt.x, pt.y)
        for (pt in slicePointsRight.asReversed()) lineTo(pt.x, pt.y)
        close()
    }
    drawPath(path = fullRoadPath, color = asphaltColor)

    // 3. Dynamic Asphalt Textured Slices & High-Speed Tarmac Segments
    for (i in 0 until segments) {
        val wDist = sliceWorldDists[i]
        val isShaded = (wDist / 4.5f).toInt() % 2 == 0
        if (isShaded) {
            val p0L = slicePointsLeft[i]
            val p1L = slicePointsLeft[i + 1]
            val p1R = slicePointsRight[i + 1]
            val p0R = slicePointsRight[i]

            val segPath = Path().apply {
                moveTo(p0L.x, p0L.y)
                lineTo(p1L.x, p1L.y)
                lineTo(p1R.x, p1R.y)
                lineTo(p0R.x, p0R.y)
                close()
            }
            drawPath(
                path = segPath,
                color = Color.Black.copy(alpha = 0.14f)
            )
        }
    }

    // 4. 3D Alternating Curb Rumble Strips (Left & Right)
    for (i in 0 until segments) {
        val wDist = sliceWorldDists[i]
        val isAlt = (wDist / 3.0f).toInt() % 2 == 0
        val curbColor = if (isAlt) curbColor1 else curbColor2

        val p0L = slicePointsLeft[i]
        val p1L = slicePointsLeft[i + 1]
        val p0R = slicePointsRight[i]
        val p1R = slicePointsRight[i + 1]

        val s = i.toFloat() / segments
        val zScreen = s * s
        val curbW = 5f + zScreen * 22f

        // Left Curb Rumble Strip
        drawLine(
            color = curbColor,
            start = p0L,
            end = p1L,
            strokeWidth = curbW,
            cap = StrokeCap.Round
        )
        // Left Outer Neon Barrier Guide
        drawLine(
            color = GalaxyPink.copy(alpha = 0.7f),
            start = Offset(p0L.x - curbW * 0.45f, p0L.y),
            end = Offset(p1L.x - curbW * 0.45f, p1L.y),
            strokeWidth = 2.0f + zScreen * 3.5f,
            cap = StrokeCap.Round
        )

        // Right Curb Rumble Strip
        drawLine(
            color = curbColor,
            start = p0R,
            end = p1R,
            strokeWidth = curbW,
            cap = StrokeCap.Round
        )
        // Right Outer Neon Barrier Guide
        drawLine(
            color = GalaxyCyan.copy(alpha = 0.7f),
            start = Offset(p0R.x + curbW * 0.45f, p0R.y),
            end = Offset(p1R.x + curbW * 0.45f, p1R.y),
            strokeWidth = 2.0f + zScreen * 3.5f,
            cap = StrokeCap.Round
        )
    }

    // 5. Solid Outer Shoulder Emergency Lane Lines (-0.88f and +0.88f)
    val shoulderFractions = listOf(-0.88f, 0.88f)
    shoulderFractions.forEach { shFrac ->
        for (i in 0 until segments) {
            val c0 = sliceCenters[i]
            val c1 = sliceCenters[i + 1]
            val hw0 = sliceHalfWidths[i]
            val hw1 = sliceHalfWidths[i + 1]

            val x0 = c0.x + (hw0 * shFrac)
            val y0 = c0.y
            val x1 = c1.x + (hw1 * shFrac)
            val y1 = c1.y

            val s = i.toFloat() / segments
            val zScreen = s * s
            val alpha = (0.35f + zScreen * 0.65f).coerceIn(0f, 1f)
            val strokeW = 2.5f + zScreen * 6f

            drawLine(
                color = Color(0xFFFFD600).copy(alpha = alpha),
                start = Offset(x0, y0),
                end = Offset(x1, y1),
                strokeWidth = strokeW,
                cap = StrokeCap.Square
            )
        }
    }

    // 6. 3D Dashed Lane Markings (6 Broad Multi-Lane Highway System: -0.72, -0.44, -0.16, 0.16, 0.44, 0.72)
    val laneFractions = listOf(-0.72f, -0.44f, -0.16f, 0.16f, 0.44f, 0.72f)
    laneFractions.forEach { laneFraction ->
        for (i in 0 until segments) {
            val wDist = sliceWorldDists[i]
            // Standard highway dashed stripes: 6m dash / 6m gap
            val isStripe = (wDist % 12.0f) < 5.8f
            if (!isStripe) continue

            val c0 = sliceCenters[i]
            val c1 = sliceCenters[i + 1]
            val hw0 = sliceHalfWidths[i]
            val hw1 = sliceHalfWidths[i + 1]

            val x0 = c0.x + (hw0 * laneFraction)
            val y0 = c0.y
            val x1 = c1.x + (hw1 * laneFraction)
            val y1 = c1.y

            val s = i.toFloat() / segments
            val zScreen = s * s
            val alpha = (0.28f + zScreen * 0.72f).coerceIn(0f, 1f)
            val strokeW = 2.5f + zScreen * 7.5f

            drawLine(
                color = Color.White.copy(alpha = alpha),
                start = Offset(x0, y0),
                end = Offset(x1, y1),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }
    }

    // 7. Center Dividing Double Line at 0.0f
    for (i in 0 until segments) {
        val wDist = sliceWorldDists[i]
        val isStripe = (wDist % 8.0f) < 4.5f
        if (!isStripe) continue

        val c0 = sliceCenters[i]
        val c1 = sliceCenters[i + 1]
        val s = i.toFloat() / segments
        val zScreen = s * s
        val strokeW = 2.0f + zScreen * 5f
        val lineSep = 3.0f + zScreen * 8f

        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = (0.4f + zScreen * 0.6f).coerceIn(0f, 1f)),
            start = Offset(c0.x - lineSep / 2f, c0.y),
            end = Offset(c1.x - lineSep / 2f, c1.y),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = (0.4f + zScreen * 0.6f).coerceIn(0f, 1f)),
            start = Offset(c0.x + lineSep / 2f, c0.y),
            end = Offset(c1.x + lineSep / 2f, c1.y),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }

    // 8. Directional Speed Chevron Markings Painted on Asphalt (Every 80m)
    for (i in 0 until segments) {
        val wDist = sliceWorldDists[i]
        val isChevron = (wDist % 80.0f) < 4.0f
        if (!isChevron) continue

        val s = i.toFloat() / segments
        val zScreen = s * s
        if (zScreen < 0.12f) continue

        val c = sliceCenters[i]
        val hw = sliceHalfWidths[i]
        val cX = c.x
        val cY = c.y

        val chevW = hw * 0.75f
        val chevH = 12f + zScreen * 28f

        val chevronPath = Path().apply {
            moveTo(cX, cY - chevH)
            lineTo(cX - chevW * 0.4f, cY)
            lineTo(cX - chevW * 0.25f, cY)
            lineTo(cX, cY - chevH + (8f + zScreen * 10f))
            lineTo(cX + chevW * 0.25f, cY)
            lineTo(cX + chevW * 0.4f, cY)
            close()
        }
        drawPath(
            path = chevronPath,
            color = city.secondaryColor.copy(alpha = (zScreen * 0.85f).coerceIn(0f, 0.9f))
        )
    }
}

/**
 * 3. 3D Roadside Scenery (Trees, Skyscrapers, Billboards, Streetlights & Safety Posts)
 * Placed in physical world coordinates ahead of the rider:
 * - Distant objects move slowly near the horizon.
 * - Objects close to the road zoom past quickly with true 3D perspective parallax.
 */
private fun DrawScope.drawRoadsideScenery(
    width: Float,
    horizonY: Float,
    bottomY: Float,
    scrollOffset: Float,
    curvature: Float,
    city: CityMilestone,
    totalDistance: Float
) {
    val bottomCenterX = width / 2f
    val topRoadHalfW = 65f
    val bottomRoadHalfW = width * 0.98f

    val camHeight = 1.35f
    val focalLength = (bottomY - horizonY) * 0.65f

    // 1. Highway Safety Reflector Guide Posts (Every 12m)
    val postInterval = 12f
    val firstPostIdx = (totalDistance / postInterval).toInt()
    for (idx in firstPostIdx..(firstPostIdx + 16)) {
        val worldDist = idx * postInterval
        val zRel = worldDist - totalDistance
        if (zRel < 1.8f || zRel > 180f) continue

        val zScreen = ((camHeight * focalLength) / zRel) / (bottomY - horizonY)
        if (zScreen < 0.02f || zScreen > 1.05f) continue

        val y = horizonY + (bottomY - horizonY) * zScreen
        val curveOffset = CityMilestoneCatalog.getProceduralRoadOffset(totalDistance, zRel, width)
        val cX = bottomCenterX + curveOffset
        val halfW = topRoadHalfW + (bottomRoadHalfW - topRoadHalfW) * zScreen

        val postH = 6f + zScreen * 24f
        val postW = 1.5f + zScreen * 4f

        // Left post
        val leftPostX = cX - halfW - (postW * 1.5f)
        drawLine(
            color = Color(0xFF64748B),
            start = Offset(leftPostX, y),
            end = Offset(leftPostX, y - postH),
            strokeWidth = postW
        )
        drawCircle(
            color = Color(0xFFFFB703),
            radius = postW * 0.8f,
            center = Offset(leftPostX, y - postH + postW)
        )

        // Right post
        val rightPostX = cX + halfW + (postW * 1.5f)
        drawLine(
            color = Color(0xFF64748B),
            start = Offset(rightPostX, y),
            end = Offset(rightPostX, y - postH),
            strokeWidth = postW
        )
        drawCircle(
            color = Color(0xFFFF1E44),
            radius = postW * 0.8f,
            center = Offset(rightPostX, y - postH + postW)
        )
    }

    // 2. Roadside Landmark Scenery (Streetlights, Trees, Skyscrapers, Billboards)
    val landmarkInterval = 24f
    val firstLandmarkIdx = (totalDistance / landmarkInterval).toInt()

    for (idx in firstLandmarkIdx..(firstLandmarkIdx + 10)) {
        val worldDist = idx * landmarkInterval
        val zRel = worldDist - totalDistance
        if (zRel < 2.0f || zRel > 200f) continue

        val zScreen = ((camHeight * focalLength) / zRel) / (bottomY - horizonY)
        if (zScreen < 0.03f || zScreen > 1.04f) continue

        val y = horizonY + (bottomY - horizonY) * zScreen
        val curveOffset = CityMilestoneCatalog.getProceduralRoadOffset(totalDistance, zRel, width)
        val cX = bottomCenterX + curveOffset
        val halfW = topRoadHalfW + (bottomRoadHalfW - topRoadHalfW) * zScreen

        val scale = 0.2f + zScreen * 1.6f
        val isLeft = idx % 2 == 0
        val shoulderExtra = (16f + zScreen * 80f)
        val objX = cX + (if (isLeft) -(halfW + shoulderExtra) else (halfW + shoulderExtra))

        val objectType = idx % 4

        withTransform({
            translate(left = objX, top = y)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        }) {
            when (objectType) {
                0 -> {
                    // Modern High-Rise Cyber Skyscraper
                    val bH = 75f
                    val bW = 42f
                    drawRoundRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(-bW / 2f, -bH),
                        size = Size(bW, bH),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = city.primaryColor.copy(alpha = 0.85f),
                        topLeft = Offset(-bW / 2f, -bH),
                        size = Size(bW, bH),
                        cornerRadius = CornerRadius(4f, 4f),
                        style = Stroke(width = 2f)
                    )
                    // Windows with warm light glow
                    for (r in 1..5) {
                        for (c in 0..1) {
                            val winX = -bW / 2f + 7f + (c * 17f)
                            val winY = -bH + (r * 12f)
                            drawRect(
                                color = if ((r + c) % 2 == 0) city.secondaryColor else Color(0xFF00F0FF),
                                topLeft = Offset(winX, winY),
                                size = Size(10f, 6f)
                            )
                        }
                    }
                    drawCircle(color = city.secondaryColor, radius = 5f, center = Offset(0f, -bH - 6f))
                }

                1 -> {
                    // Cyber Highway Tree / Palm
                    drawRoundRect(
                        color = Color(0xFF4E342E),
                        topLeft = Offset(-3.5f, -42f),
                        size = Size(7f, 42f),
                        cornerRadius = CornerRadius(2.5f, 2.5f)
                    )
                    drawCircle(color = Color(0xFF00E676), radius = 22f, center = Offset(0f, -48f))
                    drawCircle(color = Color(0xFF00C853), radius = 15f, center = Offset(-9f, -44f))
                    drawCircle(color = Color(0xFF76FF03), radius = 15f, center = Offset(9f, -44f))
                }

                2 -> {
                    // Highway Arch Streetlamp with Ground Light Cone
                    val poleH = 70f
                    val armDir = if (isLeft) 20f else -20f
                    drawLine(
                        color = Color(0xFF64748B),
                        start = Offset(0f, 0f),
                        end = Offset(0f, -poleH),
                        strokeWidth = 3.5f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF64748B),
                        start = Offset(0f, -poleH),
                        end = Offset(armDir, -poleH + 5f),
                        strokeWidth = 3.5f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = Color(0xFFFFD600),
                        radius = 4.5f,
                        center = Offset(armDir, -poleH + 5f)
                    )
                    val lightConePath = Path().apply {
                        moveTo(armDir, -poleH + 5f)
                        lineTo(armDir - 28f, 0f)
                        lineTo(armDir + 28f, 0f)
                        close()
                    }
                    drawPath(
                        path = lightConePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFD600).copy(alpha = 0.35f), Color.Transparent),
                            startY = -poleH + 5f,
                            endY = 0f
                        )
                    )
                }

                else -> {
                    // Neon Digital Highway Billboard
                    val signH = 48f
                    val signW = 56f
                    drawLine(
                        color = Color(0xFF334155),
                        start = Offset(0f, 0f),
                        end = Offset(0f, -signH),
                        strokeWidth = 4f
                    )
                    drawRoundRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(-signW / 2f, -signH - 28f),
                        size = Size(signW, 28f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = city.primaryColor,
                        topLeft = Offset(-signW / 2f, -signH - 28f),
                        size = Size(signW, 28f),
                        cornerRadius = CornerRadius(4f, 4f),
                        style = Stroke(width = 2.2f)
                    )
                    drawRect(
                        color = city.secondaryColor,
                        topLeft = Offset(-signW / 2f + 4f, -signH - 22f),
                        size = Size(signW - 8f, 7f)
                    )
                    drawRect(
                        color = city.primaryColor,
                        topLeft = Offset(-signW / 2f + 4f, -signH - 11f),
                        size = Size(signW - 8f, 5f)
                    )
                }
            }
        }
    }
}

/**
 * 3.1 3D Roadside Animated Trees System
 * - Staggered procedural trees on BOTH left and right highway shoulders.
 * - 4 distinct procedural tree species:
 *   1. Alpine Conifer / Pine Tree (layered pyramidal needle tiers with sunlit canopy)
 *   2. Broadleaf Deciduous Oak (sturdy branched trunk with leafy canopy clouds)
 *   3. Coastal Fan Palm (slender ringed trunk with drooping fan fronds swaying in wind)
 *   4. Autumn Amber / Golden Maple (rich golden and amber foliage clusters)
 * - Dynamic wind response:
 *   Trees naturally sway with the breeze, and lean / flutter significantly more when the
 *   motorcycle speeds past (speed-induced wind buffeting).
 * - True 3D perspective projection with 1/Z scaling and speed-aligned motion.
 * - Soft ground contact shadows.
 * - Distance-based LOD and culling for silky-smooth 60 FPS performance.
 */
private fun DrawScope.drawRoadsideAnimatedTrees(
    width: Float,
    horizonY: Float,
    bottomY: Float,
    curvature: Float,
    totalDistance: Float,
    playerSpeedKmh: Float
) {
    val bottomCenterX = width / 2f
    val topRoadHalfW = 65f
    val bottomRoadHalfW = width * 0.98f

    val camHeight = 1.35f
    val focalLength = (bottomY - horizonY) * 0.65f

    val treeInterval = 14f
    val firstTreeIdx = (totalDistance / treeInterval).toInt()
    val timeSec = System.currentTimeMillis() * 0.003f
    val speedFactor = (playerSpeedKmh / 100f).coerceIn(0.2f, 2.4f)

    for (idx in firstTreeIdx..(firstTreeIdx + 16)) {
        // Draw on BOTH sides of the highway with natural staggered offsets
        for (side in 0..1) {
            val isLeft = side == 0
            val worldDist = idx * treeInterval + (if (isLeft) 0f else 7f)
            val zRel = worldDist - totalDistance
            if (zRel < 2.2f || zRel > 210f) continue

            val zScreen = ((camHeight * focalLength) / zRel) / (bottomY - horizonY)
            if (zScreen < 0.02f || zScreen > 1.05f) continue

            val y = horizonY + (bottomY - horizonY) * zScreen
            val curveOffset = CityMilestoneCatalog.getProceduralRoadOffset(totalDistance, zRel, width)
            val cX = bottomCenterX + curveOffset
            val halfW = topRoadHalfW + (bottomRoadHalfW - topRoadHalfW) * zScreen

            val shoulderDist = 28f + zScreen * 95f
            val treeX = if (isLeft) cX - halfW - shoulderDist else cX + halfW + shoulderDist
            val scale = 0.22f + zScreen * 1.85f

            // Dynamic Wind Sway Animation (reacts to motorcycle speed)
            val baseSway = sin(timeSec * 2.2f + idx * 1.4f).toFloat()
            val totalSway = (baseSway * 2.8f + speedFactor * 3.5f) * (if (isLeft) -1f else 1f)

            val species = (idx * 3 + if (isLeft) 1 else 2) % 4

            withTransform({
                translate(left = treeX, top = y)
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            }) {
                // Ground Contact Shadow
                drawOval(
                    color = Color.Black.copy(alpha = (0.42f * zScreen.coerceIn(0.25f, 0.75f))),
                    topLeft = Offset(-18f, -4f),
                    size = Size(36f, 10f)
                )

                when (species) {
                    0 -> {
                        // Alpine Conifer / Pine Tree
                        val trunkH = 50f
                        drawLine(
                            color = Color(0xFF3E2723),
                            start = Offset(0f, 0f),
                            end = Offset(totalSway * 0.2f, -trunkH),
                            strokeWidth = 5.5f,
                            cap = StrokeCap.Round
                        )

                        // 3 Layered Pyramidal Evergreen Needle Tiers
                        val tier1Y = -22f
                        val tier2Y = -34f
                        val tier3Y = -48f

                        // Tier 1 (Base Canopy)
                        val p1 = Path().apply {
                            moveTo(-22f, tier1Y)
                            lineTo(22f, tier1Y)
                            lineTo(totalSway * 0.4f, tier1Y - 18f)
                            close()
                        }
                        drawPath(p1, Color(0xFF1B4332))

                        // Tier 2 (Mid Canopy)
                        val p2 = Path().apply {
                            moveTo(-17f + totalSway * 0.3f, tier2Y)
                            lineTo(17f + totalSway * 0.3f, tier2Y)
                            lineTo(totalSway * 0.7f, tier2Y - 18f)
                            close()
                        }
                        drawPath(p2, Color(0xFF2D6A4F))

                        // Tier 3 (Sunlit Apex Canopy)
                        val p3 = Path().apply {
                            moveTo(-11f + totalSway * 0.6f, tier3Y)
                            lineTo(11f + totalSway * 0.6f, tier3Y)
                            lineTo(totalSway * 1.1f, tier3Y - 20f)
                            close()
                        }
                        drawPath(p3, Color(0xFF40916C))
                    }

                    1 -> {
                        // Sprawling Deciduous Oak
                        val trunkH = 46f
                        val tTopX = totalSway * 0.3f
                        drawLine(
                            color = Color(0xFF4A3525),
                            start = Offset(0f, 0f),
                            end = Offset(tTopX, -trunkH),
                            strokeWidth = 6.5f,
                            cap = StrokeCap.Round
                        )
                        // Boughs
                        drawLine(
                            color = Color(0xFF4A3525),
                            start = Offset(tTopX, -trunkH + 10f),
                            end = Offset(tTopX - 12f + totalSway * 0.5f, -trunkH - 8f),
                            strokeWidth = 4f
                        )
                        drawLine(
                            color = Color(0xFF4A3525),
                            start = Offset(tTopX, -trunkH + 10f),
                            end = Offset(tTopX + 12f + totalSway * 0.5f, -trunkH - 8f),
                            strokeWidth = 4f
                        )

                        // Foliage Clouds with Highlighting
                        val cY = -trunkH - 12f
                        drawCircle(color = Color(0xFF2D6A4F), radius = 22f, center = Offset(tTopX - 10f + totalSway * 0.6f, cY + 4f))
                        drawCircle(color = Color(0xFF2D6A4F), radius = 22f, center = Offset(tTopX + 10f + totalSway * 0.6f, cY + 4f))
                        drawCircle(color = Color(0xFF52B788), radius = 25f, center = Offset(tTopX + totalSway * 0.8f, cY - 4f))
                        drawCircle(color = Color(0xFF74C69D), radius = 16f, center = Offset(tTopX - 4f + totalSway * 0.9f, cY - 14f))
                    }

                    2 -> {
                        // Coastal Fan Palm
                        val trunkH = 54f
                        val palmPath = Path().apply {
                            moveTo(0f, 0f)
                            quadraticTo(
                                totalSway * 0.5f, -trunkH * 0.5f,
                                totalSway * 1.2f, -trunkH
                            )
                        }
                        drawPath(palmPath, Color(0xFF5D4037), style = Stroke(width = 5.5f, cap = StrokeCap.Round))

                        val crownX = totalSway * 1.2f
                        val crownY = -trunkH

                        // Coconut cluster
                        drawCircle(color = Color(0xFF3E2723), radius = 3.5f, center = Offset(crownX - 2f, crownY + 2f))
                        drawCircle(color = Color(0xFF3E2723), radius = 3.5f, center = Offset(crownX + 2f, crownY + 2f))

                        // Drooping Fan Fronds (Swaying in wind)
                        val frondAngles = listOf(-130f, -90f, -50f, -160f, -20f, -110f, -70f)
                        for (a in frondAngles) {
                            val rad = Math.toRadians((a + totalSway * 1.8f).toDouble()).toFloat()
                            val frondLen = 26f
                            val fEndX = crownX + cos(rad) * frondLen
                            val fEndY = crownY + sin(rad) * frondLen
                            val ctrlX = crownX + cos(rad) * (frondLen * 0.5f)
                            val ctrlY = crownY + sin(rad) * (frondLen * 0.5f) - 6f

                            val fPath = Path().apply {
                                moveTo(crownX, crownY)
                                quadraticTo(ctrlX, ctrlY, fEndX, fEndY)
                            }
                            drawPath(fPath, Color(0xFF2D6A4F), style = Stroke(width = 3.2f, cap = StrokeCap.Round))
                            drawCircle(color = Color(0xFF52B788), radius = 2.5f, center = Offset(fEndX, fEndY))
                        }
                    }

                    else -> {
                        // Autumn Amber / Golden Maple
                        val trunkH = 48f
                        val tTopX = totalSway * 0.3f
                        drawLine(
                            color = Color(0xFF2B2D42),
                            start = Offset(0f, 0f),
                            end = Offset(tTopX, -trunkH),
                            strokeWidth = 6.2f,
                            cap = StrokeCap.Round
                        )
                        val cY = -trunkH - 10f
                        drawCircle(color = Color(0xFFC84B31), radius = 20f, center = Offset(tTopX - 11f + totalSway * 0.5f, cY + 4f))
                        drawCircle(color = Color(0xFFE76F51), radius = 22f, center = Offset(tTopX + 11f + totalSway * 0.5f, cY + 4f))
                        drawCircle(color = Color(0xFFF4A261), radius = 24f, center = Offset(tTopX + totalSway * 0.8f, cY - 6f))
                        drawCircle(color = Color(0xFFE9C46A), radius = 15f, center = Offset(tTopX - 4f + totalSway * 0.9f, cY - 14f))

                        // Drifting golden leaves
                        if (zScreen > 0.15f) {
                            val leafOffset = (System.currentTimeMillis() * 0.04f + idx * 20f) % 30f
                            drawCircle(color = Color(0xFFE9C46A), radius = 1.8f, center = Offset(tTopX + 16f + leafOffset, cY + leafOffset))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. 10KM City Welcome Overhead Gantry Gate
 * Mathematically anchored to physical 10km milestone markers on the highway.
 */
private fun DrawScope.drawCityWelcomeOverheadGate(
    width: Float,
    horizonY: Float,
    bottomY: Float,
    scrollOffset: Float,
    curvature: Float,
    city: CityMilestone,
    totalDistance: Float
) {
    val bottomCenterX = width / 2f
    val topRoadHalfW = 65f
    val bottomRoadHalfW = width * 0.98f

    val camHeight = 1.35f
    val focalLength = (bottomY - horizonY) * 0.65f

    val milestoneDist = (round(totalDistance / 10000f) * 10000f).coerceAtLeast(10000f)
    val zRel = milestoneDist - totalDistance
    if (zRel < 1.8f || zRel > 220f) return

    val zScreen = ((camHeight * focalLength) / zRel) / (bottomY - horizonY)
    if (zScreen < 0.05f || zScreen > 1.05f) return

    val y = horizonY + (bottomY - horizonY) * zScreen
    val curveOffset = CityMilestoneCatalog.getProceduralRoadOffset(totalDistance, zRel, width)
    val centerX = bottomCenterX + curveOffset
    val roadHalfW = topRoadHalfW + (bottomRoadHalfW - topRoadHalfW) * zScreen

    val archHalfW = roadHalfW + (18f + zScreen * 35f)
    val archHeight = 40f + zScreen * 120f
    val boardHeight = 18f + zScreen * 42f
    val pillarW = 4f + zScreen * 11f

    // Left Gantry Truss Pillar
    drawLine(
        color = Color(0xFF1E293B),
        start = Offset(centerX - archHalfW, y),
        end = Offset(centerX - archHalfW, y - archHeight),
        strokeWidth = pillarW,
        cap = StrokeCap.Round
    )
    drawLine(
        color = city.primaryColor,
        start = Offset(centerX - archHalfW, y),
        end = Offset(centerX - archHalfW, y - archHeight),
        strokeWidth = pillarW * 0.5f,
        cap = StrokeCap.Round
    )

    // Right Gantry Truss Pillar
    drawLine(
        color = Color(0xFF1E293B),
        start = Offset(centerX + archHalfW, y),
        end = Offset(centerX + archHalfW, y - archHeight),
        strokeWidth = pillarW,
        cap = StrokeCap.Round
    )
    drawLine(
        color = city.primaryColor,
        start = Offset(centerX + archHalfW, y),
        end = Offset(centerX + archHalfW, y - archHeight),
        strokeWidth = pillarW * 0.5f,
        cap = StrokeCap.Round
    )

    val boardTop = y - archHeight
    drawRoundRect(
        color = Color(0xFF0B0F19).copy(alpha = 0.95f),
        topLeft = Offset(centerX - archHalfW, boardTop),
        size = Size(archHalfW * 2f, boardHeight),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = city.secondaryColor,
        topLeft = Offset(centerX - archHalfW, boardTop),
        size = Size(archHalfW * 2f, boardHeight),
        cornerRadius = CornerRadius(4f, 4f),
        style = Stroke(width = 2.5f + zScreen * 4f)
    )

    drawCircle(
        color = city.primaryColor,
        radius = 8f + zScreen * 14f,
        center = Offset(centerX, boardTop + boardHeight / 2f)
    )
    drawCircle(
        color = city.secondaryColor,
        radius = 5f + zScreen * 8f,
        center = Offset(centerX, boardTop + boardHeight / 2f)
    )

    drawLine(
        color = city.primaryColor.copy(alpha = (zScreen * 0.75f).coerceIn(0f, 0.85f)),
        start = Offset(centerX - archHalfW, boardTop + boardHeight),
        end = Offset(centerX + archHalfW, boardTop + boardHeight),
        strokeWidth = 3f + zScreen * 8f
    )
}

/**
 * 5. High-Speed Warp Lines & Wind Blur Streaks
 */
private fun DrawScope.drawHighSpeedWarpLines(
    width: Float,
    horizonY: Float,
    bottomY: Float,
    speedKmh: Float,
    isNitro: Boolean,
    scrollOffset: Float,
    curvature: Float,
    totalDistance: Float
) {
    val vanishingX = CityMilestoneCatalog.getHorizonVanishingX(totalDistance, width)
    val streakCount = if (isNitro) 24 else 14
    val streakColor = if (isNitro) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.45f)

    for (i in 0 until streakCount) {
        val angle = ((i * (360f / streakCount)) + scrollOffset * 0.15f) * (PI / 180f).toFloat()
        val spread = 60f + (i * 37 % (width * 0.46f))

        val startZ = 0.2f + ((scrollOffset * 3.5f + i * 50f) % 200f) / 200f * 0.75f
        val len = 0.18f

        val z0 = startZ
        val z1 = (startZ + len).coerceAtMost(1.0f)

        val y0 = horizonY + (bottomY - horizonY) * (z0 * z0)
        val y1 = horizonY + (bottomY - horizonY) * (z1 * z1)

        val x0 = vanishingX + (cos(angle) * spread * (z0 * z0))
        val x1 = vanishingX + (cos(angle) * spread * (z1 * z1))

        drawLine(
            color = streakColor.copy(alpha = (z0 * 0.75f).coerceIn(0f, 0.9f)),
            start = Offset(x0, y0),
            end = Offset(x1, y1),
            strokeWidth = 1.5f + z0 * 4f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * 6. Competitor Bikes & Moving Highway Traffic (Relative Velocity Passing Logic!)
 * When player speed is higher, you catch up to bikes, pass right by them, and they drop back behind you!
 */
private fun DrawScope.drawHighwayRacersAndTraffic(
    racers: List<RacerState>,
    player: RacerState,
    obstacles: List<HighwayObstacle>,
    width: Float,
    horizonY: Float,
    bottomY: Float,
    curvature: Float,
    scrollOffset: Float,
    playerSpeedKmh: Float
): Pair<Boolean, String> {
    val bottomCenterX = width / 2f
    val topRoadHalfW = 65f
    val bottomRoadHalfW = width * 0.98f

    var triggeredOvertake = false
    var overtakenRacerName = ""

    // 1. Moving Highway Traffic Obstacles & Train Crossings
    if (obstacles.isNotEmpty()) {
        obstacles.forEach { obs ->
            val distDiff = obs.distanceMeters - player.totalRaceDistance
            if (distDiff in -20f..280f) {
                val normDist = (1f - (distDiff / 280f)).coerceIn(0.05f, 1.05f)
                val z = (normDist * normDist).coerceIn(0.05f, 1.15f)

                val oppY = horizonY + (bottomY - horizonY) * z
                val curveOffset = CityMilestoneCatalog.getProceduralRoadOffset(player.totalRaceDistance, distDiff.coerceAtLeast(0f), width)
                val centerX = bottomCenterX + curveOffset
                val roadWAtZ = topRoadHalfW + (bottomRoadHalfW - topRoadHalfW) * z

                val trafficX = centerX + (roadWAtZ * obs.lane)
                val scale = (0.42f + z * 1.55f).coerceIn(0.42f, 2.45f)

                withTransform({
                    translate(left = trafficX, top = oppY)
                    scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
                }) {
                    when (obs.type) {
                        ObstacleType.TWO_WHEELER -> {
                            // Two-Wheeler Commuter / Scooter / Motorcycle Traffic (Enlarged & Detailed)
                            val bikeCol = Color(obs.primaryColorHex)
                            // Shadow
                            drawOval(
                                color = Color.Black.copy(alpha = 0.55f),
                                topLeft = Offset(-16f, 6f),
                                size = Size(32f, 14f)
                            )
                            // Rear Wheel & Tire
                            drawRoundRect(
                                color = Color(0xFF1E293B),
                                topLeft = Offset(-6f, -6f),
                                size = Size(12f, 24f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            // Bodywork / Cowl
                            drawRoundRect(
                                color = bikeCol,
                                topLeft = Offset(-12f, -24f),
                                size = Size(24f, 26f),
                                cornerRadius = CornerRadius(5f, 5f)
                            )
                            // Commuter Rider Body & Helmet
                            drawOval(
                                color = Color(0xFF0F172A),
                                topLeft = Offset(-10f, -38f),
                                size = Size(20f, 20f)
                            )
                            drawCircle(
                                color = Color(obs.secondaryColorHex),
                                radius = 8f,
                                center = Offset(0f, -32f)
                            )
                            // Visor
                            drawRoundRect(
                                color = Color(0xFF38BDF8),
                                topLeft = Offset(-6f, -34f),
                                size = Size(12f, 5.5f),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                            // Handlebars & Mirrors
                            drawLine(
                                color = Color(0xFF94A3B8),
                                start = Offset(-18f, -26f),
                                end = Offset(18f, -26f),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                            drawCircle(color = Color(0xFF64748B), radius = 2.8f, center = Offset(-18f, -27f))
                            drawCircle(color = Color(0xFF64748B), radius = 2.8f, center = Offset(18f, -27f))
                            // Glowing LED Tail Light & Indicator
                            drawRoundRect(
                                color = Color(0xFFFF1744),
                                topLeft = Offset(-6f, -8f),
                                size = Size(12f, 5f),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                            if (obs.isHonked) {
                                val blink = (System.currentTimeMillis() / 160) % 2 == 0L
                                if (blink) {
                                    drawCircle(color = Color(0xFFFFD600), radius = 4.5f, center = Offset(14f, -8f))
                                }
                            }
                        }
                        ObstacleType.FOUR_WHEELER, ObstacleType.TRAFFIC_CAR -> {
                            // Highway Sedan / Sports Car (Enlarged, Imposing Stance)
                            val carCol = Color(obs.primaryColorHex)
                            // Shadow
                            drawOval(
                                color = Color.Black.copy(alpha = 0.65f),
                                topLeft = Offset(-31f, 2f),
                                size = Size(62f, 18f)
                            )
                            // Car Lower Bumper & Body
                            drawRoundRect(
                                color = carCol,
                                topLeft = Offset(-26f, -32f),
                                size = Size(52f, 38f),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                            // Rear Windshield / Glass Cabin
                            drawRoundRect(
                                color = Color(0xFF0F172A),
                                topLeft = Offset(-19f, -30f),
                                size = Size(38f, 20f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            // Aerodynamic Lip Spoiler
                            drawRoundRect(
                                color = Color(obs.secondaryColorHex),
                                topLeft = Offset(-24f, -14f),
                                size = Size(48f, 4.5f),
                                cornerRadius = CornerRadius(1.5f, 1.5f)
                            )
                            // Dual Wide LED Taillight Bars
                            drawRoundRect(
                                color = Color(0xFFFF1744),
                                topLeft = Offset(-23f, -10f),
                                size = Size(15f, 5.5f),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                            drawRoundRect(
                                color = Color(0xFFFF1744),
                                topLeft = Offset(8f, -10f),
                                size = Size(15f, 5.5f),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                            // Dual Chrome Exhaust Pipes
                            drawCircle(color = Color(0xFFCBD5E1), radius = 3.2f, center = Offset(-16f, 4f))
                            drawCircle(color = Color(0xFFCBD5E1), radius = 3.2f, center = Offset(16f, 4f))
                            // Honk response turn signal indicator
                            if (obs.isHonked) {
                                val blink = (System.currentTimeMillis() / 160) % 2 == 0L
                                if (blink) {
                                    val indX = if (obs.lane >= 0f) 20f else -20f
                                    drawCircle(color = Color(0xFFFFD600), radius = 5.5f, center = Offset(indX, -10f))
                                }
                            }
                        }
                        ObstacleType.TRAFFIC_TAXI -> {
                            // City Taxi (High-Visibility Enlarged Cabin)
                            drawOval(
                                color = Color.Black.copy(alpha = 0.65f),
                                topLeft = Offset(-30f, 2f),
                                size = Size(60f, 18f)
                            )
                            drawRoundRect(
                                color = Color(0xFFFFD600),
                                topLeft = Offset(-26f, -33f),
                                size = Size(52f, 38f),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                            // Checker stripe
                            for (c in 0..6) {
                                val col = if (c % 2 == 0) Color.Black else Color.White
                                drawRect(
                                    color = col,
                                    topLeft = Offset(-26f + (c * 7.42f), -14f),
                                    size = Size(7.42f, 5f)
                                )
                            }
                            drawRoundRect(
                                color = Color(0xFF0F172A),
                                topLeft = Offset(-19f, -30f),
                                size = Size(38f, 20f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            // Glowing Roof Taxi Sign
                            drawRoundRect(
                                color = Color(0xFFFFFFFF),
                                topLeft = Offset(-10f, -42f),
                                size = Size(20f, 9f),
                                cornerRadius = CornerRadius(2.5f, 2.5f)
                            )
                            drawCircle(color = Color(0xFFFF9100), radius = 3f, center = Offset(0f, -37.5f))
                            // Taillights
                            drawCircle(color = Color(0xFFFF1744), radius = 5.5f, center = Offset(-18f, -6f))
                            drawCircle(color = Color(0xFFFF1744), radius = 5.5f, center = Offset(18f, -6f))
                        }
                        ObstacleType.TRUCK -> {
                            // Massive 18-Wheeler Heavy Freight Truck / Container (Imposing Giant)
                            val truckCol = Color(obs.primaryColorHex)
                            // Huge ground shadow
                            drawOval(
                                color = Color.Black.copy(alpha = 0.8f),
                                topLeft = Offset(-44f, 4f),
                                size = Size(88f, 26f)
                            )
                            // Heavy Dual Axles & Mudflaps (Left & Right)
                            drawRoundRect(
                                color = Color(0xFF0F172A),
                                topLeft = Offset(-36f, -6f),
                                size = Size(16f, 24f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            drawRoundRect(
                                color = Color(0xFF0F172A),
                                topLeft = Offset(20f, -6f),
                                size = Size(16f, 24f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            // Mudflaps with Hazard Reflectors
                            drawRect(color = Color.White, topLeft = Offset(-33f, 10f), size = Size(10f, 4f))
                            drawRect(color = Color.White, topLeft = Offset(23f, 10f), size = Size(10f, 4f))

                            // Large Cargo Container Box (Tall & Massive!)
                            val boxW = 68f
                            val boxH = 82f
                            drawRoundRect(
                                color = truckCol,
                                topLeft = Offset(-boxW / 2f, -boxH),
                                size = Size(boxW, boxH),
                                cornerRadius = CornerRadius(5f, 5f)
                            )
                            // Container Door Ribs & Latches
                            drawLine(
                                color = Color.Black.copy(alpha = 0.45f),
                                start = Offset(0f, -boxH),
                                end = Offset(0f, -2f),
                                strokeWidth = 3f
                            )
                            drawLine(
                                color = Color(0xFFCBD5E1),
                                start = Offset(-9f, -36f),
                                end = Offset(-9f, -14f),
                                strokeWidth = 2.5f
                            )
                            drawLine(
                                color = Color(0xFFCBD5E1),
                                start = Offset(9f, -36f),
                                end = Offset(9f, -14f),
                                strokeWidth = 2.5f
                            )

                            // Heavy Red/White Chevron Hazard Stripe across bumper
                            for (s in 0..7) {
                                val sc = if (s % 2 == 0) Color(0xFFFF1744) else Color.White
                                drawRect(
                                    color = sc,
                                    topLeft = Offset(-boxW / 2f + (s * 8.5f), -10f),
                                    size = Size(8.5f, 7f)
                                )
                            }
                            // Triple Amber Roof Marker Clearance Lamps
                            drawCircle(color = Color(0xFFFFD600), radius = 3.5f, center = Offset(-24f, -boxH + 5f))
                            drawCircle(color = Color(0xFFFFD600), radius = 3.5f, center = Offset(0f, -boxH + 5f))
                            drawCircle(color = Color(0xFFFFD600), radius = 3.5f, center = Offset(24f, -boxH + 5f))
                            // Dual Heavy Taillight Clusters
                            drawRoundRect(
                                color = Color(0xFFFF1744),
                                topLeft = Offset(-30f, -22f),
                                size = Size(11f, 8f),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                            drawRoundRect(
                                color = Color(0xFFFF1744),
                                topLeft = Offset(19f, -22f),
                                size = Size(11f, 8f),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                        }
                        ObstacleType.TEMPO -> {
                            // 3-Wheeler Auto-Tempo / Cargo Delivery Van (Enlarged)
                            val tempoCol = Color(obs.primaryColorHex)
                            drawOval(
                                color = Color.Black.copy(alpha = 0.6f),
                                topLeft = Offset(-24f, 2f),
                                size = Size(48f, 16f)
                            )
                            // Rear Tires
                            drawRoundRect(
                                color = Color(0xFF1E293B),
                                topLeft = Offset(-21f, -8f),
                                size = Size(9f, 20f),
                                cornerRadius = CornerRadius(3.5f, 3.5f)
                            )
                            drawRoundRect(
                                color = Color(0xFF1E293B),
                                topLeft = Offset(12f, -8f),
                                size = Size(9f, 20f),
                                cornerRadius = CornerRadius(3.5f, 3.5f)
                            )
                            // Lower Chassis / Cargo Bed
                            drawRoundRect(
                                color = tempoCol,
                                topLeft = Offset(-19f, -30f),
                                size = Size(38f, 28f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            // Canvas Canopy Roof Cover
                            drawRoundRect(
                                color = Color(0xFF334155),
                                topLeft = Offset(-18f, -44f),
                                size = Size(36f, 18f),
                                cornerRadius = CornerRadius(5f, 5f)
                            )
                            // Rear Spare Tire Mounted on Back
                            drawCircle(color = Color(0xFF0F172A), radius = 8f, center = Offset(0f, -20f))
                            drawCircle(color = Color(0xFF64748B), radius = 3.5f, center = Offset(0f, -20f))
                            // Round Tail Lights
                            drawCircle(color = Color(0xFFFF1744), radius = 4.5f, center = Offset(-13f, -7f))
                            drawCircle(color = Color(0xFFFF1744), radius = 4.5f, center = Offset(13f, -7f))
                        }
                        ObstacleType.TRAIN_CROSSING -> {
                            // Level Railway Crossing Spanning Across Highway
                            val trackSpan = 160f
                            // 1. Dual Parallel Steel Railroad Tracks across road
                            drawLine(
                                color = Color(0xFF94A3B8),
                                start = Offset(-trackSpan, -18f),
                                end = Offset(trackSpan, -18f),
                                strokeWidth = 4.5f
                            )
                            drawLine(
                                color = Color(0xFF94A3B8),
                                start = Offset(-trackSpan, 6f),
                                end = Offset(trackSpan, 6f),
                                strokeWidth = 4.5f
                            )
                            // Wooden / Concrete Sleepers
                            for (sl in -10..10) {
                                val sx = sl * 15f
                                drawLine(
                                    color = Color(0xFF475569),
                                    start = Offset(sx, -22f),
                                    end = Offset(sx, 10f),
                                    strokeWidth = 3f
                                )
                            }
                            // 2. Twin Crossing Signal Posts with Flashing Red Crossbuck
                            val blinker = (System.currentTimeMillis() / 250) % 2 == 0L
                            // Left Signal Post
                            drawLine(color = Color(0xFFCBD5E1), start = Offset(-trackSpan * 0.45f, 6f), end = Offset(-trackSpan * 0.45f, -65f), strokeWidth = 4f)
                            drawCircle(color = if (blinker) Color(0xFFFF1744) else Color(0xFF881337), radius = 5f, center = Offset(-trackSpan * 0.45f - 6f, -58f))
                            drawCircle(color = if (!blinker) Color(0xFFFF1744) else Color(0xFF881337), radius = 5f, center = Offset(-trackSpan * 0.45f + 6f, -58f))
                            // Right Signal Post
                            drawLine(color = Color(0xFFCBD5E1), start = Offset(trackSpan * 0.45f, 6f), end = Offset(trackSpan * 0.45f, -65f), strokeWidth = 4f)
                            drawCircle(color = if (blinker) Color(0xFFFF1744) else Color(0xFF881337), radius = 5f, center = Offset(trackSpan * 0.45f - 6f, -58f))
                            drawCircle(color = if (!blinker) Color(0xFFFF1744) else Color(0xFF881337), radius = 5f, center = Offset(trackSpan * 0.45f + 6f, -58f))

                            // 3. Lowered Crossing Barrier Arm with Red/White reflective stripes
                            val barArmL = -trackSpan * 0.45f
                            val barArmR = trackSpan * 0.45f
                            val barY = -18f
                            drawLine(color = Color.White, start = Offset(barArmL, barY), end = Offset(barArmR, barY), strokeWidth = 5f)
                            for (b in 0..14) {
                                val bx = barArmL + (b * (barArmR - barArmL) / 14f)
                                drawLine(color = Color(0xFFFF1744), start = Offset(bx, barY - 2.5f), end = Offset(bx + 4f, barY + 2.5f), strokeWidth = 3f)
                            }

                            // 4. Moving High-Speed Freight / Bullet Train passing through!
                            // Train moves horizontally across tracks
                            val trainProgress = obs.trainProgress
                            val trainX = -260f + (trainProgress * 520f)
                            val trainY = -24f
                            val trainH = 34f
                            val carCount = 5
                            val carW = 60f

                            for (c in 0 until carCount) {
                                val cX = trainX - (c * (carW + 6f))
                                if (cX in -300f..300f) {
                                    val isEngine = (c == 0)
                                    val trainColor = if (isEngine) Color(0xFFDC2626) else Color(0xFF1E293B)
                                    drawRoundRect(
                                        color = trainColor,
                                        topLeft = Offset(cX, trainY - trainH),
                                        size = Size(carW, trainH),
                                        cornerRadius = CornerRadius(4f, 4f)
                                    )
                                    // Train Windows (Glowing)
                                    for (w in 0..3) {
                                        drawRoundRect(
                                            color = Color(0xFFFEF08A),
                                            topLeft = Offset(cX + 6f + (w * 13f), trainY - trainH + 6f),
                                            size = Size(9f, 9f),
                                            cornerRadius = CornerRadius(1.5f, 1.5f)
                                        )
                                    }
                                    // Engine High-Intensity Warning Headlight Cone
                                    if (isEngine) {
                                        drawCircle(color = Color(0xFFFFFFFF), radius = 5f, center = Offset(cX + carW - 2f, trainY - trainH + 12f))
                                        drawPath(
                                            path = Path().apply {
                                                moveTo(cX + carW, trainY - trainH + 12f)
                                                lineTo(cX + carW + 80f, trainY - trainH - 20f)
                                                lineTo(cX + carW + 80f, trainY + 20f)
                                                close()
                                            },
                                            brush = Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent))
                                        )
                                    }
                                }
                            }
                        }
                        ObstacleType.ROAD_CONE -> {
                            // High Visibility Safety Cone
                            val coneH = 28f
                            val coneBaseW = 20f
                            // Base
                            drawRoundRect(
                                color = Color(0xFFFF6D00),
                                topLeft = Offset(-coneBaseW / 2f, -6f),
                                size = Size(coneBaseW, 6f),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                            // Cone body
                            val conePath = Path().apply {
                                moveTo(0f, -coneH)
                                lineTo(coneBaseW * 0.42f, -6f)
                                lineTo(-coneBaseW * 0.42f, -6f)
                                close()
                            }
                            drawPath(path = conePath, color = Color(0xFFFF6D00))
                            // Reflective white band
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(-coneBaseW * 0.25f, -18f),
                                size = Size(coneBaseW * 0.5f, 6f)
                            )
                        }
                        ObstacleType.BARRIER -> {
                            // Roadworks Hazard Barrier
                            val barW = 36f
                            val barH = 22f
                            drawRoundRect(
                                color = Color(0xFFE2E8F0),
                                topLeft = Offset(-barW / 2f, -barH),
                                size = Size(barW, barH),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                            // Orange diagonal stripes
                            for (s in 0..2) {
                                val sx = -barW / 2f + 4f + (s * 10f)
                                drawRect(
                                    color = Color(0xFFFF3D00),
                                    topLeft = Offset(sx, -barH + 2f),
                                    size = Size(6f, barH - 4f)
                                )
                            }
                            // Flashing Warning Light
                            val blink = ((System.currentTimeMillis() / 250) % 2 == 0L)
                            drawCircle(
                                color = if (blink) Color(0xFFFFD600) else Color(0xFFFF8F00),
                                radius = 4.5f,
                                center = Offset(0f, -barH - 4f)
                            )
                        }
                        ObstacleType.OIL_SPILL -> {
                            drawOval(
                                color = Color(0xFF111111).copy(alpha = 0.85f),
                                topLeft = Offset(-20f, -6f),
                                size = Size(40f, 12f)
                            )
                            drawOval(
                                color = Color(0xFF222222).copy(alpha = 0.7f),
                                topLeft = Offset(-14f, -4f),
                                size = Size(20f, 8f)
                            )
                        }
                        ObstacleType.DEBRIS -> {
                            drawRoundRect(
                                color = Color(0xFF5D4037),
                                topLeft = Offset(-12f, -10f),
                                size = Size(24f, 10f),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                            drawLine(
                                color = Color(0xFF3E2723),
                                start = Offset(-10f, -10f),
                                end = Offset(-10f, 0f),
                                strokeWidth = 2f
                            )
                            drawLine(
                                color = Color(0xFF3E2723),
                                start = Offset(10f, -10f),
                                end = Offset(10f, 0f),
                                strokeWidth = 2f
                            )
                        }
                    }

                    // Slipstream speed wake when passing
                    if (z > 0.70f && playerSpeedKmh > 150f && obs.type != ObstacleType.TRAIN_CROSSING) {
                        drawLine(
                            color = Color(0xFF00F0FF).copy(alpha = 0.65f),
                            start = Offset(-13f, 4f),
                            end = Offset(-13f, 28f),
                            strokeWidth = 2.5f
                        )
                        drawLine(
                            color = Color(0xFF00F0FF).copy(alpha = 0.65f),
                            start = Offset(13f, 4f),
                            end = Offset(13f, 28f),
                            strokeWidth = 2.5f
                        )
                    }
                }
            }
        }
    } else {
        // Fallback procedural traffic
        val trafficCount = 3
        val trafficSpeedKmh = 120f
        val relativeTrafficSpeed = playerSpeedKmh - trafficSpeedKmh

        for (t in 0 until trafficCount) {
            val trafficProgress = ((t * 0.33f) + (scrollOffset * 0.0035f * (relativeTrafficSpeed / 100f).coerceIn(0.2f, 2.5f))) % 1f
            val z = trafficProgress * trafficProgress

            if (z in 0.10f..0.96f) {
                val oppY = horizonY + (bottomY - horizonY) * z
                val approxRelZ = (1f - trafficProgress) * 200f
                val curveOffset = CityMilestoneCatalog.getProceduralRoadOffset(player.totalRaceDistance, approxRelZ, width)
                val centerX = bottomCenterX + curveOffset
                val roadWAtZ = topRoadHalfW + (bottomRoadHalfW - topRoadHalfW) * z

                val laneIdx = if (t % 2 == 0) -0.55f else 0.55f
                val trafficX = centerX + (roadWAtZ * laneIdx)
                val scale = 0.42f + z * 1.45f

                withTransform({
                    translate(left = trafficX, top = oppY)
                    scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
                }) {
                    drawRoundRect(
                        color = if (t == 0) Color(0xFFFFD600) else Color(0xFF00E5FF),
                        topLeft = Offset(-24f, -34f),
                        size = Size(48f, 38f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(-18f, -30f),
                        size = Size(36f, 22f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawCircle(color = Color(0xFFFF1744), radius = 5.5f, center = Offset(-16f, 0f))
                    drawCircle(color = Color(0xFFFF1744), radius = 5.5f, center = Offset(16f, 0f))
                }
            }
        }
    }

    // 2. AI Competitor Racers with Relative Velocity Perspective
    racers.forEachIndexed { index, opp ->
        val distDiff = (opp.totalRaceDistance - player.totalRaceDistance)

        // Only draw bikes in visible visual range (-30m behind to +280m ahead)
        if (distDiff in -30f..280f) {
            // When distDiff > 0: Opponent is ahead (approaching as player accelerates)
            // When distDiff <= 0: Player has passed opponent (dropping back into rear view!)
            val normDist = (1f - (distDiff / 280f)).coerceIn(0.05f, 1.05f)
            val z = (normDist * normDist).coerceIn(0.05f, 1.15f)

            val oppY = horizonY + (bottomY - horizonY) * z
            val curveOffset = CityMilestoneCatalog.getProceduralRoadOffset(player.totalRaceDistance, distDiff.coerceAtLeast(0f), width)
            val centerX = bottomCenterX + curveOffset
            val roadWAtZ = topRoadHalfW + (bottomRoadHalfW - topRoadHalfW) * z

            val laneIndex = (index % 4) - 1.5f
            val laneOffsetFraction = (laneIndex / 2.0f) * 0.72f
            val oppX = centerX + (roadWAtZ * laneOffsetFraction)

            // Dynamic scale: scales up large as you approach, then zooms past behind!
            val scale = (0.42f + z * 1.55f).coerceIn(0.42f, 2.45f)
            val oppPrimary = Color(opp.primaryColorHex)
            val oppSecondary = Color(opp.secondaryColorHex)
            val oppLean = opp.leanAngleRad * 18f

            // Check if player is actively passing this bike right now
            if (distDiff in -5f..12f && playerSpeedKmh > opp.speedKmh) {
                triggeredOvertake = true
                overtakenRacerName = opp.name
            }

            withTransform({
                translate(left = oppX, top = oppY)
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            }) {
                // Shadow
                drawOval(
                    color = Color.Black.copy(alpha = 0.55f),
                    topLeft = Offset(-16f, -12f),
                    size = Size(32f, 22f)
                )

                // Slipstream Wind Trail when passing
                if (z > 0.65f) {
                    val trailAlpha = ((z - 0.65f) / 0.35f).coerceIn(0f, 0.7f)
                    drawLine(
                        color = Color(0xFF00F0FF).copy(alpha = trailAlpha),
                        start = Offset(-10f, 10f),
                        end = Offset(-10f, 40f),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = Color(0xFF00F0FF).copy(alpha = trailAlpha),
                        start = Offset(10f, 10f),
                        end = Offset(10f, 40f),
                        strokeWidth = 3f
                    )
                }

                // Rear Racing Tire
                drawRoundRect(
                    color = Color(0xFF161B22),
                    topLeft = Offset(-6f + oppLean * 0.2f, -10f),
                    size = Size(12f, 22f),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Bike Fairing / Tail Cowl
                val path = Path().apply {
                    moveTo(0f, -26f)
                    lineTo(10f + oppLean * 0.3f, -8f)
                    lineTo(8f + oppLean * 0.4f, 12f)
                    lineTo(-8f + oppLean * 0.4f, 12f)
                    lineTo(-10f + oppLean * 0.3f, -8f)
                    close()
                }
                drawPath(path = path, color = oppPrimary)

                // Rider Back, Helmet & Leathers
                drawOval(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(-10f + oppLean * 0.6f, -16f),
                    size = Size(20f, 22f)
                )
                drawCircle(
                    color = oppSecondary,
                    radius = 6.5f,
                    center = Offset(0f + oppLean * 0.9f, -14f)
                )

                // Glowing LED Taillight
                drawRoundRect(
                    color = Color(0xFFFF1744),
                    topLeft = Offset(-6f + oppLean * 0.3f, 10f),
                    size = Size(12f, 4.5f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }
    }

    return Pair(triggeredOvertake, overtakenRacerName)
}

/**
 * 7. Realistic 3D Superbike & Seated Rider
 * - Detailed 3D motorcycle model with suspension pitch, rotating rear slick tire, burnt titanium exhaust,
 *   glowing brake rotor under braking, and reactive LED tail lights.
 * - Realistic human rider anatomically seated with thighs hugging the tank, articulated arms with hands
 *   naturally connected to the handlebars, knee slider touching down in sharp turns, and dynamic body posture
 *   (low aerodynamic tuck during acceleration, upright air-brake during deceleration, hang-off in turns).
 */
private fun DrawScope.drawRealistic3DSuperbikeAndRider(
    player: RacerState,
    racers: List<RacerState>,
    obstacles: List<HighwayObstacle>,
    width: Float,
    bikeY: Float,
    isNitro: Boolean,
    rumbleOffsetX: Float
) {
    val vanishingX = width / 2f
    val roadHalfW = width * 0.98f

    val lateralNorm = player.posX.coerceIn(-0.96f, 0.96f)
    val bikeX = vanishingX + (lateralNorm * roadHalfW * 0.92f) + (player.leanAngleRad * 20f) + rumbleOffsetX

    val leanOffset = player.leanAngleRad * 28f
    val throttle = player.throttleRatio
    val brake = player.brakeRatio
    val isTucking = throttle > 0.25f || player.speedKmh > 160f || isNitro
    val isBraking = brake > 0.15f

    // Dynamic Suspension & Engine Vibration
    val bounceY = if (player.speedKmh > 10f) sin(System.currentTimeMillis() * 0.05f * (player.speedKmh / 50f)) * 3.2f else 0f
    val pitchSquatY = player.pitchAngleRad * 22f // Squats under acceleration (+), dives under braking (-)

    val bikeScale = 3.65f

    val bikePrimary = Color(player.primaryColorHex)
    val bikeSecondary = Color(player.secondaryColorHex)

    val galaxyBodyBrush = Brush.linearGradient(
        colors = listOf(bikePrimary, bikeSecondary, bikePrimary.copy(alpha = 0.8f), bikeSecondary.copy(alpha = 0.9f)),
        start = Offset(-16f, -40f),
        end = Offset(16f, 10f)
    )

    withTransform({
        translate(left = bikeX, top = bikeY + bounceY + pitchSquatY)
        scale(scaleX = bikeScale, scaleY = bikeScale, pivot = Offset.Zero)
    }) {
        // 0. Soft Asphalt Contact Shadow
        drawOval(
            color = Color.Black.copy(alpha = 0.7f),
            topLeft = Offset(-28f + leanOffset * 0.3f, -8f),
            size = Size(56f, 28f)
        )

        // 1. Dual Forward Xenon Headlight Cones casting onto the highway
        drawPath(
            path = Path().apply {
                moveTo(-8f, -32f)
                lineTo(-75f, -250f)
                lineTo(75f, -250f)
                lineTo(8f, -32f)
                close()
            },
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF80F7FF).copy(alpha = 0.45f), Color.Transparent),
                startY = -32f,
                endY = -250f
            )
        )

        // 2. Twin Nitro / Acceleration Exhaust Flame Jets
        if (isNitro || player.speedKmh > 185f) {
            val flamePulse = ((System.currentTimeMillis() % 100) / 100f) * 6f
            val flameBrush = Brush.radialGradient(
                colors = listOf(GalaxyCyan, GalaxyPink, GalaxyViolet, Color.Transparent),
                center = Offset(14f + leanOffset * 0.3f, 26f),
                radius = 38f + flamePulse
            )
            val jitter = ((System.currentTimeMillis() % 8) - 4f) * 0.9f
            drawCircle(
                brush = flameBrush,
                radius = 22f + flamePulse,
                center = Offset(14f + leanOffset * 0.3f, 26f + jitter)
            )
            drawOval(
                color = Color.White,
                topLeft = Offset(12f + leanOffset * 0.3f, 24f + jitter),
                size = Size(4f, 9f)
            )
        }

        // 3. Wide 200mm Rear Racing Slick Tire
        drawRoundRect(
            color = Color(0xFF111114),
            topLeft = Offset(-8.5f + leanOffset * 0.25f, -6f),
            size = Size(17f, 34f),
            cornerRadius = CornerRadius(5.5f, 5.5f)
        )
        // 3D Directional Tire Tread Grooves
        val treadOffset = (System.currentTimeMillis() * player.speedKmh * 0.006f) % 8f
        for (i in 0..4) {
            val ty = -4f + (i * 8f) + treadOffset
            if (ty < 26f) {
                drawLine(
                    color = Color(0xFF33333E),
                    start = Offset(-5f + leanOffset * 0.25f, ty),
                    end = Offset(-5f + leanOffset * 0.25f, ty + 4f),
                    strokeWidth = 1.8f
                )
                drawLine(
                    color = Color(0xFF33333E),
                    start = Offset(5f + leanOffset * 0.25f, ty),
                    end = Offset(5f + leanOffset * 0.25f, ty + 4f),
                    strokeWidth = 1.8f
                )
            }
        }

        // 4. Gold Drive Chain & Rear Sprocket (Left)
        val chainX = -8f + leanOffset * 0.25f
        drawLine(
            color = Color(0xFFFFD700),
            start = Offset(chainX, -4f),
            end = Offset(chainX, 20f),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )
        drawCircle(color = Color(0xFF64748B), radius = 3.2f, center = Offset(chainX, 8f))

        // 5. Ventilated Rear Brake Disc & Caliper (Right) - GLOWS RED UNDER HARD BRAKING!
        val brakeX = 7.5f + leanOffset * 0.25f
        drawCircle(color = Color(0xFFCBD5E1), radius = 4f, center = Offset(brakeX, 8f))
        drawCircle(color = Color(0xFFFFD700), radius = 2f, center = Offset(brakeX, 8f))
        if (isBraking) {
            // Glowing hot red/orange rotor under deceleration
            drawCircle(
                color = Color(0xFFFF3D00).copy(alpha = (brake * 0.85f).coerceIn(0.4f, 0.95f)),
                radius = 5.2f,
                center = Offset(brakeX, 8f)
            )
            drawCircle(
                color = Color(0xFFFFD600).copy(alpha = (brake * 0.7f).coerceIn(0.2f, 0.8f)),
                radius = 3f,
                center = Offset(brakeX, 8f)
            )
        }

        // 6. Aluminum Swingarm & Inverted Monoshock Suspension Spring
        drawPath(
            path = Path().apply {
                moveTo(-10f + leanOffset * 0.3f, -12f)
                lineTo(10f + leanOffset * 0.3f, -12f)
                lineTo(11f + leanOffset * 0.3f, 2f)
                lineTo(-11f + leanOffset * 0.3f, 2f)
                close()
            },
            color = Color(0xFF94A3B8)
        )
        // Spring coil (compresses under acceleration squat)
        val springSquat = (player.pitchAngleRad * 4f).coerceIn(-3f, 3f)
        for (sc in 0..3) {
            val sy = -10f + (sc * 3.5f) + springSquat
            drawLine(
                color = GalaxyPink,
                start = Offset(-2.5f + leanOffset * 0.3f, sy),
                end = Offset(2.5f + leanOffset * 0.3f, sy + 1.5f),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
        }

        // 7. Galaxy Superbike Tail Cowl & Leather Racing Saddle
        val tailPath = Path().apply {
            moveTo(-14f + leanOffset * 0.4f, -28f)
            lineTo(14f + leanOffset * 0.4f, -28f)
            lineTo(16f + leanOffset * 0.45f, -8f)
            lineTo(-16f + leanOffset * 0.45f, -8f)
            close()
        }
        drawPath(path = tailPath, brush = galaxyBodyBrush)

        // Neon Tail Accents & Downforce Winglets
        drawLine(
            color = GalaxyCyan,
            start = Offset(-14f + leanOffset * 0.4f, -28f),
            end = Offset(-16f + leanOffset * 0.45f, -8f),
            strokeWidth = 1.5f
        )
        drawLine(
            color = GalaxyPink,
            start = Offset(14f + leanOffset * 0.4f, -28f),
            end = Offset(16f + leanOffset * 0.45f, -8f),
            strokeWidth = 1.5f
        )

        // Alcantara Racing Seat with Red Stitching
        drawRoundRect(
            color = Color(0xFF171720),
            topLeft = Offset(-12f + leanOffset * 0.42f, -34f),
            size = Size(24f, 20f),
            cornerRadius = CornerRadius(5f, 5f)
        )
        drawLine(
            color = GalaxyPink.copy(alpha = 0.7f),
            start = Offset(-10f + leanOffset * 0.42f, -32f),
            end = Offset(10f + leanOffset * 0.42f, -32f),
            strokeWidth = 0.8f
        )

        // Burnt Titanium Silencer Exhaust (Heat Temper Bands: Gold -> Violet -> Cobalt -> Cyan)
        val exhaustX = 12f + leanOffset * 0.3f
        val exhaustBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE2B056),
                Color(0xFFC026D3),
                Color(0xFF2563EB),
                Color(0xFF00F0FF)
            ),
            startY = 0f,
            endY = 26f
        )
        drawRoundRect(
            brush = exhaustBrush,
            topLeft = Offset(exhaustX, 0f),
            size = Size(7.5f, 26f),
            cornerRadius = CornerRadius(3.5f, 3.5f)
        )
        drawOval(
            color = Color(0xFF0F172A),
            topLeft = Offset(exhaustX + 1f, 23f),
            size = Size(5.5f, 4f)
        )
        drawOval(
            color = GalaxyCyan,
            topLeft = Offset(exhaustX + 2f, 24f),
            size = Size(3.5f, 2f)
        )

        // 7.5 Complete Front Wheel Assembly & Inverted Telescopic Fork Stanchions
        val frontWheelSteer = player.leanAngleRad * 0.45f
        val cowlLean = leanOffset * 1.35f
        val frontWheelX = cowlLean * 0.70f + frontWheelSteer * 7f
        val frontWheelY = -66f

        // Rotating Front Tire with Directional Tread Grooves
        drawRoundRect(
            color = Color(0xFF111114),
            topLeft = Offset(frontWheelX - 5.5f, frontWheelY),
            size = Size(11f, 26f),
            cornerRadius = CornerRadius(4.5f, 4.5f)
        )
        val frontTreadOffset = (System.currentTimeMillis() * player.speedKmh * 0.006f) % 7f
        for (i in 0..3) {
            val fty = frontWheelY + 2f + (i * 7f) + frontTreadOffset
            if (fty < frontWheelY + 24f) {
                drawLine(
                    color = Color(0xFF33333E),
                    start = Offset(frontWheelX - 3.5f, fty),
                    end = Offset(frontWheelX - 3.5f, fty + 3f),
                    strokeWidth = 1.4f
                )
                drawLine(
                    color = Color(0xFF33333E),
                    start = Offset(frontWheelX + 3.5f, fty),
                    end = Offset(frontWheelX + 3.5f, fty + 3f),
                    strokeWidth = 1.4f
                )
            }
        }

        // Dual Ventilated Drilled Front Brake Discs & Brembo Radial Calipers
        val leftDiscX = frontWheelX - 6f
        val rightDiscX = frontWheelX + 6f
        val discY = frontWheelY + 13f
        drawCircle(color = Color(0xFFCBD5E1), radius = 5.2f, center = Offset(leftDiscX, discY))
        drawCircle(color = Color(0xFFFFD700), radius = 2.4f, center = Offset(leftDiscX, discY))
        drawCircle(color = Color(0xFFCBD5E1), radius = 5.2f, center = Offset(rightDiscX, discY))
        drawCircle(color = Color(0xFFFFD700), radius = 2.4f, center = Offset(rightDiscX, discY))

        // Radial Calipers (Glow warm under deceleration)
        val caliperColor = if (isBraking) Color(0xFFFF3D00) else Color(0xFF1E293B)
        drawRoundRect(color = caliperColor, topLeft = Offset(leftDiscX - 2.5f, discY - 4f), size = Size(3.5f, 8f), cornerRadius = CornerRadius(1.2f, 1.2f))
        drawRoundRect(color = caliperColor, topLeft = Offset(rightDiscX - 1f, discY - 4f), size = Size(3.5f, 8f), cornerRadius = CornerRadius(1.2f, 1.2f))

        // Gold Inverted Telescopic Fork Stanchions connecting Triple Clamp down to Front Axle
        drawLine(
            color = Color(0xFFFFD700),
            start = Offset(-11f + cowlLean, -28f),
            end = Offset(leftDiscX, discY),
            strokeWidth = 3.2f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFFFD700),
            start = Offset(11f + cowlLean, -28f),
            end = Offset(rightDiscX, discY),
            strokeWidth = 3.2f,
            cap = StrokeCap.Round
        )

        // Aerodynamic Carbon Front Fender / Mudguard
        drawRoundRect(
            brush = galaxyBodyBrush,
            topLeft = Offset(frontWheelX - 6.5f, frontWheelY - 2f),
            size = Size(13f, 16f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // 7.6 Billet Rearset Footpegs & Rider's Racing Boots
        val leftPegX = -15f + leanOffset * 0.35f
        val rightPegX = 15f + leanOffset * 0.35f
        val pegY = -12f

        // Aluminum Rearset Brackets & Pegs
        drawLine(color = Color(0xFFCBD5E1), start = Offset(leftPegX - 4f, pegY), end = Offset(leftPegX, pegY), strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(color = Color(0xFFCBD5E1), start = Offset(rightPegX, pegY), end = Offset(rightPegX + 4f, pegY), strokeWidth = 3f, cap = StrokeCap.Round)

        // Rider's Racing Boots firmly planted on rearsets
        drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(leftPegX - 6.5f, pegY - 4f), size = Size(7f, 11f), cornerRadius = CornerRadius(2.5f, 2.5f))
        drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(rightPegX - 0.5f, pegY - 4f), size = Size(7f, 11f), cornerRadius = CornerRadius(2.5f, 2.5f))
        // Titanium Heel Plate & Toe Slider
        drawCircle(color = Color(0xFFE2E8F0), radius = 1.6f, center = Offset(leftPegX - 5f, pegY + 5f))
        drawCircle(color = Color(0xFFE2E8F0), radius = 1.6f, center = Offset(rightPegX + 5f, pegY + 5f))

        // 8. Front Cockpit Fairing, Windscreen & Clip-on Handlebars
        // Front Fairing Body
        drawRoundRect(
            brush = galaxyBodyBrush,
            topLeft = Offset(-13f + cowlLean, -39f),
            size = Size(26f, 17f),
            cornerRadius = CornerRadius(3f, 3f)
        )
        // Aerodynamic Smoked Bubble Windscreen
        drawRoundRect(
            color = Color(0xFF0F172A).copy(alpha = 0.65f),
            topLeft = Offset(-9f + cowlLean, -45f),
            size = Size(18f, 10f),
            cornerRadius = CornerRadius(5f, 5f)
        )
        // Windscreen edge neon trace
        drawLine(
            color = GalaxyPink.copy(alpha = 0.8f),
            start = Offset(-8f + cowlLean, -44f),
            end = Offset(8f + cowlLean, -44f),
            strokeWidth = 1.2f
        )

        // Inverted Telescopic Front Fork Crowns with Gold-Anodized Caps
        drawCircle(color = Color(0xFFFFD700), radius = 2.5f, center = Offset(-11f + cowlLean, -29f))
        drawCircle(color = Color(0xFFFFD700), radius = 2.5f, center = Offset(11f + cowlLean, -29f))

        // Billet Aluminum Triple Clamp / Top Yoke
        drawLine(
            color = Color(0xFFCBD5E1),
            start = Offset(-12f + cowlLean, -27f),
            end = Offset(12f + cowlLean, -27f),
            strokeWidth = 2.5f
        )
        drawCircle(color = Color(0xFFF8FAFC), radius = 2.2f, center = Offset(0f + cowlLean, -27f))

        // Digital TFT Instrument Cluster inside Cockpit
        drawRoundRect(
            color = Color.Black,
            topLeft = Offset(-6f + cowlLean, -35f),
            size = Size(12f, 7f),
            cornerRadius = CornerRadius(1.5f, 1.5f)
        )
        drawCircle(color = GalaxyCyan, radius = 1.8f, center = Offset(-2.5f + cowlLean, -32f))
        drawCircle(color = GalaxyPink, radius = 1.8f, center = Offset(2.5f + cowlLean, -32f))

        // Handlebars Bar Tubes (Synchronized with Rider Hands)
        val steerDelta = frontWheelSteer * 4.5f
        val leftGripX = -24f + steerDelta
        val rightGripX = 24f + steerDelta
        val gripY = -27f

        drawLine(
            color = Color(0xFFCBD5E1),
            start = Offset(leftGripX, gripY),
            end = Offset(rightGripX, gripY),
            strokeWidth = 3.6f,
            cap = StrokeCap.Round
        )
        // Rubber Diamond-Pattern Grips
        drawLine(
            color = Color(0xFF09090B),
            start = Offset(leftGripX - 2f, gripY),
            end = Offset(leftGripX + 4f, gripY),
            strokeWidth = 5.2f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF09090B),
            start = Offset(rightGripX - 4f, gripY),
            end = Offset(rightGripX + 2f, gripY),
            strokeWidth = 5.2f,
            cap = StrokeCap.Round
        )
        // Levers (Clutch on Left, Front Brake on Right)
        drawLine(color = Color(0xFFE2E8F0), start = Offset(leftGripX + 4f, gripY + 1f), end = Offset(leftGripX - 2f, gripY + 3f), strokeWidth = 1.8f)
        drawLine(color = Color(0xFFE2E8F0), start = Offset(rightGripX - 4f, gripY + 1f), end = Offset(rightGripX + 2f, gripY + 3f), strokeWidth = 1.8f)
        // Amber Brake Fluid Reservoir
        drawCircle(color = Color(0xFF1E293B), radius = 2.4f, center = Offset(rightGripX - 7f, gripY - 4f))
        drawCircle(color = Color(0xFFF59E0B), radius = 1.4f, center = Offset(rightGripX - 7f, gripY - 4f))

        // 9. DYNAMIC HUMAN RIDER (Seated Anatomically, Hands Connected to Bars, Dynamic Posture)
        val riderTorsoY = if (isBraking) -2.5f else if (isTucking) 2.5f else 0f
        val riderTorsoLean = leanOffset * 1.35f

        // Pelvis & Thighs gripping the tank
        drawOval(
            color = Color(0xFF141624),
            topLeft = Offset(-13f + leanOffset * 0.45f, -22f),
            size = Size(26f, 16f)
        )

        // Knee-Down Slider Touch in hard turns (MotoGP knee puck touches down towards asphalt)
        if (abs(player.leanAngleRad) > 0.12f) {
            val isLeaningLeft = player.leanAngleRad < 0f
            val puckX = if (isLeaningLeft) (-16f + leanOffset * 1.1f) else (16f + leanOffset * 1.1f)
            val puckY = -6f
            // Knee slider puck
            drawRoundRect(
                color = Color(0xFFE2E8F0),
                topLeft = Offset(puckX - 3.5f, puckY - 2.5f),
                size = Size(7f, 5f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            // Titanium spark dots if scraping knee
            if (abs(player.leanAngleRad) > 0.35f && player.speedKmh > 100f) {
                drawCircle(color = Color(0xFFFFD600), radius = 1.5f, center = Offset(puckX, puckY + 3f))
                drawCircle(color = Color.White, radius = 1f, center = Offset(puckX - 2f, puckY + 4f))
            }
        }

        // Rider Torso & Armored Racing Leather Suit
        val torsoX = riderTorsoLean
        val torsoY = -27f + riderTorsoY
        drawOval(
            color = Color(0xFF121422),
            topLeft = Offset(torsoX - 16f, torsoY),
            size = Size(32f, 28f)
        )
        // Aerodynamic Speed Hump on the spine
        drawOval(
            color = Color(0xFF090A10),
            topLeft = Offset(torsoX - 6f, torsoY + 2f),
            size = Size(12f, 18f)
        )
        // Galaxy Armored Suit Shoulders
        val leftShoulderX = torsoX - 14f
        val rightShoulderX = torsoX + 14f
        val shoulderY = torsoY + 2f
        drawCircle(color = GalaxyPink, radius = 4f, center = Offset(leftShoulderX, shoulderY))
        drawCircle(color = GalaxyCyan, radius = 4f, center = Offset(rightShoulderX, shoulderY))

        // 10. ARTICULATED ARMS (NATURALLY CONNECTING SHOULDERS TO HANDLEBAR GRIPS!)
        val elbowTuck = if (isTucking) -2.5f else 0f
        val leftElbowX = leftShoulderX - 7f - elbowTuck
        val leftElbowY = shoulderY + 4f
        val rightElbowX = rightShoulderX + 7f + elbowTuck
        val rightElbowY = shoulderY + 4f

        // Left Upper Arm & Forearm to Left Grip
        drawLine(
            color = Color(0xFF1E2032),
            start = Offset(leftShoulderX, shoulderY),
            end = Offset(leftElbowX, leftElbowY),
            strokeWidth = 6.2f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF181A2A),
            start = Offset(leftElbowX, leftElbowY),
            end = Offset(leftGripX + 1f, gripY),
            strokeWidth = 5.2f,
            cap = StrokeCap.Round
        )
        // Left Titanium Elbow Slider
        drawCircle(color = Color(0xFFCBD5E1), radius = 2.4f, center = Offset(leftElbowX, leftElbowY))
        // Left Gloved Hand wrapped firmly on handlebar grip
        drawCircle(color = Color(0xFF0F172A), radius = 4f, center = Offset(leftGripX, gripY))
        drawCircle(color = GalaxyPink, radius = 2f, center = Offset(leftGripX, gripY))

        // Right Upper Arm & Forearm to Right Grip
        drawLine(
            color = Color(0xFF1E2032),
            start = Offset(rightShoulderX, shoulderY),
            end = Offset(rightElbowX, rightElbowY),
            strokeWidth = 6.2f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF181A2A),
            start = Offset(rightElbowX, rightElbowY),
            end = Offset(rightGripX - 1f, gripY),
            strokeWidth = 5.2f,
            cap = StrokeCap.Round
        )
        // Right Titanium Elbow Slider
        drawCircle(color = Color(0xFFCBD5E1), radius = 2.4f, center = Offset(rightElbowX, rightElbowY))
        // Right Gloved Hand wrapped firmly on throttle grip
        drawCircle(color = Color(0xFF0F172A), radius = 4f, center = Offset(rightGripX, gripY))
        drawCircle(color = GalaxyCyan, radius = 2f, center = Offset(rightGripX, gripY))

        // 11. High-Detail Rider Helmet with Visor & Aerodynamic Diffuser
        val helmetX = torsoX
        val helmetY = torsoY - 8f
        // Outer Shell
        drawCircle(color = Color(0xFF0F0C1E), radius = 13.8f, center = Offset(helmetX, helmetY))
        drawCircle(brush = galaxyBodyBrush, radius = 12.2f, center = Offset(helmetX, helmetY))
        // Rear spoiler / teardrop diffuser
        drawPath(
            path = Path().apply {
                moveTo(helmetX - 7f, helmetY + 7f)
                lineTo(helmetX + 7f, helmetY + 7f)
                lineTo(helmetX, helmetY + 12f)
                close()
            },
            color = Color(0xFF090A10)
        )
        // Iridescent Mirror Visor with Cosmic Horizon Reflections
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(GalaxyCyan, GalaxyPink, GalaxyViolet)),
            topLeft = Offset(helmetX - 7.5f, helmetY - 5.5f),
            size = Size(15f, 7f),
            cornerRadius = CornerRadius(2.5f, 2.5f)
        )
        // Specular Glint
        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = Offset(helmetX - 5.5f, helmetY - 4.5f),
            end = Offset(helmetX + 3.5f, helmetY - 4.5f),
            strokeWidth = 1.2f
        )

        // 12. DUAL FUNCTIONAL REARVIEW MIRRORS (Live Rear Traffic Tracking!)
        val leftMirrorX = -27f
        val rightMirrorX = 27f
        val mirrorY = -38f

        drawLine(color = Color(0xFF64748B), start = Offset(-18f, -27f), end = Offset(leftMirrorX, mirrorY), strokeWidth = 2f)
        drawLine(color = Color(0xFF64748B), start = Offset(18f, -27f), end = Offset(rightMirrorX, mirrorY), strokeWidth = 2f)

        val leftTrafficBehind = racers.any { !it.isPlayer && (it.totalRaceDistance - player.totalRaceDistance) in -160f..-2f && it.posX < player.posX } ||
                obstacles.any { (it.distanceMeters - player.totalRaceDistance) in -160f..-2f && it.lane < player.posX }

        val rightTrafficBehind = racers.any { !it.isPlayer && (it.totalRaceDistance - player.totalRaceDistance) in -160f..-2f && it.posX >= player.posX } ||
                obstacles.any { (it.distanceMeters - player.totalRaceDistance) in -160f..-2f && it.lane >= player.posX }

        // Left Mirror
        drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(leftMirrorX - 10f, mirrorY - 7f), size = Size(20f, 14f), cornerRadius = CornerRadius(4f, 4f))
        drawRoundRect(color = Color(0xFF1E293B), topLeft = Offset(leftMirrorX - 8.5f, mirrorY - 5.5f), size = Size(17f, 11f), cornerRadius = CornerRadius(3f, 3f))
        drawLine(color = Color(0xFF334155), start = Offset(leftMirrorX - 8f, mirrorY), end = Offset(leftMirrorX + 8f, mirrorY), strokeWidth = 1f)
        if (leftTrafficBehind) {
            val pulse = ((System.currentTimeMillis() % 400) / 400f) * 0.4f + 0.6f
            drawCircle(color = Color(0xFFFFFF00).copy(alpha = pulse), radius = 3.5f, center = Offset(leftMirrorX - 3f, mirrorY + 1f))
            drawCircle(color = Color.White, radius = 2f, center = Offset(leftMirrorX - 3f, mirrorY + 1f))
        }

        // Right Mirror
        drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(rightMirrorX - 10f, mirrorY - 7f), size = Size(20f, 14f), cornerRadius = CornerRadius(4f, 4f))
        drawRoundRect(color = Color(0xFF1E293B), topLeft = Offset(rightMirrorX - 8.5f, mirrorY - 5.5f), size = Size(17f, 11f), cornerRadius = CornerRadius(3f, 3f))
        drawLine(color = Color(0xFF334155), start = Offset(rightMirrorX - 8f, mirrorY), end = Offset(rightMirrorX + 8f, mirrorY), strokeWidth = 1f)
        if (rightTrafficBehind) {
            val pulse = ((System.currentTimeMillis() % 400) / 400f) * 0.4f + 0.6f
            drawCircle(color = Color(0xFFFFFF00).copy(alpha = pulse), radius = 3.5f, center = Offset(rightMirrorX + 3f, mirrorY + 1f))
            drawCircle(color = Color.White, radius = 2f, center = Offset(rightMirrorX + 3f, mirrorY + 1f))
        }

        // 13. LED Tail Light Blade & Amber Indicators (FLARES BRIGHT CRIMSON ON BRAKING!)
        val brakeLightColor = if (isBraking) Color(0xFFFF0033) else Color(0xFFFF1E44)
        val brakeLightHeight = if (isBraking) 8f else 6f
        drawRoundRect(
            color = brakeLightColor,
            topLeft = Offset(-8f + leanOffset * 0.45f, -10f),
            size = Size(16f, brakeLightHeight),
            cornerRadius = CornerRadius(2f, 2f)
        )
        if (isBraking) {
            // Bright red brake glow flare
            drawOval(
                color = Color(0xFFFF0033).copy(alpha = 0.5f),
                topLeft = Offset(-18f + leanOffset * 0.45f, -14f),
                size = Size(36f, 16f)
            )
        }
        drawRoundRect(
            color = Color(0xFFFFB703),
            topLeft = Offset(-16f + leanOffset * 0.45f, -9f),
            size = Size(6f, 5f),
            cornerRadius = CornerRadius(1.5f, 1.5f)
        )
        drawRoundRect(
            color = Color(0xFFFFB703),
            topLeft = Offset(10f + leanOffset * 0.45f, -9f),
            size = Size(6f, 5f),
            cornerRadius = CornerRadius(1.5f, 1.5f)
        )
    }
}

/**
 * 8. First-Person Helmet / Cockpit Camera View
 * - Looking directly through the aerodynamic double-bubble smoked windscreen onto the highway.
 * - Upper triple clamp, clip-on bars, and rider's gloved hands holding the grips in first person.
 * - High-definition digital TFT instrument dash with live RPM tachometer bar, digital gear, speed readout,
 *   and shift light LEDs.
 * - Dual wide-angle mirrors with live trailing traffic reflection.
 */
private fun DrawScope.drawFirstPersonCockpitView(
    player: RacerState,
    racers: List<RacerState>,
    obstacles: List<HighwayObstacle>,
    width: Float,
    height: Float,
    bikeY: Float,
    isNitro: Boolean,
    rumbleOffsetX: Float
) {
    val roadHalfW = width * 0.58f
    val lateralNorm = player.posX.coerceIn(-0.96f, 0.96f)
    val cockpitCenterX = (width / 2f) + (lateralNorm * roadHalfW * 0.92f) + (player.leanAngleRad * 16f) + rumbleOffsetX

    val leanRad = player.leanAngleRad
    val speedKmh = player.speedKmh
    val simulatedRpm = ((player.speedKmh / 280f) * 12500f + 1500f).coerceIn(1200f, 14000f)
    val rpmRatio = (simulatedRpm / 14000f).coerceIn(0f, 1f)
    val isBraking = player.brakeRatio > 0.15f

    withTransform({
        translate(left = cockpitCenterX, top = bikeY)
        rotate(degrees = leanRad * 5f, pivot = Offset.Zero)
    }) {
        // 1. Aerodynamic Double-Bubble Smoked Windscreen Arch
        val screenW = width * 0.62f
        val screenH = height * 0.32f

        val windshieldPath = Path().apply {
            moveTo(-screenW * 0.42f, 0f)
            cubicTo(
                -screenW * 0.38f, -screenH * 0.8f,
                screenW * 0.38f, -screenH * 0.8f,
                screenW * 0.42f, 0f
            )
            close()
        }
        drawPath(
            path = windshieldPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0F172A).copy(alpha = 0.55f), Color(0xFF020617).copy(alpha = 0.85f)),
                startY = -screenH * 0.8f,
                endY = 0f
            )
        )
        // Windshield Neon Border Trace
        drawPath(
            path = windshieldPath,
            color = GalaxyPink.copy(alpha = 0.65f),
            style = Stroke(width = 3f)
        )

        // Speed Wind Streaks on Visor/Windscreen
        if (speedKmh > 80f) {
            val streakAlpha = (speedKmh / 280f).coerceIn(0.2f, 0.7f)
            val stOff = (System.currentTimeMillis() * 0.5f) % 40f
            drawLine(
                color = Color.White.copy(alpha = streakAlpha),
                start = Offset(-screenW * 0.2f, -screenH * 0.6f + stOff),
                end = Offset(-screenW * 0.25f, -screenH * 0.3f + stOff),
                strokeWidth = 1.5f
            )
            drawLine(
                color = Color.White.copy(alpha = streakAlpha),
                start = Offset(screenW * 0.2f, -screenH * 0.6f + stOff),
                end = Offset(screenW * 0.25f, -screenH * 0.3f + stOff),
                strokeWidth = 1.5f
            )
        }

        // 2. Center Digital TFT Racing Instrument Cluster Screen
        val dashW = 160f
        val dashH = 88f
        val dashY = -screenH * 0.48f

        // Dash Bezel
        drawRoundRect(
            color = Color(0xFF0B0F19),
            topLeft = Offset(-dashW / 2f, dashY),
            size = Size(dashW, dashH),
            cornerRadius = CornerRadius(10f, 10f)
        )
        drawRoundRect(
            color = Color(0xFF334155),
            topLeft = Offset(-dashW / 2f, dashY),
            size = Size(dashW, dashH),
            cornerRadius = CornerRadius(10f, 10f),
            style = Stroke(width = 2.5f)
        )

        // Dash Screen Glass
        drawRoundRect(
            color = Color(0xFF020617),
            topLeft = Offset(-dashW / 2f + 6f, dashY + 6f),
            size = Size(dashW - 12f, dashH - 12f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // Live RPM Tachometer Bar (Color Shifts: Green -> Yellow -> Redline)
        val tachW = dashW - 24f
        val tachFillW = tachW * rpmRatio
        val tachColor = when {
            rpmRatio > 0.85f -> Color(0xFFFF1E44)
            rpmRatio > 0.65f -> Color(0xFFFFB703)
            else -> Color(0xFF00E676)
        }
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(-dashW / 2f + 12f, dashY + 12f),
            size = Size(tachW, 10f),
            cornerRadius = CornerRadius(3f, 3f)
        )
        drawRoundRect(
            color = tachColor,
            topLeft = Offset(-dashW / 2f + 12f, dashY + 12f),
            size = Size(tachFillW, 10f),
            cornerRadius = CornerRadius(3f, 3f)
        )

        // Shift Light LEDs across the top
        for (led in 0..4) {
            val ledX = -dashW / 2f + 20f + (led * 28f)
            val ledColor = if (rpmRatio > (0.6f + led * 0.08f)) Color(0xFF00F0FF) else Color(0xFF1E293B)
            drawCircle(color = ledColor, radius = 3.5f, center = Offset(ledX, dashY + 28f))
        }

        // Digital Gear Display ("1" .. "6")
        drawCircle(color = GalaxyPink.copy(alpha = 0.25f), radius = 14f, center = Offset(-dashW / 4f, dashY + 54f))
        drawCircle(color = GalaxyPink, radius = 14f, center = Offset(-dashW / 4f, dashY + 54f), style = Stroke(width = 2f))

        // Speedometer Digital Value Arc (Cyan Arc)
        drawArc(
            color = GalaxyCyan,
            startAngle = 135f,
            sweepAngle = (speedKmh / 300f).coerceIn(0f, 1f) * 270f,
            useCenter = false,
            topLeft = Offset(dashW / 4f - 18f, dashY + 36f),
            size = Size(36f, 36f),
            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
        )

        // 3. Billet Aluminum Triple Clamp Yoke & Steering Stem Hex Nut
        val yokeW = width * 0.72f
        val yokeY = -22f

        drawLine(
            color = Color(0xFF94A3B8),
            start = Offset(-yokeW / 2f, yokeY),
            end = Offset(yokeW / 2f, yokeY),
            strokeWidth = 14f,
            cap = StrokeCap.Round
        )
        // Center Steering Stem Nut
        drawCircle(color = Color(0xFFF1F5F9), radius = 10f, center = Offset(0f, yokeY))
        drawCircle(color = Color(0xFF0F172A), radius = 5f, center = Offset(0f, yokeY))

        // Left and Right Fork Caps with Gold Anodized Adjusters
        drawCircle(color = Color(0xFFFFD700), radius = 9f, center = Offset(-yokeW * 0.38f, yokeY))
        drawCircle(color = Color(0xFF2563EB), radius = 4.5f, center = Offset(-yokeW * 0.38f, yokeY))

        drawCircle(color = Color(0xFFFFD700), radius = 9f, center = Offset(yokeW * 0.38f, yokeY))
        drawCircle(color = Color(0xFF2563EB), radius = 4.5f, center = Offset(yokeW * 0.38f, yokeY))

        // 4. Clip-on Handlebars, Grips & Levers
        val barLeftX = -yokeW * 0.44f
        val barRightX = yokeW * 0.44f
        val barY = yokeY + 12f

        // Rubber Diamond-Pattern Grips
        drawLine(
            color = Color(0xFF18181B),
            start = Offset(barLeftX - 60f, barY + 18f),
            end = Offset(barLeftX, barY),
            strokeWidth = 22f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF18181B),
            start = Offset(barRightX + 60f, barY + 18f),
            end = Offset(barRightX, barY),
            strokeWidth = 22f,
            cap = StrokeCap.Round
        )

        // CNC Aluminum Levers (Clutch on Left, Front Brake on Right)
        val brakeLeverSqueeze = if (isBraking) -8f else 0f
        drawLine(
            color = Color(0xFFCBD5E1),
            start = Offset(barLeftX, barY - 4f),
            end = Offset(barLeftX - 52f, barY + 6f),
            strokeWidth = 5.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFCBD5E1),
            start = Offset(barRightX, barY - 4f),
            end = Offset(barRightX + 52f, barY + 6f + brakeLeverSqueeze),
            strokeWidth = 5.5f,
            cap = StrokeCap.Round
        )

        // Amber Brembo Front Brake Master Cylinder Reservoir (Right)
        drawCircle(color = Color(0xFF1E293B), radius = 9f, center = Offset(barRightX - 16f, barY - 14f))
        drawCircle(color = Color(0xFFF59E0B), radius = 6f, center = Offset(barRightX - 16f, barY - 14f))

        // 5. RIDER'S GLOVED HANDS GRIPPING HANDLEBARS IN FIRST PERSON!
        // Left gloved hand (Black leather with pink knuckle armor)
        val leftHandX = barLeftX - 30f
        val leftHandY = barY + 10f
        drawOval(
            color = Color(0xFF0F172A),
            topLeft = Offset(leftHandX - 18f, leftHandY - 14f),
            size = Size(36f, 28f)
        )
        drawCircle(color = GalaxyPink, radius = 5.5f, center = Offset(leftHandX, leftHandY - 4f))

        // Right gloved hand (Black leather with cyan knuckle armor, gripping throttle)
        val rightHandX = barRightX + 30f
        val rightHandY = barY + 10f
        drawOval(
            color = Color(0xFF0F172A),
            topLeft = Offset(rightHandX - 18f, rightHandY - 14f),
            size = Size(36f, 28f)
        )
        drawCircle(color = GalaxyCyan, radius = 5.5f, center = Offset(rightHandX, rightHandY - 4f))

        // 6. Dual Functional Fairing-Mounted Rearview Mirrors
        val mirrorY = -screenH * 0.72f
        val leftMirrorX = -screenW * 0.44f
        val rightMirrorX = screenW * 0.44f

        val leftTrafficBehind = racers.any { !it.isPlayer && (it.totalRaceDistance - player.totalRaceDistance) in -160f..-2f && it.posX < player.posX } ||
                obstacles.any { (it.distanceMeters - player.totalRaceDistance) in -160f..-2f && it.lane < player.posX }

        val rightTrafficBehind = racers.any { !it.isPlayer && (it.totalRaceDistance - player.totalRaceDistance) in -160f..-2f && it.posX >= player.posX } ||
                obstacles.any { (it.distanceMeters - player.totalRaceDistance) in -160f..-2f && it.lane >= player.posX }

        // Left Mirror Housing & Glass
        drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(leftMirrorX - 32f, mirrorY - 20f), size = Size(64f, 40f), cornerRadius = CornerRadius(10f, 10f))
        drawRoundRect(color = Color(0xFF1E293B), topLeft = Offset(leftMirrorX - 28f, mirrorY - 16f), size = Size(56f, 32f), cornerRadius = CornerRadius(8f, 8f))
        drawLine(color = Color(0xFF334155), start = Offset(leftMirrorX - 26f, mirrorY), end = Offset(leftMirrorX + 26f, mirrorY), strokeWidth = 2f)
        if (leftTrafficBehind) {
            val pulse = ((System.currentTimeMillis() % 400) / 400f) * 0.4f + 0.6f
            drawCircle(color = Color(0xFFFFFF00).copy(alpha = pulse), radius = 6f, center = Offset(leftMirrorX - 8f, mirrorY + 3f))
            drawCircle(color = Color.White, radius = 3.5f, center = Offset(leftMirrorX - 8f, mirrorY + 3f))
        }

        // Right Mirror Housing & Glass
        drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(rightMirrorX - 32f, mirrorY - 20f), size = Size(64f, 40f), cornerRadius = CornerRadius(10f, 10f))
        drawRoundRect(color = Color(0xFF1E293B), topLeft = Offset(rightMirrorX - 28f, mirrorY - 16f), size = Size(56f, 32f), cornerRadius = CornerRadius(8f, 8f))
        drawLine(color = Color(0xFF334155), start = Offset(rightMirrorX - 26f, mirrorY), end = Offset(rightMirrorX + 26f, mirrorY), strokeWidth = 2f)
        if (rightTrafficBehind) {
            val pulse = ((System.currentTimeMillis() % 400) / 400f) * 0.4f + 0.6f
            drawCircle(color = Color(0xFFFFFF00).copy(alpha = pulse), radius = 6f, center = Offset(rightMirrorX + 8f, mirrorY + 3f))
            drawCircle(color = Color.White, radius = 3.5f, center = Offset(rightMirrorX + 8f, mirrorY + 3f))
        }
    }
}

/**
 * Dynamic "OVERTAKE!" Speed Indicator Pop
 */
private fun DrawScope.drawOvertakeBanner(
    width: Float,
    y: Float,
    alpha: Float,
    racerName: String,
    isNitro: Boolean
) {
    val bannerW = 200f
    val bannerH = 40f
    val x = (width - bannerW) / 2f

    drawRoundRect(
        color = Color(0xFF0B0F19).copy(alpha = 0.92f * alpha),
        topLeft = Offset(x, y),
        size = Size(bannerW, bannerH),
        cornerRadius = CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = if (isNitro) Color(0xFFB5179E).copy(alpha = alpha) else Color(0xFF00F0FF).copy(alpha = alpha),
        topLeft = Offset(x, y),
        size = Size(bannerW, bannerH),
        cornerRadius = CornerRadius(10f, 10f),
        style = Stroke(width = 2f)
    )

    // Glowing Chevrons
    val chevronColor = Color(0xFFFFD600).copy(alpha = alpha)
    drawLine(
        color = chevronColor,
        start = Offset(x + 14f, y + 12f),
        end = Offset(x + 22f, y + 20f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = chevronColor,
        start = Offset(x + 22f, y + 20f),
        end = Offset(x + 14f, y + 28f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )

    drawLine(
        color = chevronColor,
        start = Offset(x + bannerW - 14f, y + 12f),
        end = Offset(x + bannerW - 22f, y + 20f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = chevronColor,
        start = Offset(x + bannerW - 22f, y + 20f),
        end = Offset(x + bannerW - 14f, y + 28f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
}

/**
 * Visual Particle Effects Layer:
 * High-Speed Slipstream Draft Trails, Overtake Sparks & Stars, Impact Shocks and Smoke.
 */
private fun DrawScope.drawActiveVisualParticles(
    particles: List<VisualCanvasParticle>,
    bikeCenterX: Float,
    bikeCenterY: Float,
    isFirstPerson: Boolean
) {
    if (particles.isEmpty()) return

    particles.forEach { p ->
        val lifeFraction = (p.currentLife / p.maxLife).coerceIn(0f, 1f)
        val alpha = (lifeFraction * 0.95f).coerceIn(0f, 1f)
        val screenX = bikeCenterX + p.x
        val screenY = bikeCenterY + p.y

        when (p.type) {
            ParticleVisualType.SPARK -> {
                // High-velocity incandescent collision / scrape sparks
                val sparkLength = (p.size * 3.5f * (1f + (1f - lifeFraction))).coerceAtLeast(4f)
                val sparkColor = p.color.copy(alpha = alpha)
                val coreColor = Color.White.copy(alpha = alpha)

                // Outer fiery glow streak
                drawLine(
                    color = sparkColor,
                    start = Offset(screenX, screenY),
                    end = Offset(screenX - (p.vx * 0.035f), screenY - (p.vy * 0.035f)),
                    strokeWidth = p.size,
                    cap = StrokeCap.Round
                )
                // Hot core
                drawCircle(
                    color = coreColor,
                    radius = (p.size * 0.5f).coerceAtLeast(1.5f),
                    center = Offset(screenX, screenY)
                )
            }
            ParticleVisualType.SLIPSTREAM_STREAK -> {
                // Aerodynamic wind ribbon whistling along bike flanks
                val streakLength = (40f + (1f - lifeFraction) * 60f)
                val streakAlpha = (alpha * 0.75f).coerceIn(0f, 1f)
                val streakBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        p.color.copy(alpha = streakAlpha),
                        Color.White.copy(alpha = streakAlpha * 0.8f),
                        Color.Transparent
                    ),
                    startY = screenY - streakLength,
                    endY = screenY + (streakLength * 0.4f)
                )
                drawLine(
                    brush = streakBrush,
                    start = Offset(screenX, screenY - streakLength),
                    end = Offset(screenX + (p.vx * 0.04f), screenY + 15f),
                    strokeWidth = p.size,
                    cap = StrokeCap.Round
                )
            }
            ParticleVisualType.COMBO_STAR -> {
                // 4-Pointed Near-Miss Combo Star
                val starSize = p.size * (0.8f + (1f - lifeFraction) * 0.5f)
                val starColor = p.color.copy(alpha = alpha)
                val centerColor = Color.White.copy(alpha = alpha)

                // Vertical & Horizontal rays
                drawLine(
                    color = starColor,
                    start = Offset(screenX, screenY - starSize),
                    end = Offset(screenX, screenY + starSize),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = starColor,
                    start = Offset(screenX - starSize, screenY),
                    end = Offset(screenX + starSize, screenY),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
                // Center diamond / glow core
                drawCircle(
                    color = centerColor,
                    radius = (starSize * 0.35f).coerceAtLeast(2f),
                    center = Offset(screenX, screenY)
                )
            }
            ParticleVisualType.SHOCKWAVE_RING -> {
                // Expanding perspective road shockwave
                val ringRadius = p.size * 3.5f
                val ringAlpha = (alpha * 0.85f).coerceIn(0f, 1f)
                drawOval(
                    color = p.color.copy(alpha = ringAlpha),
                    topLeft = Offset(screenX - ringRadius, screenY - (ringRadius * 0.35f)),
                    size = Size(ringRadius * 2f, ringRadius * 0.7f),
                    style = Stroke(width = (3.5f * lifeFraction).coerceAtLeast(1f))
                )
            }
            ParticleVisualType.SMOKE_PUFF -> {
                // Billowing road friction smoke puff
                val puffRadius = p.size * (1.2f + (1f - lifeFraction) * 1.8f)
                val smokeAlpha = (alpha * 0.45f).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            p.color.copy(alpha = smokeAlpha),
                            p.color.copy(alpha = smokeAlpha * 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(screenX, screenY),
                        radius = puffRadius
                    ),
                    radius = puffRadius,
                    center = Offset(screenX, screenY)
                )
            }
            ParticleVisualType.DEBRIS_CHUNK -> {
                // Tumbling asphalt/rubber chunk
                val debrisSize = p.size * 1.2f
                drawRoundRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(screenX - debrisSize / 2f, screenY - debrisSize / 2f),
                    size = Size(debrisSize, debrisSize * 0.75f),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
            }
        }
    }
}
