package com.example.game.environment

import androidx.compose.ui.graphics.Color

data class CityMilestone(
    val id: String,
    val name: String,
    val tagline: String,
    val famousFor: String,
    val emoji: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val landmarkName: String
)

object CityMilestoneCatalog {

    val CITIES = listOf(
        CityMilestone(
            id = "mumbai",
            name = "MUMBAI",
            tagline = "THE CITY OF DREAMS",
            famousFor = "Gateway of India & Bollywood",
            emoji = "🌊",
            primaryColor = Color(0xFF00F0FF),
            secondaryColor = Color(0xFFFFD600),
            landmarkName = "Sea Link Expressway"
        ),
        CityMilestone(
            id = "pune",
            name = "PUNE",
            tagline = "OXFORD OF THE EAST",
            famousFor = "IT & Historic Forts",
            emoji = "🎓",
            primaryColor = Color(0xFF7C4DFF),
            secondaryColor = Color(0xFF00E5FF),
            landmarkName = "Deccan High-Speed Bypass"
        ),
        CityMilestone(
            id = "nagpur",
            name = "NAGPUR",
            tagline = "THE ORANGE CITY",
            famousFor = "Famous Juicy Oranges & Tiger Reserves",
            emoji = "🍊",
            primaryColor = Color(0xFFFF6B00),
            secondaryColor = Color(0xFFFFD600),
            landmarkName = "Zero Mile Tiger Highway"
        ),
        CityMilestone(
            id = "hyderabad",
            name = "HYDERABAD",
            tagline = "CITY OF PEARLS",
            famousFor = "Charminar & World Famous Biryani",
            emoji = "💎",
            primaryColor = Color(0xFF00E676),
            secondaryColor = Color(0xFF00F0FF),
            landmarkName = "Cyberabad Outer Ring Road"
        ),
        CityMilestone(
            id = "jaipur",
            name = "JAIPUR",
            tagline = "THE PINK CITY",
            famousFor = "Hawa Mahal & Royal Rajput Palaces",
            emoji = "🏰",
            primaryColor = Color(0xFFFF1493),
            secondaryColor = Color(0xFFFFD700),
            landmarkName = "Amer Fort Royal Speedway"
        ),
        CityMilestone(
            id = "bengaluru",
            name = "BENGALURU",
            tagline = "SILICON & GARDEN CITY",
            famousFor = "Tech Capital & Lush Botanical Gardens",
            emoji = "💻",
            primaryColor = Color(0xFF00F0FF),
            secondaryColor = Color(0xFF76FF03),
            landmarkName = "Electronic City Elevated Flyover"
        ),
        CityMilestone(
            id = "chennai",
            name = "CHENNAI",
            tagline = "GATEWAY OF SOUTH",
            famousFor = "Marina Beach & Detroit of Asia Autos",
            emoji = "🚗",
            primaryColor = Color(0xFFFF9100),
            secondaryColor = Color(0xFFFF1744),
            landmarkName = "East Coast Highway"
        ),
        CityMilestone(
            id = "kolkata",
            name = "KOLKATA",
            tagline = "CITY OF JOY",
            famousFor = "Howrah Bridge & Heritage Yellow Cabs",
            emoji = "🚖",
            primaryColor = Color(0xFFFFD600),
            secondaryColor = Color(0xFF2979FF),
            landmarkName = "Vidyasagar Tollway"
        ),
        CityMilestone(
            id = "delhi",
            name = "DELHI",
            tagline = "THE CAPITAL CITY",
            famousFor = "India Gate, Red Fort & Dilwalon ki Dilli",
            emoji = "🏛️",
            primaryColor = Color(0xFFFF1744),
            secondaryColor = Color(0xFFFFAB00),
            landmarkName = "Yamuna Express Superway"
        ),
        CityMilestone(
            id = "goa",
            name = "GOA",
            tagline = "SUNSHINE BEACH CAPITAL",
            famousFor = "Golden Coastlines & Tropical Vibes",
            emoji = "🌴",
            primaryColor = Color(0xFF00E5FF),
            secondaryColor = Color(0xFFFFEA00),
            landmarkName = "Mandovi Coastal Boulevard"
        )
    )

    /**
     * Get the city corresponding to current race distance in meters.
     * Every 10 km (10,000 m) marks a new city territory.
     */
    fun getCityForDistance(distanceMeters: Float): CityMilestone {
        val segment10Km = (distanceMeters / 10_000f).toInt()
        val index = (segment10Km) % CITIES.size
        return CITIES[index.coerceAtLeast(0)]
    }

    /**
     * Returns true if player is currently in the milestone crossing zone (within 250 meters of a 10km boundary).
     */
    fun isCrossing10KmMilestone(distanceMeters: Float): Boolean {
        if (distanceMeters < 500f) return false
        val distInCurrent10Km = distanceMeters % 10_000f
        return distInCurrent10Km in 0f..350f || distInCurrent10Km in 9650f..10000f
    }

    enum class TurnType {
        STRAIGHT,
        GENTLE_LEFT,
        GENTLE_RIGHT,
        SHARP_LEFT,
        SHARP_RIGHT,
        S_CHICANE,
        HAIRPIN_LEFT,
        HAIRPIN_RIGHT
    }

    data class TurnWarning(
        val type: TurnType,
        val label: String,
        val chevronSymbol: String,
        val distanceToTurnMeters: Float,
        val intensity: Float // 0.0 to 1.0
    )

    /**
     * Procedural Multi-Harmonic Road Curvature Generator:
     * Calculates instantaneous curvature factor (-1.0 to +1.0) along infinite highway.
     * Combines distinct track zones, high-speed straights, sweeping curves,
     * S-chicanes, hairpins, and subtle organic highway micro-sways.
     */
    fun getRoadCurvature(distanceMeters: Float): Float {
        val segmentLength = 450f
        val segmentIndex = (distanceMeters / segmentLength).toInt()
        val progress = (distanceMeters % segmentLength) / segmentLength // 0.0 to 1.0

        // Multi-frequency harmonic micro-sway for organic realism
        val organicMicroSway = kotlin.math.sin(distanceMeters * 0.012f) * 0.06f

        val macroPattern = (segmentIndex % 12)
        val baseCurve = when (macroPattern) {
            0 -> 0.0f // High-Speed Interstate Straight Sprint
            1 -> -0.45f // Sweeping Gentle Left Bend
            2 -> -0.75f // Accelerating Wide Left Sweeper
            3 -> kotlin.math.sin(progress * Math.PI.toFloat() * 2f) * 0.82f // Technical S-Chicane (Left to Right)
            4 -> 0.60f // Sweeping Banked Right Turn
            5 -> 0.0f // Elevated Sea-Link Bridge Straight
            6 -> -0.88f // Sharp Mountain Apex Hairpin Left
            7 -> kotlin.math.sin(progress * Math.PI.toFloat() * 3f) * 0.70f // Rapid Double-Chicane
            8 -> 0.85f // Sharp Mountain Hairpin Right
            9 -> 0.40f // Gentle Flowing Right Curve
            10 -> kotlin.math.sin(progress * Math.PI.toFloat() * 2f) * -0.78f // S-Bend Transition (Right to Left)
            else -> 0.0f // Express Tunnel Straightaway
        }

        val blendedCurve = baseCurve * (0.35f + 0.65f * kotlin.math.sin(progress * Math.PI.toFloat())) + organicMicroSway
        return blendedCurve.coerceIn(-1.0f, 1.0f)
    }

    /**
     * Calculates the exact procedural horizontal projection offset for road slices,
     * roadside objects, animated trees, and traffic at relative distance [zRel] meters ahead of player.
     * Multi-samples the procedural curve profile ahead of the bike to form authentic pseudo-3D curves.
     */
    fun getProceduralRoadOffset(playerDistance: Float, zRel: Float, screenWidth: Float): Float {
        if (zRel <= 0.1f) return 0f

        val clampedZ = zRel.coerceIn(0f, 260f)
        val zNorm = clampedZ / 260f
        // Perspective quadratic depth growth
        val zPerspective = zNorm * zNorm

        // Sample curvature at 4 progressive points along the road ahead
        val k1 = getRoadCurvature(playerDistance + clampedZ * 0.25f)
        val k2 = getRoadCurvature(playerDistance + clampedZ * 0.50f)
        val k3 = getRoadCurvature(playerDistance + clampedZ * 0.75f)
        val k4 = getRoadCurvature(playerDistance + clampedZ * 1.00f)

        // Weighted integration giving progressive curvature deflection
        val accumulatedCurvature = (k1 * 0.4f + k2 * 0.3f + k3 * 0.2f + k4 * 0.1f)

        // Project horizontal displacement on screen
        return accumulatedCurvature * zPerspective * (screenWidth * 0.42f)
    }

    /**
     * Compute vanishing point X at the horizon based on distant road curve ~200m ahead.
     */
    fun getHorizonVanishingX(playerDistance: Float, screenWidth: Float): Float {
        val distantCurve = getRoadCurvature(playerDistance + 180f)
        return (screenWidth / 2f) + (distantCurve * screenWidth * 0.26f)
    }

    /**
     * Predict upcoming turns 150m-350m ahead and return turn warning prompts for HUD
     */
    fun getTurnWarning(distanceMeters: Float): TurnWarning? {
        val aheadDist = distanceMeters + 220f
        val upcomingCurve = getRoadCurvature(aheadDist)
        val currentCurve = getRoadCurvature(distanceMeters)

        val curveDiff = upcomingCurve - currentCurve

        return when {
            upcomingCurve < -0.70f -> TurnWarning(
                type = TurnType.SHARP_LEFT,
                label = "SHARP LEFT BEND",
                chevronSymbol = "<<<",
                distanceToTurnMeters = 200f,
                intensity = kotlin.math.abs(upcomingCurve)
            )
            upcomingCurve > 0.70f -> TurnWarning(
                type = TurnType.SHARP_RIGHT,
                label = "SHARP RIGHT BEND",
                chevronSymbol = ">>>",
                distanceToTurnMeters = 200f,
                intensity = kotlin.math.abs(upcomingCurve)
            )
            upcomingCurve < -0.40f -> TurnWarning(
                type = TurnType.GENTLE_LEFT,
                label = "CURVE LEFT",
                chevronSymbol = "<<",
                distanceToTurnMeters = 250f,
                intensity = kotlin.math.abs(upcomingCurve)
            )
            upcomingCurve > 0.40f -> TurnWarning(
                type = TurnType.GENTLE_RIGHT,
                label = "CURVE RIGHT",
                chevronSymbol = ">>",
                distanceToTurnMeters = 250f,
                intensity = kotlin.math.abs(upcomingCurve)
            )
            kotlin.math.abs(curveDiff) > 0.5f -> TurnWarning(
                type = TurnType.S_CHICANE,
                label = "S-CHICANE AHEAD",
                chevronSymbol = "< >",
                distanceToTurnMeters = 180f,
                intensity = 0.8f
            )
            else -> null
        }
    }
}
