package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val avatarUrl: String?,
    val level: Int,
    val xp: Long,
    val coins: Long,
    val wins: Int,
    val racesCompleted: Int,
    val totalDistanceKm: Float = 0f,
    val selectedBikeId: String,
    val isGuest: Boolean
)

@Entity(tableName = "bikes")
data class BikeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val baseTopSpeed: Int,
    val baseAcceleration: Int,
    val baseHandling: Int,
    val baseBraking: Int,
    val baseNitro: Int,
    val price: Long,
    val unlockLevel: Int,
    val primaryColorHex: Long,
    val secondaryColorHex: Long
)

@Entity(tableName = "player_bikes")
data class PlayerBikeEntity(
    @PrimaryKey val id: String,
    val playerId: String,
    val bikeId: String,
    val unlocked: Boolean,
    val engineLevel: Int,
    val tireLevel: Int,
    val brakeLevel: Int,
    val handlingLevel: Int,
    val nitroLevel: Int
)

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val difficulty: String,
    val defaultLaps: Int,
    val environment: String,
    val unlockLevel: Int,
    val roadWidth: Float,
    val lengthMeters: Int,
    val themeColorHex: Long
)

@Entity(tableName = "race_results")
data class RaceResultEntity(
    @PrimaryKey val id: String,
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
    val mode: String,
    val timestamp: Long
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val requirementType: String,
    val requirementValue: Int,
    val rewardCoins: Long,
    val rewardXp: Long
)

@Entity(tableName = "player_achievements")
data class PlayerAchievementEntity(
    @PrimaryKey val id: String,
    val playerId: String,
    val achievementId: String,
    val isUnlocked: Boolean,
    val currentProgress: Int,
    val unlockedAt: Long?
)

@Entity(tableName = "leaderboard_scores")
data class LeaderboardEntity(
    @PrimaryKey val id: String,
    val rank: Int,
    val playerId: String,
    val playerName: String,
    val trackId: String,
    val trackName: String,
    val bikeName: String,
    val timeMs: Long,
    val distanceKm: Float = 0f,
    val topSpeedKmh: Float = 0f,
    val isCurrentPlayer: Boolean,
    val timestamp: Long
)

@Entity(tableName = "championship_progress")
data class ChampionshipProgressEntity(
    @PrimaryKey val id: String,
    val playerId: String,
    val championshipId: String,
    val currentRaceIndex: Int,
    val totalPoints: Int,
    val isCompleted: Boolean
)

@Entity(tableName = "daily_challenges")
data class DailyChallengeEntity(
    @PrimaryKey val id: String,
    val dateString: String,
    val trackId: String,
    val trackName: String,
    val targetTimeMs: Long,
    val rewardCoins: Long,
    val rewardXp: Long,
    val isCompleted: Boolean,
    val bestAttemptMs: Long?
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String, // "RACE_RESULT", "UPGRADE_BIKE", "UNLOCK_BIKE", "PROFILE_UPDATE", "ACHIEVEMENT"
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attempts: Int = 0
)

/**
 * Core Room Entity representing a 'Racer' to support career progression and offline persistence.
 * Tracks racer level, career tier, reputation, coins, XP, podiums, wins, distance, and equipped motorcycle.
 */
@Entity(tableName = "career_racers")
data class Racer(
    @PrimaryKey val id: String = "player_default",
    val name: String = "Apex Rider",
    val level: Int = 1,
    val xp: Long = 0L,
    val coins: Long = 1000L,
    val careerTier: Int = 1,
    val reputation: Int = 100,
    val wins: Int = 0,
    val podiums: Int = 0,
    val racesCompleted: Int = 0,
    val totalDistanceKm: Float = 0f,
    val selectedMotorcycleId: String = "bike_legend_x1",
    val isOfflinePlayer: Boolean = true,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)

/**
 * Core Room Entity representing a 'Motorcycle' to support career progression and offline persistence.
 * Contains performance attributes (speed, acceleration, handling, braking, nitro), upgrade levels, price, and unlock state.
 */
@Entity(tableName = "career_motorcycles")
data class Motorcycle(
    @PrimaryKey val id: String,
    val name: String,
    val category: String = "SUPERBIKE",
    val description: String = "",
    val topSpeedKmh: Float = 220f,
    val accelerationMpss: Float = 12f,
    val handlingRating: Float = 80f,
    val brakingPower: Float = 85f,
    val nitroCapacity: Float = 100f,
    val price: Long = 0L,
    val unlockLevel: Int = 1,
    val isUnlocked: Boolean = false,
    val engineUpgradeLevel: Int = 1,
    val handlingUpgradeLevel: Int = 1,
    val brakeUpgradeLevel: Int = 1,
    val nitroUpgradeLevel: Int = 1,
    val primaryColorHex: Long = 0xFF00F0FF,
    val secondaryColorHex: Long = 0xFFFF0055
)

/**
 * Core Room Entity representing a 'Track' to support career progression and offline persistence.
 * Captures track geometry, difficulty, laps, environment, unlock level, and offline personal records.
 */
@Entity(tableName = "career_tracks")
data class Track(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val difficulty: String = "NORMAL",
    val defaultLaps: Int = 3,
    val environment: String = "NEON_HIGHWAY",
    val unlockLevel: Int = 1,
    val isUnlocked: Boolean = true,
    val roadWidth: Float = 140f,
    val lengthMeters: Int = 2400,
    val themeColorHex: Long = 0xFF00F0FF,
    val offlineBestTimeMs: Long = 0L,
    val offlineBestLapMs: Long = 0L
)

/**
 * Core Room Entity representing a 'RaceResult' to support career progression and offline persistence.
 * Records position, total time, best lap, laps completed, coins & XP earned, and offline sync status.
 */
@Entity(tableName = "career_race_results")
data class RaceResult(
    @PrimaryKey val id: String,
    val racerId: String,
    val trackId: String,
    val trackName: String,
    val motorcycleId: String,
    val motorcycleName: String,
    val position: Int,
    val raceTimeMs: Long,
    val bestLapMs: Long,
    val lapsCompleted: Int,
    val coinsEarned: Long,
    val xpEarned: Long,
    val raceMode: String = "CAREER",
    val timestamp: Long = System.currentTimeMillis(),
    val isSyncedOffline: Boolean = true
)

