package com.example.core.model

enum class RaceMode {
    QUICK_RACE,
    CHAMPIONSHIP,
    TIME_TRIAL,
    PRACTICE
}

enum class Difficulty {
    EASY,
    NORMAL,
    HARD
}

enum class ControlType {
    BUTTONS,
    STEERING_WHEEL,
    MOTION_TILT,
    JOYSTICK
}

enum class GraphicsQuality {
    HIGH,
    MEDIUM,
    LOW
}

enum class BikeUpgradeType {
    ENGINE,
    TIRES,
    BRAKES,
    HANDLING,
    NITRO
}

data class Player(
    val id: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val level: Int = 1,
    val xp: Long = 0,
    val coins: Long = 1000,
    val wins: Int = 0,
    val racesCompleted: Int = 0,
    val totalDistanceKm: Float = 0f,
    val selectedBikeId: String = "bike_legend_x1",
    val isGuest: Boolean = true
) {
    val xpToNextLevel: Long
        get() = (level * 1000L)

    val winRate: Float
        get() = if (racesCompleted > 0) (wins.toFloat() / racesCompleted.toFloat()) * 100f else 0f
}

data class Bike(
    val id: String,
    val name: String,
    val description: String,
    val baseTopSpeed: Int,      // Base km/h e.g. 180
    val baseAcceleration: Int,  // 1-100
    val baseHandling: Int,      // 1-100
    val baseBraking: Int,       // 1-100
    val baseNitro: Int,         // 1-100
    val price: Long,
    val unlockLevel: Int,
    val primaryColorHex: Long = 0xFF00F0FF,
    val secondaryColorHex: Long = 0xFFFF6B00
)

data class PlayerBike(
    val id: String,
    val playerId: String,
    val bikeId: String,
    val unlocked: Boolean = false,
    val engineLevel: Int = 1,
    val tireLevel: Int = 1,
    val brakeLevel: Int = 1,
    val handlingLevel: Int = 1,
    val nitroLevel: Int = 1
)

data class EffectiveBikeStats(
    val bike: Bike,
    val topSpeedKmh: Float,
    val acceleration: Float,
    val handling: Float,
    val braking: Float,
    val nitroMultiplier: Float,
    val nitroDurationSec: Float,
    val upgradeLevels: PlayerBike
)

data class Track(
    val id: String,
    val name: String,
    val description: String,
    val difficulty: Difficulty,
    val defaultLaps: Int = 3,
    val environment: String,
    val unlockLevel: Int = 1,
    val roadWidth: Float = 140f,
    val lengthMeters: Int = 2400,
    val themeColorHex: Long = 0xFF00F0FF
)

data class RaceResult(
    val id: String,
    val playerId: String,
    val trackId: String,
    val trackName: String,
    val bikeId: String,
    val bikeName: String,
    val position: Int,
    val timeMs: Long,
    val bestLapMs: Long,
    val laps: Int,
    val coinsEarned: Long,
    val xpEarned: Long,
    val mode: RaceMode = RaceMode.QUICK_RACE,
    val timestamp: Long = System.currentTimeMillis()
)

data class LeaderboardEntry(
    val id: String,
    val rank: Int,
    val playerId: String,
    val playerName: String,
    val trackId: String,
    val trackName: String,
    val bikeName: String,
    val timeMs: Long,
    val distanceKm: Float = 0f,
    val topSpeedKmh: Float = 0f,
    val isCurrentPlayer: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val requirementType: String,
    val requirementValue: Int,
    val rewardCoins: Long,
    val rewardXp: Long,
    val isUnlocked: Boolean = false,
    val currentProgress: Int = 0,
    val unlockedAt: Long? = null
)

data class Championship(
    val id: String,
    val name: String,
    val description: String,
    val requiredLevel: Int,
    val currentRaceIndex: Int = 0,
    val totalPoints: Int = 0,
    val isCompleted: Boolean = false,
    val races: List<ChampionshipRace> = emptyList()
)

data class ChampionshipRace(
    val id: String,
    val championshipId: String,
    val trackId: String,
    val raceOrder: Int,
    val laps: Int = 3
)

data class DailyChallenge(
    val id: String,
    val dateString: String,
    val trackId: String,
    val trackName: String,
    val targetTimeMs: Long,
    val rewardCoins: Long,
    val rewardXp: Long,
    val isCompleted: Boolean = false,
    val bestAttemptMs: Long? = null
)

data class GameSettings(
    val musicVolume: Float = 0.8f,
    val sfxVolume: Float = 1.0f,
    val vibrationEnabled: Boolean = true,
    val controlType: ControlType = ControlType.BUTTONS,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH,
    val tiltEnabled: Boolean = false,
    val autoAccelerate: Boolean = false,
    val tiltSensitivity: Float = 1.0f,
    val tiltDeadzone: Float = 0.05f,
    val invertTilt: Boolean = false
)

data class RacerState(
    val id: String,
    val name: String,
    val isPlayer: Boolean,
    val posX: Float,
    val posY: Float,
    val velocityX: Float = 0f,
    val velocityY: Float = 0f,
    val speedKmh: Float = 0f,
    val angleRad: Float = 0f,
    val leanAngleRad: Float = 0f,
    val currentLap: Int = 1,
    val nextCheckpointIndex: Int = 0,
    val totalRaceDistance: Float = 0f,
    val isNitroActive: Boolean = false,
    val finishedTimeMs: Long? = null,
    val primaryColorHex: Long = 0xFF00F0FF,
    val secondaryColorHex: Long = 0xFFFF6B00,
    val bikeName: String = "Legend X1",
    val pitchAngleRad: Float = 0f,
    val throttleRatio: Float = 0f,
    val brakeRatio: Float = 0f
)

enum class GameEngineState {
    LOADING,
    COUNTDOWN,
    RACING,
    PAUSED,
    FINISHED,
    MISSION_FAILED,
    RESULTS
}

enum class ObstacleType {
    TWO_WHEELER,       // Scooters, Commuters & Cafe Racers
    FOUR_WHEELER,      // Sedans, Sports Coupes & Hatchbacks
    TRAFFIC_CAR,       // Standard City Traffic
    TRAFFIC_TAXI,      // Yellow & Cyber Cabs
    TRUCK,             // Heavy 18-Wheeler Semi-Trailer Haulers
    TEMPO,             // 3-Wheeler Auto & Utility Delivery Tempos
    TRAIN_CROSSING,    // Level Railway Crossing with Barrier & High-Speed Train
    ROAD_CONE,
    BARRIER,
    OIL_SPILL,
    DEBRIS
}

data class HighwayObstacle(
    val id: String,
    val type: ObstacleType = ObstacleType.FOUR_WHEELER,
    val lane: Float = 0f, // -0.8f to +0.8f
    val distanceMeters: Float = 0f,
    val speedKmh: Float = 95f,
    val primaryColorHex: Long = 0xFFFFD600,
    val secondaryColorHex: Long = 0xFF00F0FF,
    val isHit: Boolean = false,
    val trainProgress: Float = 0f, // 0.0 to 1.0 crossing animation progress
    val isHonked: Boolean = false, // When player honks, car shifts lane
    val vehicleSubModel: Int = 0   // Style variation (e.g. Scooter vs Cafe, Pickup vs Auto)
)
