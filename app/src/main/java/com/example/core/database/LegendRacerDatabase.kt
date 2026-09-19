package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        PlayerEntity::class,
        BikeEntity::class,
        PlayerBikeEntity::class,
        TrackEntity::class,
        RaceResultEntity::class,
        AchievementEntity::class,
        PlayerAchievementEntity::class,
        LeaderboardEntity::class,
        ChampionshipProgressEntity::class,
        DailyChallengeEntity::class,
        SyncQueueEntity::class,
        Racer::class,
        Motorcycle::class,
        Track::class,
        RaceResult::class
    ],
    version = 4,
    exportSchema = false
)
abstract class LegendRacerDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun bikeDao(): BikeDao
    abstract fun trackDao(): TrackDao
    abstract fun raceResultDao(): RaceResultDao
    abstract fun achievementDao(): AchievementDao
    abstract fun leaderboardDao(): LeaderboardDao
    abstract fun championshipDao(): ChampionshipDao
    abstract fun dailyChallengeDao(): DailyChallengeDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun racerDao(): RacerDao
    abstract fun motorcycleDao(): MotorcycleDao
    abstract fun careerTrackDao(): CareerTrackDao
    abstract fun careerRaceResultDao(): CareerRaceResultDao

    companion object {
        @Volatile
        private var INSTANCE: LegendRacerDatabase? = null

        fun getInstance(context: Context): LegendRacerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LegendRacerDatabase::class.java,
                    "legend_racer.db"
                )
                .fallbackToDestructiveMigration(true)
                .addCallback(DatabaseCallback(context))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val context: Context
    ) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                getInstance(context).seedInitialData()
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            CoroutineScope(Dispatchers.IO).launch {
                val dbInstance = getInstance(context)
                if (dbInstance.playerDao().getPlayerDirect() == null) {
                    dbInstance.seedInitialData()
                }
            }
        }
    }

    suspend fun seedInitialData() {
        // 1. Initial Player
        val initialPlayer = PlayerEntity(
            id = "guest_player_1",
            displayName = "Ashok Gharge",
            avatarUrl = null,
            level = 5,
            xp = 1450,
            coins = 8500,
            wins = 8,
            racesCompleted = 12,
            totalDistanceKm = 1248.5f,
            selectedBikeId = "bike_galaxy_edition",
            isGuest = false
        )
        playerDao().insertOrUpdate(initialPlayer)

        // 2. Fictional Bikes - 5 Free High-Performance Racing Designs
        val bikes = listOf(
            BikeEntity(
                id = "bike_galaxy_edition",
                name = "Galaxy Superbike 1000RR",
                description = "Cosmic hyperbike engineered with celestial nebula aero winglets, forged carbon frame, and plasma nitro pulse.",
                baseTopSpeed = 295,
                baseAcceleration = 98,
                baseHandling = 95,
                baseBraking = 96,
                baseNitro = 99,
                price = 0,
                unlockLevel = 1,
                primaryColorHex = 0xFFFF1B7A,   // Nebula Pink
                secondaryColorHex = 0xFF8B5CF6  // Cosmic Violet
            ),
            BikeEntity(
                id = "bike_streetfighter",
                name = "Streetfighter Naked 1200",
                description = "Exposed trellis-frame beast with aggressive streetfighter bars, immense low-end torque, and rapid chicane flicking.",
                baseTopSpeed = 275,
                baseAcceleration = 96,
                baseHandling = 94,
                baseBraking = 92,
                baseNitro = 90,
                price = 0,
                unlockLevel = 1,
                primaryColorHex = 0xFF00E676,   // Toxic Neon Green
                secondaryColorHex = 0xFFFF3D00  // Flare Orange
            ),
            BikeEntity(
                id = "bike_hayabusa",
                name = "Hayabusa Hyper Tourer",
                description = "Aerodynamic teardrop bullet engineered in high-speed wind tunnels with dual chrome cannon exhausts and rock-solid highway stability.",
                baseTopSpeed = 315,
                baseAcceleration = 94,
                baseHandling = 88,
                baseBraking = 95,
                baseNitro = 95,
                price = 0,
                unlockLevel = 1,
                primaryColorHex = 0xFFFFFFFF,   // Pearl White
                secondaryColorHex = 0xFFFFD600  // Celestial Gold
            ),
            BikeEntity(
                id = "bike_cyber_pulse",
                name = "Cyber Pulse Neon Concept",
                description = "Futuristic electric hyperbike with hubless glowing neon ring wheels, zero-friction magnetic drive, and instantaneous electric warp.",
                baseTopSpeed = 285,
                baseAcceleration = 99,
                baseHandling = 96,
                baseBraking = 97,
                baseNitro = 98,
                price = 0,
                unlockLevel = 1,
                primaryColorHex = 0xFF00F0FF,   // Electric Cyan
                secondaryColorHex = 0xFFE040FB  // Neon Magenta
            ),
            BikeEntity(
                id = "bike_cafe_racer",
                name = "Cafe Racer Custom 800",
                description = "Handcrafted vintage cafe racer featuring rounded aerodynamic bubble cowl, chrome spoked wheels, clip-on bars, and twin exhaust roar.",
                baseTopSpeed = 250,
                baseAcceleration = 90,
                baseHandling = 98,
                baseBraking = 90,
                baseNitro = 85,
                price = 0,
                unlockLevel = 1,
                primaryColorHex = 0xFFFF1744,   // Ruby Crimson
                secondaryColorHex = 0xFFB0BEC5  // Polished Chrome
            ),
            BikeEntity(
                id = "bike_legend_x1",
                name = "Legend X1 Sport",
                description = "Balanced sportbike with agile cornering response and stable chassis.",
                baseTopSpeed = 220,
                baseAcceleration = 80,
                baseHandling = 85,
                baseBraking = 80,
                baseNitro = 75,
                price = 0,
                unlockLevel = 1,
                primaryColorHex = 0xFF2979FF,   // Royal Blue
                secondaryColorHex = 0xFFFF9100  // Neon Amber
            )
        )
        bikeDao().insertBikes(bikes)

        // Initial Player Bike Ownership - All 5 Types Free & Unlocked!
        val playerBikes = listOf(
            PlayerBikeEntity("pb_0", "guest_player_1", "bike_galaxy_edition", unlocked = true, 1, 1, 1, 1, 1),
            PlayerBikeEntity("pb_1", "guest_player_1", "bike_streetfighter", unlocked = true, 1, 1, 1, 1, 1),
            PlayerBikeEntity("pb_2", "guest_player_1", "bike_hayabusa", unlocked = true, 1, 1, 1, 1, 1),
            PlayerBikeEntity("pb_3", "guest_player_1", "bike_cyber_pulse", unlocked = true, 1, 1, 1, 1, 1),
            PlayerBikeEntity("pb_4", "guest_player_1", "bike_cafe_racer", unlocked = true, 1, 1, 1, 1, 1),
            PlayerBikeEntity("pb_5", "guest_player_1", "bike_legend_x1", unlocked = true, 1, 1, 1, 1, 1)
        )
        bikeDao().insertPlayerBikes(playerBikes)

        // 3. Fictional Tracks
        val tracks = listOf(
            TrackEntity(
                id = "track_city_rush",
                name = "City Rush",
                description = "Downtown neon boulevard with wide high-speed straightaways and gentle chicanes.",
                difficulty = "EASY",
                defaultLaps = 3,
                environment = "Neon Metropolis",
                unlockLevel = 1,
                roadWidth = 160f,
                lengthMeters = 2200,
                themeColorHex = 0xFF00F0FF
            ),
            TrackEntity(
                id = "track_desert_storm",
                name = "Desert Storm",
                description = "Canyon dunes highway featuring sharp sweeping curves and sweeping elevation changes.",
                difficulty = "NORMAL",
                defaultLaps = 3,
                environment = "Canyon Oasis",
                unlockLevel = 2,
                roadWidth = 145f,
                lengthMeters = 2800,
                themeColorHex = 0xFFFF6B00
            ),
            TrackEntity(
                id = "track_mountain_edge",
                name = "Mountain Edge",
                description = "Tight mountain pass with technical hairpins demanding precise braking and throttle control.",
                difficulty = "HARD",
                defaultLaps = 3,
                environment = "Alpine Ridge",
                unlockLevel = 5,
                roadWidth = 130f,
                lengthMeters = 3100,
                themeColorHex = 0xFFB5179E
            ),
            TrackEntity(
                id = "track_night_highway",
                name = "Night Highway",
                description = "Midnight coastal express tollway with fast flowing multi-lane racing sectors.",
                difficulty = "NORMAL",
                defaultLaps = 3,
                environment = "Tokyo Express",
                unlockLevel = 8,
                roadWidth = 150f,
                lengthMeters = 2600,
                themeColorHex = 0xFF00E676
            ),
            TrackEntity(
                id = "track_coastal_run",
                name = "Coastal Run",
                description = "Oceanfront grand prix circuit featuring rapid esses and an ultra-long sprint finish.",
                difficulty = "HARD",
                defaultLaps = 3,
                environment = "Riviera Grand Prix",
                unlockLevel = 12,
                roadWidth = 140f,
                lengthMeters = 3400,
                themeColorHex = 0xFFFFD600
            )
        )
        trackDao().insertTracks(tracks)

        // 4. Achievements
        val achievements = listOf(
            AchievementEntity("ach_first_race", "First Ignition", "Complete your very first race.", "RACE_COUNT", 1, 200, 150),
            AchievementEntity("ach_first_win", "Checkered Glory", "Win your first 1st place race victory.", "WINS", 1, 500, 300),
            AchievementEntity("ach_speed_demon", "Speed Demon", "Exceed 220 km/h in any race.", "MAX_SPEED", 220, 400, 250),
            AchievementEntity("ach_nitro_master", "Nitro Surge", "Deplete 5 full nitro tanks during races.", "NITRO_COUNT", 5, 350, 200),
            AchievementEntity("ach_10_races", "Asphalt Veteran", "Complete 10 total races across any tracks.", "RACE_COUNT", 10, 1000, 600),
            AchievementEntity("ach_25_races", "Speed Addict", "Complete 25 total races.", "RACE_COUNT", 25, 2500, 1200),
            AchievementEntity("ach_50_races", "Track Legend", "Complete 50 total races.", "RACE_COUNT", 50, 6000, 3000),
            AchievementEntity("ach_time_trial", "Time Specialist", "Set a personal best time in Time Trial mode.", "TIME_TRIAL", 1, 500, 350),
            AchievementEntity("ach_champion", "Season Champion", "Win 1st overall in the Grand Championship.", "CHAMPIONSHIP", 1, 3000, 2000),
            AchievementEntity("ach_perfect_finish", "Apex Master", "Finish a 3-lap race without touching track barriers.", "CLEAN_RACE", 1, 800, 500)
        )
        achievementDao().insertAchievements(achievements)

        val playerAchievements = achievements.map { ach ->
            PlayerAchievementEntity(
                id = "pa_${ach.id}",
                playerId = "guest_player_1",
                achievementId = ach.id,
                isUnlocked = false,
                currentProgress = 0,
                unlockedAt = null
            )
        }
        achievementDao().insertPlayerAchievements(playerAchievements)

        // 5. Initial Leaderboard entries sorted by Distance (KM)
        // Higher km rider is at 1st rank with the registered user prominently shown!
        val initialScores = listOf(
            LeaderboardEntity(
                id = "lb_player",
                rank = 1,
                playerId = "guest_player_1",
                playerName = "Ashok Gharge",
                trackId = "track_city_rush",
                trackName = "City Rush",
                bikeName = "Legend X1",
                timeMs = 45200L,
                distanceKm = 1248.5f,
                topSpeedKmh = 238.4f,
                isCurrentPlayer = true,
                timestamp = System.currentTimeMillis()
            ),
            LeaderboardEntity(
                id = "lb_1",
                rank = 2,
                playerId = "rival_1",
                playerName = "SpeedPhantom",
                trackId = "track_city_rush",
                trackName = "City Rush",
                bikeName = "Legend Pro",
                timeMs = 48200L,
                distanceKm = 985.2f,
                topSpeedKmh = 230.5f,
                isCurrentPlayer = false,
                timestamp = System.currentTimeMillis() - 86400000
            ),
            LeaderboardEntity(
                id = "lb_2",
                rank = 3,
                playerId = "rival_2",
                playerName = "VortexRider",
                trackId = "track_city_rush",
                trackName = "City Rush",
                bikeName = "Legend GT",
                timeMs = 49800L,
                distanceKm = 742.0f,
                topSpeedKmh = 222.0f,
                isCurrentPlayer = false,
                timestamp = System.currentTimeMillis() - 72000000
            ),
            LeaderboardEntity(
                id = "lb_3",
                rank = 4,
                playerId = "rival_3",
                playerName = "CyberBlaze",
                trackId = "track_city_rush",
                trackName = "City Rush",
                bikeName = "Legend X2",
                timeMs = 51400L,
                distanceKm = 520.4f,
                topSpeedKmh = 215.0f,
                isCurrentPlayer = false,
                timestamp = System.currentTimeMillis() - 43200000
            ),
            LeaderboardEntity(
                id = "lb_4",
                rank = 5,
                playerId = "rival_4",
                playerName = "ApexPredator",
                trackId = "track_city_rush",
                trackName = "City Rush",
                bikeName = "Legend X1",
                timeMs = 53100L,
                distanceKm = 310.8f,
                topSpeedKmh = 205.0f,
                isCurrentPlayer = false,
                timestamp = System.currentTimeMillis() - 21600000
            ),
            LeaderboardEntity(
                id = "lb_5",
                rank = 6,
                playerId = "rival_5",
                playerName = "NeonDrifter",
                trackId = "track_city_rush",
                trackName = "City Rush",
                bikeName = "Legend X1",
                timeMs = 55600L,
                distanceKm = 180.2f,
                topSpeedKmh = 195.0f,
                isCurrentPlayer = false,
                timestamp = System.currentTimeMillis() - 10800000
            )
        )
        leaderboardDao().insertScores(initialScores)

        // 6. Daily Challenge
        val challenge = DailyChallengeEntity(
            id = "daily_challenge_today",
            dateString = "2026-09-12",
            trackId = "track_city_rush",
            trackName = "City Rush",
            targetTimeMs = 52000L,
            rewardCoins = 1200L,
            rewardXp = 800L,
            isCompleted = false,
            bestAttemptMs = null
        )
        dailyChallengeDao().insertChallenge(challenge)

        // 7. Career Progression Offline Persistence Seeding: Racer
        val defaultRacer = Racer(
            id = "racer_career_1",
            name = "Apex Rider",
            level = 5,
            xp = 1450L,
            coins = 8500L,
            careerTier = 2,
            reputation = 420,
            wins = 8,
            podiums = 11,
            racesCompleted = 12,
            totalDistanceKm = 1248.5f,
            selectedMotorcycleId = "motorcycle_legend_x1",
            isOfflinePlayer = true
        )
        racerDao().insertOrUpdateRacer(defaultRacer)

        // 8. Career Progression Offline Persistence Seeding: Motorcycles
        val careerMotorcycles = listOf(
            Motorcycle(
                id = "motorcycle_legend_x1",
                name = "Hero Splendor Plus",
                category = "COMMUTER",
                description = "Legendary Indian commuter bike. Unmatched mileage and reliability.",
                topSpeedKmh = 110f,
                accelerationMpss = 6.5f,
                handlingRating = 95f,
                brakingPower = 70f,
                nitroCapacity = 50f,
                price = 0L,
                unlockLevel = 1,
                isUnlocked = true,
                engineUpgradeLevel = 2,
                primaryColorHex = 0xFF000000,
                secondaryColorHex = 0xFFC0C0C0
            ),
            Motorcycle(
                id = "motorcycle_legend_x2",
                name = "Apex Hyper-Sport 1000",
                category = "HYPERBIKE",
                description = "Raw liter-class powerhouse built for high-speed highway dominance.",
                topSpeedKmh = 245f,
                accelerationMpss = 13.8f,
                handlingRating = 76f,
                brakingPower = 88f,
                nitroCapacity = 120f,
                price = 5000L,
                unlockLevel = 3,
                isUnlocked = true,
                engineUpgradeLevel = 1,
                primaryColorHex = 0xFFFF0055,
                secondaryColorHex = 0xFF7928CA
            ),
            Motorcycle(
                id = "motorcycle_shadow_gt",
                name = "Shadow GT Prototype",
                category = "PROTOTYPE",
                description = "Carbon-fiber experimental rocket with massive twin-turbo nitro capacity.",
                topSpeedKmh = 280f,
                accelerationMpss = 16.5f,
                handlingRating = 92f,
                brakingPower = 94f,
                nitroCapacity = 150f,
                price = 15000L,
                unlockLevel = 7,
                isUnlocked = false,
                engineUpgradeLevel = 1,
                primaryColorHex = 0xFF7928CA,
                secondaryColorHex = 0xFF00F0FF
            )
        )
        motorcycleDao().insertMotorcycles(careerMotorcycles)

        // 9. Career Progression Offline Persistence Seeding: Tracks
        val careerTracks = listOf(
            Track(
                id = "career_track_city_rush",
                name = "Neon Expressway",
                description = "High-speed expressway through towering skyscrapers and illuminated tunnels.",
                difficulty = "EASY",
                defaultLaps = 3,
                environment = "Metropolis Highway",
                unlockLevel = 1,
                isUnlocked = true,
                roadWidth = 145f,
                lengthMeters = 2400,
                themeColorHex = 0xFF00F0FF,
                offlineBestTimeMs = 52400L,
                offlineBestLapMs = 16800L
            ),
            Track(
                id = "career_track_mountain_edge",
                name = "Alpine Mountain Pass",
                description = "Technical mountain bends with hairpins demanding precision braking.",
                difficulty = "HARD",
                defaultLaps = 3,
                environment = "Alpine Ridge",
                unlockLevel = 4,
                isUnlocked = true,
                roadWidth = 130f,
                lengthMeters = 3100,
                themeColorHex = 0xFFB5179E,
                offlineBestTimeMs = 68200L,
                offlineBestLapMs = 21900L
            ),
            Track(
                id = "career_track_night_highway",
                name = "Midnight Expressway",
                description = "Multi-lane coastal speedway under glowing streetlamps and neon billboards.",
                difficulty = "NORMAL",
                defaultLaps = 3,
                environment = "Tokyo Express",
                unlockLevel = 7,
                isUnlocked = false,
                roadWidth = 150f,
                lengthMeters = 2800,
                themeColorHex = 0xFF00E676,
                offlineBestTimeMs = 0L,
                offlineBestLapMs = 0L
            )
        )
        careerTrackDao().insertTracks(careerTracks)
    }
}
