package com.example.game.tracks

import com.example.game.engine.Vector2D
import kotlin.math.*

data class CheckpointGate(
    val index: Int,
    val center: Vector2D,
    val leftBound: Vector2D,
    val rightBound: Vector2D,
    val direction: Vector2D,
    val isFinishLine: Boolean = false
)

data class TrackLayout(
    val id: String,
    val name: String,
    val waypoints: List<Vector2D>,
    val roadWidth: Float,
    val totalLength: Float,
    val checkpointGates: List<CheckpointGate>,
    val asphaltColorHex: Long = 0xFF181E2A,
    val curbColorHex1: Long = 0xFF00F0FF,
    val curbColorHex2: Long = 0xFFFF6B00,
    val groundColorHex: Long = 0xFF0A0D14
)

object TrackCatalog {

    fun getTrackLayout(trackId: String): TrackLayout {
        return when (trackId) {
            "track_desert_storm" -> buildDesertStormLayout()
            "track_mountain_edge" -> buildMountainEdgeLayout()
            "track_night_highway" -> buildNightHighwayLayout()
            "track_coastal_run" -> buildCoastalRunLayout()
            else -> buildCityRushLayout() // Default
        }
    }

    private fun buildCityRushLayout(): TrackLayout {
        // High-speed downtown circuit with wide apexes and spacious chicanes
        val rawPoints = listOf(
            Vector2D(400f, 400f),
            Vector2D(1300f, 400f),
            Vector2D(2000f, 650f),
            Vector2D(2400f, 1200f),
            Vector2D(2300f, 1900f),
            Vector2D(1700f, 2300f),
            Vector2D(1300f, 1900f),
            Vector2D(1000f, 2000f),
            Vector2D(700f, 2400f),
            Vector2D(300f, 2000f),
            Vector2D(180f, 1200f),
            Vector2D(220f, 650f)
        )
        return generateSmoothCircuit(
            id = "track_city_rush",
            name = "City Rush",
            rawPoints = rawPoints,
            roadWidth = 230f,
            asphaltColorHex = 0xFF141A24,
            curbColor1 = 0xFF00F0FF,
            curbColor2 = 0xFFFFFFFF,
            groundColor = 0xFF080B10
        )
    }

    private fun buildDesertStormLayout(): TrackLayout {
        val rawPoints = listOf(
            Vector2D(500f, 500f),
            Vector2D(1500f, 450f),
            Vector2D(2200f, 800f),
            Vector2D(2600f, 1500f),
            Vector2D(2300f, 2400f),
            Vector2D(1500f, 2500f),
            Vector2D(1100f, 2200f),
            Vector2D(700f, 1800f),
            Vector2D(400f, 2100f),
            Vector2D(180f, 1500f),
            Vector2D(220f, 850f)
        )
        return generateSmoothCircuit(
            id = "track_desert_storm",
            name = "Desert Storm",
            rawPoints = rawPoints,
            roadWidth = 220f,
            asphaltColorHex = 0xFF221A16,
            curbColor1 = 0xFFFF6B00,
            curbColor2 = 0xFFFFD600,
            groundColor = 0xFF160E0A
        )
    }

    private fun buildMountainEdgeLayout(): TrackLayout {
        val rawPoints = listOf(
            Vector2D(400f, 400f),
            Vector2D(1100f, 400f),
            Vector2D(1650f, 750f),
            Vector2D(1300f, 1200f),
            Vector2D(1950f, 1500f),
            Vector2D(2350f, 2050f),
            Vector2D(1800f, 2550f),
            Vector2D(1200f, 2300f),
            Vector2D(850f, 1700f),
            Vector2D(400f, 1500f),
            Vector2D(180f, 950f)
        )
        return generateSmoothCircuit(
            id = "track_mountain_edge",
            name = "Mountain Edge",
            rawPoints = rawPoints,
            roadWidth = 200f,
            asphaltColorHex = 0xFF161424,
            curbColor1 = 0xFFB5179E,
            curbColor2 = 0xFF00F0FF,
            groundColor = 0xFF0B0A14
        )
    }

    private fun buildNightHighwayLayout(): TrackLayout {
        val rawPoints = listOf(
            Vector2D(400f, 400f),
            Vector2D(1650f, 400f),
            Vector2D(2500f, 850f),
            Vector2D(2700f, 1750f),
            Vector2D(2250f, 2500f),
            Vector2D(1300f, 2650f),
            Vector2D(550f, 2350f),
            Vector2D(180f, 1600f),
            Vector2D(220f, 850f)
        )
        return generateSmoothCircuit(
            id = "track_night_highway",
            name = "Night Highway",
            rawPoints = rawPoints,
            roadWidth = 230f,
            asphaltColorHex = 0xFF101C20,
            curbColor1 = 0xFF00E676,
            curbColor2 = 0xFF00F0FF,
            groundColor = 0xFF080F12
        )
    }

    private fun buildCoastalRunLayout(): TrackLayout {
        val rawPoints = listOf(
            Vector2D(400f, 400f),
            Vector2D(1700f, 500f),
            Vector2D(2550f, 950f),
            Vector2D(2750f, 1900f),
            Vector2D(2000f, 2550f),
            Vector2D(1400f, 2150f),
            Vector2D(850f, 2650f),
            Vector2D(320f, 2250f),
            Vector2D(160f, 1300f)
        )
        return generateSmoothCircuit(
            id = "track_coastal_run",
            name = "Coastal Run",
            rawPoints = rawPoints,
            roadWidth = 220f,
            asphaltColorHex = 0xFF1A1F2C,
            curbColor1 = 0xFFFFD600,
            curbColor2 = 0xFFFF1744,
            groundColor = 0xFF0A101D
        )
    }

    private fun generateSmoothCircuit(
        id: String,
        name: String,
        rawPoints: List<Vector2D>,
        roadWidth: Float,
        asphaltColorHex: Long,
        curbColor1: Long,
        curbColor2: Long,
        groundColor: Long
    ): TrackLayout {
        // Subdivide using Catmull-Rom spline interpolation to create silky smooth racing line
        val waypoints = mutableListOf<Vector2D>()
        val count = rawPoints.size
        val subdivisionsPerSegment = 16

        for (i in 0 until count) {
            val p0 = rawPoints[(i - 1 + count) % count]
            val p1 = rawPoints[i]
            val p2 = rawPoints[(i + 1) % count]
            val p3 = rawPoints[(i + 2) % count]

            for (step in 0 until subdivisionsPerSegment) {
                val t = step.toFloat() / subdivisionsPerSegment
                val t2 = t * t
                val t3 = t2 * t

                val x = 0.5f * ((2f * p1.x) +
                        (-p0.x + p2.x) * t +
                        (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * t2 +
                        (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * t3)

                val y = 0.5f * ((2f * p1.y) +
                        (-p0.y + p2.y) * t +
                        (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 +
                        (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3)

                waypoints.add(Vector2D(x, y))
            }
        }

        // Build Checkpoint Gates spaced evenly along the waypoints
        var totalLength = 0f
        val wpCount = waypoints.size
        for (i in 0 until wpCount) {
            val pA = waypoints[i]
            val pB = waypoints[(i + 1) % wpCount]
            totalLength += pA.distanceTo(pB)
        }

        val numberOfCheckpoints = 24
        val stepSize = wpCount / numberOfCheckpoints
        val checkpointGates = mutableListOf<CheckpointGate>()

        for (cpIdx in 0 until numberOfCheckpoints) {
            val wpIndex = (cpIdx * stepSize) % wpCount
            val center = waypoints[wpIndex]
            val next = waypoints[(wpIndex + 1) % wpCount]
            val dir = (next - center).normalized()
            val normal = Vector2D(-dir.y, dir.x)

            val halfWidth = roadWidth * 0.65f
            val left = center + normal * halfWidth
            val right = center - normal * halfWidth

            checkpointGates.add(
                CheckpointGate(
                    index = cpIdx,
                    center = center,
                    leftBound = left,
                    rightBound = right,
                    direction = dir,
                    isFinishLine = cpIdx == 0
                )
            )
        }

        return TrackLayout(
            id = id,
            name = name,
            waypoints = waypoints,
            roadWidth = roadWidth,
            totalLength = totalLength,
            checkpointGates = checkpointGates,
            asphaltColorHex = asphaltColorHex,
            curbColorHex1 = curbColor1,
            curbColorHex2 = curbColor2,
            groundColorHex = groundColor
        )
    }
}
