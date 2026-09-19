package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players LIMIT 1")
    fun getPlayer(): Flow<PlayerEntity?>

    @Query("SELECT * FROM players LIMIT 1")
    suspend fun getPlayerDirect(): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(player: PlayerEntity)

    @Query("UPDATE players SET coins = coins + :deltaCoins, xp = xp + :deltaXp, level = :newLevel WHERE id = :playerId")
    suspend fun updateCoinsAndXp(playerId: String, deltaCoins: Long, deltaXp: Long, newLevel: Int)

    @Query("UPDATE players SET selectedBikeId = :bikeId WHERE id = :playerId")
    suspend fun setSelectedBike(playerId: String, bikeId: String)

    @Query("UPDATE players SET racesCompleted = racesCompleted + 1, wins = wins + :winDelta WHERE id = :playerId")
    suspend fun recordRaceCompletion(playerId: String, winDelta: Int)

    @Query("UPDATE players SET displayName = :newName WHERE id = :playerId")
    suspend fun updateDisplayName(playerId: String, newName: String)

    @Query("UPDATE players SET totalDistanceKm = totalDistanceKm + :deltaKm WHERE id = :playerId")
    suspend fun addDistanceKm(playerId: String, deltaKm: Float)
}

@Dao
interface BikeDao {
    @Query("SELECT * FROM bikes ORDER BY unlockLevel ASC")
    fun getAllBikes(): Flow<List<BikeEntity>>

    @Query("SELECT * FROM bikes WHERE id = :id")
    suspend fun getBikeById(id: String): BikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBikes(bikes: List<BikeEntity>)

    @Query("SELECT * FROM player_bikes WHERE playerId = :playerId")
    fun getPlayerBikes(playerId: String): Flow<List<PlayerBikeEntity>>

    @Query("SELECT * FROM player_bikes WHERE playerId = :playerId AND bikeId = :bikeId")
    suspend fun getPlayerBike(playerId: String, bikeId: String): PlayerBikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerBikes(playerBikes: List<PlayerBikeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerBike(playerBike: PlayerBikeEntity)

    @Update
    suspend fun updatePlayerBike(playerBike: PlayerBikeEntity)
}

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY unlockLevel ASC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: String): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)
}

@Dao
interface RaceResultDao {
    @Query("SELECT * FROM race_results WHERE playerId = :playerId ORDER BY timestamp DESC")
    fun getPlayerRaceResults(playerId: String): Flow<List<RaceResultEntity>>

    @Query("SELECT * FROM race_results WHERE trackId = :trackId ORDER BY timeMs ASC LIMIT 10")
    fun getBestResultsForTrack(trackId: String): Flow<List<RaceResultEntity>>

    @Query("SELECT MIN(bestLapMs) FROM race_results WHERE trackId = :trackId AND playerId = :playerId AND bestLapMs > 0")
    suspend fun getPersonalBestLap(trackId: String, playerId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaceResult(result: RaceResultEntity)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM player_achievements WHERE playerId = :playerId")
    fun getPlayerAchievements(playerId: String): Flow<List<PlayerAchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerAchievements(playerAchievements: List<PlayerAchievementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerAchievement(playerAchievement: PlayerAchievementEntity)

    @Query("UPDATE player_achievements SET isUnlocked = 1, currentProgress = :progress, unlockedAt = :unlockedAt WHERE playerId = :playerId AND achievementId = :achievementId")
    suspend fun unlockAchievement(playerId: String, achievementId: String, progress: Int, unlockedAt: Long)
}

@Dao
interface LeaderboardDao {
    @Query("SELECT * FROM leaderboard_scores WHERE trackId = :trackId ORDER BY distanceKm DESC, timeMs ASC LIMIT 50")
    fun getScoresForTrack(trackId: String): Flow<List<LeaderboardEntity>>

    @Query("SELECT * FROM leaderboard_scores ORDER BY distanceKm DESC, timeMs ASC LIMIT 50")
    fun getGlobalScores(): Flow<List<LeaderboardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScores(scores: List<LeaderboardEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(score: LeaderboardEntity)

    @Query("DELETE FROM leaderboard_scores WHERE trackId = :trackId")
    suspend fun clearScoresForTrack(trackId: String)

    @Query("UPDATE leaderboard_scores SET playerName = :newName WHERE isCurrentPlayer = 1 OR playerId = :playerId")
    suspend fun updatePlayerName(playerId: String, newName: String)

    @Query("UPDATE leaderboard_scores SET distanceKm = distanceKm + :deltaKm, topSpeedKmh = MAX(topSpeedKmh, :topSpeed) WHERE isCurrentPlayer = 1 OR playerId = :playerId")
    suspend fun addDistanceToPlayer(playerId: String, deltaKm: Float, topSpeed: Float)

    @Query("SELECT * FROM leaderboard_scores WHERE isCurrentPlayer = 1 LIMIT 1")
    suspend fun getCurrentPlayerScore(): LeaderboardEntity?
}

@Dao
interface ChampionshipDao {
    @Query("SELECT * FROM championship_progress WHERE playerId = :playerId AND championshipId = :championshipId")
    fun getProgress(playerId: String, championshipId: String): Flow<ChampionshipProgressEntity?>

    @Query("SELECT * FROM championship_progress WHERE playerId = :playerId AND championshipId = :championshipId")
    suspend fun getProgressDirect(playerId: String, championshipId: String): ChampionshipProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: ChampionshipProgressEntity)
}

@Dao
interface DailyChallengeDao {
    @Query("SELECT * FROM daily_challenges ORDER BY dateString DESC LIMIT 1")
    fun getLatestChallenge(): Flow<DailyChallengeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: DailyChallengeEntity)

    @Query("UPDATE daily_challenges SET isCompleted = 1, bestAttemptMs = :timeMs WHERE id = :id")
    suspend fun markCompleted(id: String, timeMs: Long)
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    suspend fun getPendingItems(): List<SyncQueueEntity>

    @Insert
    suspend fun enqueue(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun dequeue(id: Long)

    @Query("UPDATE sync_queue SET attempts = attempts + 1 WHERE id = :id")
    suspend fun incrementAttempts(id: Long)
}

@Dao
interface RacerDao {
    @Query("SELECT * FROM career_racers LIMIT 1")
    fun getRacer(): Flow<Racer?>

    @Query("SELECT * FROM career_racers LIMIT 1")
    suspend fun getRacerDirect(): Racer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRacer(racer: Racer)

    @Query("UPDATE career_racers SET coins = coins + :deltaCoins, xp = xp + :deltaXp, level = :newLevel, careerTier = :tier WHERE id = :racerId")
    suspend fun updateCareerProgression(racerId: String, deltaCoins: Long, deltaXp: Long, newLevel: Int, tier: Int)

    @Query("UPDATE career_racers SET wins = wins + :winDelta, racesCompleted = racesCompleted + 1, totalDistanceKm = totalDistanceKm + :deltaKm WHERE id = :racerId")
    suspend fun recordRaceStats(racerId: String, winDelta: Int, deltaKm: Float)

    @Query("UPDATE career_racers SET selectedMotorcycleId = :motorcycleId WHERE id = :racerId")
    suspend fun setSelectedMotorcycle(racerId: String, motorcycleId: String)
}

@Dao
interface MotorcycleDao {
    @Query("SELECT * FROM career_motorcycles ORDER BY unlockLevel ASC")
    fun getAllMotorcycles(): Flow<List<Motorcycle>>

    @Query("SELECT * FROM career_motorcycles WHERE id = :id")
    suspend fun getMotorcycleById(id: String): Motorcycle?

    @Query("SELECT * FROM career_motorcycles WHERE isUnlocked = 1")
    fun getUnlockedMotorcycles(): Flow<List<Motorcycle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMotorcycles(motorcycles: List<Motorcycle>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMotorcycle(motorcycle: Motorcycle)

    @Update
    suspend fun updateMotorcycle(motorcycle: Motorcycle)

    @Query("UPDATE career_motorcycles SET isUnlocked = 1 WHERE id = :id")
    suspend fun unlockMotorcycle(id: String)
}

@Dao
interface CareerTrackDao {
    @Query("SELECT * FROM career_tracks ORDER BY unlockLevel ASC")
    fun getAllCareerTracks(): Flow<List<Track>>

    @Query("SELECT * FROM career_tracks WHERE id = :id")
    suspend fun getTrackById(id: String): Track?

    @Query("SELECT * FROM career_tracks WHERE isUnlocked = 1")
    fun getUnlockedTracks(): Flow<List<Track>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<Track>)

    @Update
    suspend fun updateTrack(track: Track)

    @Query("UPDATE career_tracks SET offlineBestTimeMs = :timeMs, offlineBestLapMs = :lapMs WHERE id = :trackId AND (offlineBestTimeMs == 0 OR :timeMs < offlineBestTimeMs)")
    suspend fun updateOfflineRecords(trackId: String, timeMs: Long, lapMs: Long)
}

@Dao
interface CareerRaceResultDao {
    @Query("SELECT * FROM career_race_results WHERE racerId = :racerId ORDER BY timestamp DESC")
    fun getRacerResults(racerId: String): Flow<List<RaceResult>>

    @Query("SELECT * FROM career_race_results WHERE trackId = :trackId ORDER BY raceTimeMs ASC LIMIT 10")
    fun getTopTrackResults(trackId: String): Flow<List<RaceResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaceResult(result: RaceResult)

    @Query("SELECT COUNT(*) FROM career_race_results WHERE racerId = :racerId")
    suspend fun getTotalRacesCount(racerId: String): Int
}

