package com.example.domain.usecase

import com.example.core.config.GameBalanceConfig
import com.example.core.model.*
import com.example.core.network.ApiResult
import com.example.core.network.AuthSession
import com.example.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class GetPlayerProfileUseCase(private val playerRepo: PlayerRepository) {
    operator fun invoke(): Flow<Player?> = playerRepo.getPlayer()
    suspend fun getDirect(): Player? = playerRepo.getPlayerDirect()
}

class GetAvailableBikesUseCase(
    private val bikeRepo: BikeRepository,
    private val playerRepo: PlayerRepository
) {
    operator fun invoke(playerId: String): Flow<List<EffectiveBikeStats>> {
        return combine(
            bikeRepo.getAllBikes(),
            bikeRepo.getPlayerBikes(playerId)
        ) { allBikes, playerBikes ->
            val playerBikeMap = playerBikes.associateBy { it.bikeId }
            allBikes.map { bike ->
                val pBike = playerBikeMap[bike.id] ?: PlayerBike(
                    id = "pb_${bike.id}",
                    playerId = playerId,
                    bikeId = bike.id,
                    unlocked = bike.price == 0L
                )
                GameBalanceConfig.calculateEffectiveStats(bike, pBike)
            }
        }
    }
}

class SelectBikeUseCase(private val playerRepo: PlayerRepository) {
    suspend operator fun invoke(bikeId: String) {
        playerRepo.selectBike(bikeId)
    }
}

class UnlockBikeUseCase(private val bikeRepo: BikeRepository) {
    suspend operator fun invoke(playerId: String, bikeId: String): Boolean {
        return bikeRepo.unlockBike(playerId, bikeId)
    }
}

class UpgradeBikeUseCase(private val bikeRepo: BikeRepository) {
    suspend operator fun invoke(playerId: String, bikeId: String, type: BikeUpgradeType): Boolean {
        return bikeRepo.upgradeBike(playerId, bikeId, type)
    }
}

class GetTracksUseCase(private val trackRepo: TrackRepository) {
    operator fun invoke(): Flow<List<Track>> = trackRepo.getAllTracks()
}

class FinishRaceUseCase(
    private val playerRepo: PlayerRepository,
    private val raceRepo: RaceRepository,
    private val achievementRepo: AchievementRepository,
    private val leaderboardRepo: LeaderboardRepository
) {
    suspend operator fun invoke(
        playerId: String,
        track: Track,
        bike: Bike,
        position: Int,
        totalRacers: Int,
        totalTimeMs: Long,
        bestLapMs: Long,
        laps: Int,
        mode: RaceMode
    ): RaceRewardSummary {
        val (earnedCoins, earnedXp) = GameBalanceConfig.calculateRaceRewards(
            position = position,
            totalRacers = totalRacers,
            laps = laps,
            difficulty = track.difficulty
        )

        val isWin = position == 1
        val (updatedPlayer, leveledUp) = playerRepo.awardRaceRewards(earnedCoins, earnedXp, isWin)

        val raceResult = RaceResult(
            id = UUID.randomUUID().toString(),
            playerId = playerId,
            trackId = track.id,
            trackName = track.name,
            bikeId = bike.id,
            bikeName = bike.name,
            position = position,
            timeMs = totalTimeMs,
            bestLapMs = bestLapMs,
            laps = laps,
            coinsEarned = earnedCoins,
            xpEarned = earnedXp,
            mode = mode
        )
        raceRepo.saveRaceResult(raceResult)

        // Trigger achievement check
        achievementRepo.checkAndUnlockAchievements(updatedPlayer, raceResult)

        return RaceRewardSummary(
            coinsEarned = earnedCoins,
            xpEarned = earnedXp,
            newCoinsTotal = updatedPlayer.coins,
            newXpTotal = updatedPlayer.xp,
            newLevel = updatedPlayer.level,
            isLevelUp = leveledUp,
            isVictory = isWin,
            raceResult = raceResult
        )
    }
}

data class RaceRewardSummary(
    val coinsEarned: Long,
    val xpEarned: Long,
    val newCoinsTotal: Long,
    val newXpTotal: Long,
    val newLevel: Int,
    val isLevelUp: Boolean,
    val isVictory: Boolean,
    val raceResult: RaceResult
)

class GetLeaderboardUseCase(private val leaderboardRepo: LeaderboardRepository) {
    operator fun invoke(trackId: String): Flow<List<LeaderboardEntry>> = leaderboardRepo.getLeaderboard(trackId)
    fun getGlobal(): Flow<List<LeaderboardEntry>> = leaderboardRepo.getGlobalLeaderboard()
    suspend fun refresh(trackId: String) = leaderboardRepo.refreshLeaderboard(trackId)
    suspend fun recordPlayerDistance(playerId: String, deltaKm: Float, topSpeedKmh: Float) =
        leaderboardRepo.recordPlayerDistance(playerId, deltaKm, topSpeedKmh)
    suspend fun updatePlayerName(playerId: String, newName: String) =
        leaderboardRepo.updatePlayerName(playerId, newName)
}

class GetAchievementsUseCase(private val achievementRepo: AchievementRepository) {
    operator fun invoke(playerId: String): Flow<List<Achievement>> = achievementRepo.getAchievements(playerId)
}

class GetChampionshipUseCase(private val championshipRepo: ChampionshipRepository) {
    operator fun invoke(playerId: String, championshipId: String = "season_1"): Flow<Championship?> =
        championshipRepo.getChampionship(playerId, championshipId)

    suspend fun recordRace(playerId: String, championshipId: String, points: Int, isFinal: Boolean) =
        championshipRepo.recordChampionshipRace(playerId, championshipId, points, isFinal)

    suspend fun reset(playerId: String, championshipId: String) =
        championshipRepo.resetChampionship(playerId, championshipId)
}

class GetDailyChallengeUseCase(private val dailyChallengeRepo: DailyChallengeRepository) {
    operator fun invoke(): Flow<DailyChallenge?> = dailyChallengeRepo.getDailyChallenge()
    suspend fun complete(challengeId: String, timeMs: Long) = dailyChallengeRepo.completeChallenge(challengeId, timeMs)
}

class SaveSettingsUseCase(private val settingsRepo: SettingsRepository) {
    suspend operator fun invoke(settings: GameSettings) = settingsRepo.saveSettings(settings)
}

class LoadSettingsUseCase(private val settingsRepo: SettingsRepository) {
    operator fun invoke(): Flow<GameSettings> = settingsRepo.settingsFlow
}

class AuthUseCase(private val authRepo: AuthRepository) {
    suspend fun loginGuest(): AuthSession = authRepo.loginGuest()
    suspend fun loginEmail(email: String, pass: String): ApiResult<AuthSession> = authRepo.loginEmail(email, pass)
    suspend fun registerEmail(email: String, pass: String, displayName: String = ""): ApiResult<AuthSession> =
        authRepo.registerEmail(email, pass, displayName)
    suspend fun logout() = authRepo.logout()
    fun getCurrentSession(): AuthSession = authRepo.getCurrentSession()
}
