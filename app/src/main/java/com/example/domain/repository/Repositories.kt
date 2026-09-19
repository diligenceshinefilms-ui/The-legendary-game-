package com.example.domain.repository

import com.example.core.model.*
import com.example.core.network.ApiResult
import com.example.core.network.AuthSession
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    fun getPlayer(): Flow<Player?>
    suspend fun getPlayerDirect(): Player?
    suspend fun updatePlayer(player: Player)
    suspend fun updateDisplayName(newName: String)
    suspend fun addDistanceKm(deltaKm: Float)
    suspend fun awardRaceRewards(coins: Long, xp: Long, won: Boolean): Pair<Player, Boolean> // Returns updated player and whether level up occurred
    suspend fun selectBike(bikeId: String)
    suspend fun syncWithRemote()
}

interface BikeRepository {
    fun getAllBikes(): Flow<List<Bike>>
    fun getPlayerBikes(playerId: String): Flow<List<PlayerBike>>
    suspend fun getBikeById(id: String): Bike?
    suspend fun getPlayerBike(playerId: String, bikeId: String): PlayerBike?
    suspend fun unlockBike(playerId: String, bikeId: String): Boolean
    suspend fun upgradeBike(playerId: String, bikeId: String, type: BikeUpgradeType): Boolean
}

interface TrackRepository {
    fun getAllTracks(): Flow<List<Track>>
    suspend fun getTrackById(id: String): Track?
}

interface RaceRepository {
    fun getPlayerRaceResults(playerId: String): Flow<List<RaceResult>>
    fun getBestResultsForTrack(trackId: String): Flow<List<RaceResult>>
    suspend fun getPersonalBestLap(trackId: String, playerId: String): Long?
    suspend fun saveRaceResult(result: RaceResult)
}

interface LeaderboardRepository {
    fun getLeaderboard(trackId: String): Flow<List<LeaderboardEntry>>
    fun getGlobalLeaderboard(): Flow<List<LeaderboardEntry>>
    suspend fun refreshLeaderboard(trackId: String)
    suspend fun recordPlayerDistance(playerId: String, deltaKm: Float, topSpeedKmh: Float)
    suspend fun updatePlayerName(playerId: String, newName: String)
}

interface AchievementRepository {
    fun getAchievements(playerId: String): Flow<List<Achievement>>
    suspend fun checkAndUnlockAchievements(player: Player, recentRace: RaceResult? = null)
}

interface ChampionshipRepository {
    fun getChampionship(playerId: String, championshipId: String): Flow<Championship?>
    suspend fun recordChampionshipRace(playerId: String, championshipId: String, points: Int, isFinalRace: Boolean)
    suspend fun resetChampionship(playerId: String, championshipId: String)
}

interface DailyChallengeRepository {
    fun getDailyChallenge(): Flow<DailyChallenge?>
    suspend fun completeChallenge(challengeId: String, timeMs: Long)
}

interface SettingsRepository {
    val settingsFlow: Flow<GameSettings>
    suspend fun saveSettings(settings: GameSettings)
}

interface AuthRepository {
    suspend fun loginGuest(): AuthSession
    suspend fun loginEmail(email: String, pass: String): ApiResult<AuthSession>
    suspend fun registerEmail(email: String, pass: String, displayName: String = ""): ApiResult<AuthSession>
    suspend fun logout()
    fun getCurrentSession(): AuthSession
}
