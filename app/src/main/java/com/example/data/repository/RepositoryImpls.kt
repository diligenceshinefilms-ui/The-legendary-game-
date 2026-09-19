package com.example.data.repository

import com.example.core.config.GameBalanceConfig
import com.example.core.database.*
import com.example.core.datastore.SettingsDataStore
import com.example.core.model.*
import com.example.core.model.Track
import com.example.core.model.RaceResult
import com.example.core.network.ApiResult
import com.example.core.network.AuthSession
import com.example.core.network.SupabaseClientProvider
import com.example.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class PlayerRepositoryImpl(
    private val playerDao: PlayerDao,
    private val syncQueueDao: SyncQueueDao,
    private val supabaseClient: SupabaseClientProvider
) : PlayerRepository {

    override fun getPlayer(): Flow<Player?> = playerDao.getPlayer().map { entity ->
        entity?.let {
            Player(
                id = it.id,
                displayName = it.displayName,
                avatarUrl = it.avatarUrl,
                level = it.level,
                xp = it.xp,
                coins = it.coins,
                wins = it.wins,
                racesCompleted = it.racesCompleted,
                selectedBikeId = it.selectedBikeId,
                isGuest = it.isGuest
            )
        }
    }

    override suspend fun getPlayerDirect(): Player? {
        val it = playerDao.getPlayerDirect() ?: return null
        return Player(
            id = it.id,
            displayName = it.displayName,
            avatarUrl = it.avatarUrl,
            level = it.level,
            xp = it.xp,
            coins = it.coins,
            wins = it.wins,
            racesCompleted = it.racesCompleted,
            selectedBikeId = it.selectedBikeId,
            isGuest = it.isGuest
        )
    }

    override suspend fun updatePlayer(player: Player) {
        playerDao.insertOrUpdate(
            PlayerEntity(
                id = player.id,
                displayName = player.displayName,
                avatarUrl = player.avatarUrl,
                level = player.level,
                xp = player.xp,
                coins = player.coins,
                wins = player.wins,
                racesCompleted = player.racesCompleted,
                selectedBikeId = player.selectedBikeId,
                isGuest = player.isGuest
            )
        )
        // Background sync attempt
        supabaseClient.updateProfile(player)
    }

    override suspend fun awardRaceRewards(coins: Long, xp: Long, won: Boolean): Pair<Player, Boolean> {
        val current = getPlayerDirect() ?: return Pair(Player("guest_player_1", "Racer Apex"), false)
        val newCoins = current.coins + coins
        var newXp = current.xp + xp
        var newLevel = current.level
        var leveledUp = false

        while (newXp >= (newLevel * 1000L)) {
            newXp -= (newLevel * 1000L)
            newLevel++
            leveledUp = true
        }

        val updated = current.copy(
            coins = newCoins,
            xp = newXp,
            level = newLevel,
            racesCompleted = current.racesCompleted + 1,
            wins = if (won) current.wins + 1 else current.wins
        )
        updatePlayer(updated)
        return Pair(updated, leveledUp)
    }

    override suspend fun selectBike(bikeId: String) {
        val current = getPlayerDirect() ?: return
        playerDao.setSelectedBike(current.id, bikeId)
    }

    override suspend fun updateDisplayName(newName: String) {
        val current = getPlayerDirect() ?: return
        playerDao.updateDisplayName(current.id, newName)
    }

    override suspend fun addDistanceKm(deltaKm: Float) {
        val current = getPlayerDirect() ?: return
        playerDao.addDistanceKm(current.id, deltaKm)
    }

    override suspend fun syncWithRemote() {
        val player = getPlayerDirect() ?: return
        supabaseClient.updateProfile(player)
    }
}

class BikeRepositoryImpl(
    private val bikeDao: BikeDao,
    private val playerDao: PlayerDao
) : BikeRepository {

    override fun getAllBikes(): Flow<List<Bike>> = bikeDao.getAllBikes().map { list ->
        list.map {
            Bike(
                id = it.id,
                name = it.name,
                description = it.description,
                baseTopSpeed = it.baseTopSpeed,
                baseAcceleration = it.baseAcceleration,
                baseHandling = it.baseHandling,
                baseBraking = it.baseBraking,
                baseNitro = it.baseNitro,
                price = it.price,
                unlockLevel = it.unlockLevel,
                primaryColorHex = it.primaryColorHex,
                secondaryColorHex = it.secondaryColorHex
            )
        }
    }

    override fun getPlayerBikes(playerId: String): Flow<List<PlayerBike>> =
        bikeDao.getPlayerBikes(playerId).map { list ->
            list.map {
                PlayerBike(
                    id = it.id,
                    playerId = it.playerId,
                    bikeId = it.bikeId,
                    unlocked = it.unlocked,
                    engineLevel = it.engineLevel,
                    tireLevel = it.tireLevel,
                    brakeLevel = it.brakeLevel,
                    handlingLevel = it.handlingLevel,
                    nitroLevel = it.nitroLevel
                )
            }
        }

    override suspend fun getBikeById(id: String): Bike? {
        val it = bikeDao.getBikeById(id) ?: return null
        return Bike(
            id = it.id,
            name = it.name,
            description = it.description,
            baseTopSpeed = it.baseTopSpeed,
            baseAcceleration = it.baseAcceleration,
            baseHandling = it.baseHandling,
            baseBraking = it.baseBraking,
            baseNitro = it.baseNitro,
            price = it.price,
            unlockLevel = it.unlockLevel,
            primaryColorHex = it.primaryColorHex,
            secondaryColorHex = it.secondaryColorHex
        )
    }

    override suspend fun getPlayerBike(playerId: String, bikeId: String): PlayerBike? {
        val it = bikeDao.getPlayerBike(playerId, bikeId) ?: return null
        return PlayerBike(
            id = it.id,
            playerId = it.playerId,
            bikeId = it.bikeId,
            unlocked = it.unlocked,
            engineLevel = it.engineLevel,
            tireLevel = it.tireLevel,
            brakeLevel = it.brakeLevel,
            handlingLevel = it.handlingLevel,
            nitroLevel = it.nitroLevel
        )
    }

    override suspend fun unlockBike(playerId: String, bikeId: String): Boolean {
        val player = playerDao.getPlayerDirect() ?: return false
        val bike = getBikeById(bikeId) ?: return false
        if (player.coins < bike.price) return false

        val currentBike = getPlayerBike(playerId, bikeId)
        val updated = currentBike?.copy(unlocked = true)
            ?: PlayerBikeEntity("pb_${bikeId}", playerId, bikeId, true, 1, 1, 1, 1, 1)

        bikeDao.insertPlayerBike(
            if (updated is PlayerBike) {
                PlayerBikeEntity(
                    id = updated.id,
                    playerId = updated.playerId,
                    bikeId = updated.bikeId,
                    unlocked = true,
                    engineLevel = updated.engineLevel,
                    tireLevel = updated.tireLevel,
                    brakeLevel = updated.brakeLevel,
                    handlingLevel = updated.handlingLevel,
                    nitroLevel = updated.nitroLevel
                )
            } else {
                updated as PlayerBikeEntity
            }
        )

        playerDao.updateCoinsAndXp(playerId, -bike.price, 0L, player.level)
        return true
    }

    override suspend fun upgradeBike(playerId: String, bikeId: String, type: BikeUpgradeType): Boolean {
        val player = playerDao.getPlayerDirect() ?: return false
        val pb = getPlayerBike(playerId, bikeId) ?: return false

        val currentLevel = when (type) {
            BikeUpgradeType.ENGINE -> pb.engineLevel
            BikeUpgradeType.TIRES -> pb.tireLevel
            BikeUpgradeType.BRAKES -> pb.brakeLevel
            BikeUpgradeType.HANDLING -> pb.handlingLevel
            BikeUpgradeType.NITRO -> pb.nitroLevel
        }

        if (currentLevel >= GameBalanceConfig.MAX_UPGRADE_LEVEL) return false
        val cost = GameBalanceConfig.getUpgradeCost(type, currentLevel)
        if (player.coins < cost) return false

        val updatedEntity = PlayerBikeEntity(
            id = pb.id,
            playerId = playerId,
            bikeId = bikeId,
            unlocked = pb.unlocked,
            engineLevel = if (type == BikeUpgradeType.ENGINE) pb.engineLevel + 1 else pb.engineLevel,
            tireLevel = if (type == BikeUpgradeType.TIRES) pb.tireLevel + 1 else pb.tireLevel,
            brakeLevel = if (type == BikeUpgradeType.BRAKES) pb.brakeLevel + 1 else pb.brakeLevel,
            handlingLevel = if (type == BikeUpgradeType.HANDLING) pb.handlingLevel + 1 else pb.handlingLevel,
            nitroLevel = if (type == BikeUpgradeType.NITRO) pb.nitroLevel + 1 else pb.nitroLevel
        )
        bikeDao.updatePlayerBike(updatedEntity)
        playerDao.updateCoinsAndXp(playerId, -cost, 0L, player.level)
        return true
    }
}

class TrackRepositoryImpl(private val trackDao: TrackDao) : TrackRepository {
    override fun getAllTracks(): Flow<List<Track>> = trackDao.getAllTracks().map { list ->
        list.map {
            Track(
                id = it.id,
                name = it.name,
                description = it.description,
                difficulty = try { Difficulty.valueOf(it.difficulty) } catch (e: Exception) { Difficulty.NORMAL },
                defaultLaps = it.defaultLaps,
                environment = it.environment,
                unlockLevel = it.unlockLevel,
                roadWidth = it.roadWidth,
                lengthMeters = it.lengthMeters,
                themeColorHex = it.themeColorHex
            )
        }
    }

    override suspend fun getTrackById(id: String): Track? {
        val it = trackDao.getTrackById(id) ?: return null
        return Track(
            id = it.id,
            name = it.name,
            description = it.description,
            difficulty = try { Difficulty.valueOf(it.difficulty) } catch (e: Exception) { Difficulty.NORMAL },
            defaultLaps = it.defaultLaps,
            environment = it.environment,
            unlockLevel = it.unlockLevel,
            roadWidth = it.roadWidth,
            lengthMeters = it.lengthMeters,
            themeColorHex = it.themeColorHex
        )
    }
}

class RaceRepositoryImpl(
    private val raceResultDao: RaceResultDao,
    private val syncQueueDao: SyncQueueDao,
    private val supabaseClient: SupabaseClientProvider
) : RaceRepository {

    override fun getPlayerRaceResults(playerId: String): Flow<List<RaceResult>> =
        raceResultDao.getPlayerRaceResults(playerId).map { list ->
            list.map {
                RaceResult(
                    id = it.id,
                    playerId = it.playerId,
                    trackId = it.trackId,
                    trackName = it.trackName,
                    bikeId = it.bikeId,
                    bikeName = it.bikeName,
                    position = it.position,
                    timeMs = it.timeMs,
                    bestLapMs = it.bestLapMs,
                    laps = it.laps,
                    coinsEarned = it.coinsEarned,
                    xpEarned = it.xpEarned,
                    mode = try { RaceMode.valueOf(it.mode) } catch (e: Exception) { RaceMode.QUICK_RACE },
                    timestamp = it.timestamp
                )
            }
        }

    override fun getBestResultsForTrack(trackId: String): Flow<List<RaceResult>> =
        raceResultDao.getBestResultsForTrack(trackId).map { list ->
            list.map {
                RaceResult(
                    id = it.id,
                    playerId = it.playerId,
                    trackId = it.trackId,
                    trackName = it.trackName,
                    bikeId = it.bikeId,
                    bikeName = it.bikeName,
                    position = it.position,
                    timeMs = it.timeMs,
                    bestLapMs = it.bestLapMs,
                    laps = it.laps,
                    coinsEarned = it.coinsEarned,
                    xpEarned = it.xpEarned,
                    mode = try { RaceMode.valueOf(it.mode) } catch (e: Exception) { RaceMode.QUICK_RACE },
                    timestamp = it.timestamp
                )
            }
        }

    override suspend fun getPersonalBestLap(trackId: String, playerId: String): Long? {
        return raceResultDao.getPersonalBestLap(trackId, playerId)
    }

    override suspend fun saveRaceResult(result: RaceResult) {
        val entity = RaceResultEntity(
            id = result.id,
            playerId = result.playerId,
            trackId = result.trackId,
            trackName = result.trackName,
            bikeId = result.bikeId,
            bikeName = result.bikeName,
            position = result.position,
            timeMs = result.timeMs,
            bestLapMs = result.bestLapMs,
            laps = result.laps,
            coinsEarned = result.coinsEarned,
            xpEarned = result.xpEarned,
            mode = result.mode.name,
            timestamp = result.timestamp
        )
        raceResultDao.insertRaceResult(entity)

        // Asynchronously submit or enqueue
        val res = supabaseClient.submitRaceResult(result)
        if (res is ApiResult.Error || res is ApiResult.NetworkUnavailable) {
            syncQueueDao.enqueue(
                SyncQueueEntity(
                    actionType = "RACE_RESULT",
                    payloadJson = """{"id":"${result.id}","timeMs":${result.timeMs}}"""
                )
            )
        }
    }
}

class LeaderboardRepositoryImpl(
    private val leaderboardDao: LeaderboardDao,
    private val supabaseClient: SupabaseClientProvider
) : LeaderboardRepository {

    override fun getLeaderboard(trackId: String): Flow<List<LeaderboardEntry>> =
        leaderboardDao.getScoresForTrack(trackId).map { list ->
            list.mapIndexed { idx, entity ->
                LeaderboardEntry(
                    id = entity.id,
                    rank = idx + 1,
                    playerId = entity.playerId,
                    playerName = entity.playerName,
                    trackId = entity.trackId,
                    trackName = entity.trackName,
                    bikeName = entity.bikeName,
                    timeMs = entity.timeMs,
                    distanceKm = entity.distanceKm,
                    topSpeedKmh = entity.topSpeedKmh,
                    isCurrentPlayer = entity.isCurrentPlayer,
                    timestamp = entity.timestamp
                )
            }
        }

    override fun getGlobalLeaderboard(): Flow<List<LeaderboardEntry>> =
        leaderboardDao.getGlobalScores().map { list ->
            list.mapIndexed { idx, entity ->
                LeaderboardEntry(
                    id = entity.id,
                    rank = idx + 1,
                    playerId = entity.playerId,
                    playerName = entity.playerName,
                    trackId = entity.trackId,
                    trackName = entity.trackName,
                    bikeName = entity.bikeName,
                    timeMs = entity.timeMs,
                    distanceKm = entity.distanceKm,
                    topSpeedKmh = entity.topSpeedKmh,
                    isCurrentPlayer = entity.isCurrentPlayer,
                    timestamp = entity.timestamp
                )
            }
        }

    override suspend fun recordPlayerDistance(playerId: String, deltaKm: Float, topSpeedKmh: Float) {
        leaderboardDao.addDistanceToPlayer(playerId, deltaKm, topSpeedKmh)
    }

    override suspend fun updatePlayerName(playerId: String, newName: String) {
        leaderboardDao.updatePlayerName(playerId, newName)
    }

    override suspend fun refreshLeaderboard(trackId: String) {
        val remoteRes = supabaseClient.fetchLeaderboard(trackId)
        if (remoteRes is ApiResult.Success) {
            val entities = remoteRes.data.mapIndexed { idx, dto ->
                LeaderboardEntity(
                    id = dto.id,
                    rank = idx + 1,
                    playerId = dto.player_id,
                    playerName = dto.player_name ?: "Racer #${dto.player_id.takeLast(4)}",
                    trackId = dto.track_id,
                    trackName = dto.track_name ?: "Circuit",
                    bikeName = dto.bike_name ?: "Legend Superbike",
                    timeMs = dto.time_ms,
                    isCurrentPlayer = dto.player_id == supabaseClient.currentUserId,
                    timestamp = System.currentTimeMillis()
                )
            }
            if (entities.isNotEmpty()) {
                leaderboardDao.clearScoresForTrack(trackId)
                leaderboardDao.insertScores(entities)
            }
        }
    }
}

class AchievementRepositoryImpl(
    private val achievementDao: AchievementDao,
    private val playerDao: PlayerDao
) : AchievementRepository {

    override fun getAchievements(playerId: String): Flow<List<Achievement>> =
        combine(
            achievementDao.getAllAchievements(),
            achievementDao.getPlayerAchievements(playerId)
        ) { all, playerAch ->
            val playerAchMap = playerAch.associateBy { it.achievementId }
            all.map { ach ->
                val pAch = playerAchMap[ach.id]
                Achievement(
                    id = ach.id,
                    name = ach.name,
                    description = ach.description,
                    requirementType = ach.requirementType,
                    requirementValue = ach.requirementValue,
                    rewardCoins = ach.rewardCoins,
                    rewardXp = ach.rewardXp,
                    isUnlocked = pAch?.isUnlocked ?: false,
                    currentProgress = pAch?.currentProgress ?: 0,
                    unlockedAt = pAch?.unlockedAt
                )
            }
        }

    override suspend fun checkAndUnlockAchievements(player: Player, recentRace: RaceResult?) {
        val playerAch = achievementDao.getPlayerAchievements(player.id)
        // Check conditions
        if (player.racesCompleted >= 1) {
            achievementDao.unlockAchievement(player.id, "ach_first_race", 1, System.currentTimeMillis())
        }
        if (player.wins >= 1) {
            achievementDao.unlockAchievement(player.id, "ach_first_win", 1, System.currentTimeMillis())
        }
        if (player.racesCompleted >= 10) {
            achievementDao.unlockAchievement(player.id, "ach_10_races", 10, System.currentTimeMillis())
        }
        if (player.racesCompleted >= 25) {
            achievementDao.unlockAchievement(player.id, "ach_25_races", 25, System.currentTimeMillis())
        }
        if (player.racesCompleted >= 50) {
            achievementDao.unlockAchievement(player.id, "ach_50_races", 50, System.currentTimeMillis())
        }
        if (recentRace != null && recentRace.position == 1) {
            achievementDao.unlockAchievement(player.id, "ach_perfect_finish", 1, System.currentTimeMillis())
        }
    }
}

class ChampionshipRepositoryImpl(
    private val championshipDao: ChampionshipDao,
    private val trackDao: TrackDao
) : ChampionshipRepository {

    override fun getChampionship(playerId: String, championshipId: String): Flow<Championship?> =
        championshipDao.getProgress(playerId, championshipId).map { progress ->
            Championship(
                id = championshipId,
                name = "Apex Grand Prix Season 1",
                description = "5-stage high speed tournament spanning all circuits.",
                requiredLevel = 1,
                currentRaceIndex = progress?.currentRaceIndex ?: 0,
                totalPoints = progress?.totalPoints ?: 0,
                isCompleted = progress?.isCompleted ?: false,
                races = listOf(
                    ChampionshipRace("cr_1", championshipId, "track_city_rush", 1, 3),
                    ChampionshipRace("cr_2", championshipId, "track_desert_storm", 2, 3),
                    ChampionshipRace("cr_3", championshipId, "track_mountain_edge", 3, 3),
                    ChampionshipRace("cr_4", championshipId, "track_night_highway", 4, 3),
                    ChampionshipRace("cr_5", championshipId, "track_coastal_run", 5, 3)
                )
            )
        }

    override suspend fun recordChampionshipRace(playerId: String, championshipId: String, points: Int, isFinalRace: Boolean) {
        val current = championshipDao.getProgressDirect(playerId, championshipId)
        val entity = ChampionshipProgressEntity(
            id = "cp_${playerId}_${championshipId}",
            playerId = playerId,
            championshipId = championshipId,
            currentRaceIndex = (current?.currentRaceIndex ?: 0) + 1,
            totalPoints = (current?.totalPoints ?: 0) + points,
            isCompleted = isFinalRace
        )
        championshipDao.insertOrUpdate(entity)
    }

    override suspend fun resetChampionship(playerId: String, championshipId: String) {
        val entity = ChampionshipProgressEntity(
            id = "cp_${playerId}_${championshipId}",
            playerId = playerId,
            championshipId = championshipId,
            currentRaceIndex = 0,
            totalPoints = 0,
            isCompleted = false
        )
        championshipDao.insertOrUpdate(entity)
    }
}

class DailyChallengeRepositoryImpl(
    private val dailyChallengeDao: DailyChallengeDao
) : DailyChallengeRepository {

    override fun getDailyChallenge(): Flow<DailyChallenge?> =
        dailyChallengeDao.getLatestChallenge().map { entity ->
            entity?.let {
                DailyChallenge(
                    id = it.id,
                    dateString = it.dateString,
                    trackId = it.trackId,
                    trackName = it.trackName,
                    targetTimeMs = it.targetTimeMs,
                    rewardCoins = it.rewardCoins,
                    rewardXp = it.rewardXp,
                    isCompleted = it.isCompleted,
                    bestAttemptMs = it.bestAttemptMs
                )
            }
        }

    override suspend fun completeChallenge(challengeId: String, timeMs: Long) {
        dailyChallengeDao.markCompleted(challengeId, timeMs)
    }
}

class SettingsRepositoryImpl(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {
    override val settingsFlow: Flow<GameSettings> = settingsDataStore.settingsFlow

    override suspend fun saveSettings(settings: GameSettings) {
        settingsDataStore.saveSettings(settings)
    }
}

class AuthRepositoryImpl(
    private val supabaseClient: SupabaseClientProvider,
    private val playerDao: PlayerDao,
    private val leaderboardDao: LeaderboardDao? = null
) : AuthRepository {

    override suspend fun loginGuest(): AuthSession {
        supabaseClient.isGuestUser = true
        supabaseClient.currentUserId = "guest_player_1"
        return AuthSession(null, "guest_player_1", "guest@legendracer.local", true)
    }

    override suspend fun loginEmail(email: String, pass: String): ApiResult<AuthSession> {
        val res = supabaseClient.loginWithEmail(email, pass)
        if (res is ApiResult.Success) {
            val session = res.data
            val cleanName = email.substringBefore("@")
            val localPlayer = playerDao.getPlayerDirect()
            if (localPlayer != null) {
                playerDao.insertOrUpdate(
                    localPlayer.copy(
                        id = session.userId ?: localPlayer.id,
                        displayName = cleanName,
                        isGuest = false
                    )
                )
                leaderboardDao?.updatePlayerName(localPlayer.id, cleanName)
            }
        }
        return res
    }

    override suspend fun registerEmail(email: String, pass: String, displayName: String): ApiResult<AuthSession> {
        val res = supabaseClient.registerWithEmail(email, pass)
        if (res is ApiResult.Success) {
            val session = res.data
            val cleanName = if (displayName.isNotBlank()) displayName.trim() else email.substringBefore("@")
            val localPlayer = playerDao.getPlayerDirect()
            if (localPlayer != null) {
                playerDao.insertOrUpdate(
                    localPlayer.copy(
                        id = session.userId ?: localPlayer.id,
                        displayName = cleanName,
                        isGuest = false
                    )
                )
                leaderboardDao?.updatePlayerName(localPlayer.id, cleanName)
            }
        }
        return res
    }

    override suspend fun logout() {
        supabaseClient.authToken = null
        supabaseClient.isGuestUser = true
        supabaseClient.currentUserId = "guest_player_1"
    }

    override fun getCurrentSession(): AuthSession {
        return AuthSession(
            accessToken = supabaseClient.authToken,
            userId = supabaseClient.currentUserId,
            userEmail = if (supabaseClient.isGuestUser) "Guest Racer" else "racer@cloud.net",
            isGuest = supabaseClient.isGuestUser
        )
    }
}
