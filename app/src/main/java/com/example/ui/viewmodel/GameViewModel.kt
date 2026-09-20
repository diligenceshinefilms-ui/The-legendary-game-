package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.audio.GameAudioManager
import com.example.core.database.LegendRacerDatabase
import com.example.core.datastore.SettingsDataStore
import com.example.core.model.*
import com.example.core.network.SupabaseClientProvider
import com.example.data.repository.*
import com.example.domain.usecase.*
import com.example.game.engine.GameEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    // Infrastructure & Repositories
    val database = LegendRacerDatabase.getInstance(application)
    val settingsDataStore = SettingsDataStore(application)
    val audioManager = GameAudioManager(application)
    val supabaseClient = SupabaseClientProvider(application)

    private val playerRepo = PlayerRepositoryImpl(database.playerDao(), database.syncQueueDao(), supabaseClient)
    private val bikeRepo = BikeRepositoryImpl(database.bikeDao(), database.playerDao())
    private val trackRepo = TrackRepositoryImpl(database.trackDao())
    private val raceRepo = RaceRepositoryImpl(database.raceResultDao(), database.syncQueueDao(), supabaseClient)
    private val leaderboardRepo = LeaderboardRepositoryImpl(database.leaderboardDao(), supabaseClient)
    private val achievementRepo = AchievementRepositoryImpl(database.achievementDao(), database.playerDao())
    private val championshipRepo = ChampionshipRepositoryImpl(database.championshipDao(), database.trackDao())
    private val dailyChallengeRepo = DailyChallengeRepositoryImpl(database.dailyChallengeDao())
    private val settingsRepo = SettingsRepositoryImpl(settingsDataStore)
    private val authRepo = AuthRepositoryImpl(supabaseClient, database.playerDao())

    // Domain Use Cases
    val getPlayerProfile = GetPlayerProfileUseCase(playerRepo)
    val getAvailableBikes = GetAvailableBikesUseCase(bikeRepo, playerRepo)
    val selectBikeUseCase = SelectBikeUseCase(playerRepo)
    val unlockBikeUseCase = UnlockBikeUseCase(bikeRepo)
    val upgradeBikeUseCase = UpgradeBikeUseCase(bikeRepo)
    val getTracks = GetTracksUseCase(trackRepo)
    val finishRaceUseCase = FinishRaceUseCase(playerRepo, raceRepo, achievementRepo, leaderboardRepo)
    val getLeaderboard = GetLeaderboardUseCase(leaderboardRepo)
    val getAchievements = GetAchievementsUseCase(achievementRepo)
    val getChampionship = GetChampionshipUseCase(championshipRepo)
    val getDailyChallenge = GetDailyChallengeUseCase(dailyChallengeRepo)
    val saveSettings = SaveSettingsUseCase(settingsRepo)
    val loadSettings = LoadSettingsUseCase(settingsRepo)
    val authUseCase = AuthUseCase(authRepo)

    // UI States
    val player: StateFlow<Player?> = getPlayerProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val bikes: StateFlow<List<EffectiveBikeStats>> = player
        .flatMapLatest { p ->
            getAvailableBikes(p?.id ?: "guest_player_1")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tracks: StateFlow<List<Track>> = getTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedTrack = MutableStateFlow<Track?>(null)
    val selectedBikeStats = MutableStateFlow<EffectiveBikeStats?>(null)
    val playerLivery = MutableStateFlow(PlayerLivery())

    val gameSettings: StateFlow<GameSettings> = loadSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GameSettings())

    val achievements: StateFlow<List<Achievement>> = player
        .flatMapLatest { p -> getAchievements(p?.id ?: "guest_player_1") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val championship: StateFlow<Championship?> = player
        .flatMapLatest { p -> getChampionship(p?.id ?: "guest_player_1") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dailyChallenge: StateFlow<DailyChallenge?> = getDailyChallenge()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active Race Engine State & Post-Race Replay Recording
    private val _activeEngine = MutableStateFlow<GameEngine?>(null)
    val activeEngine: StateFlow<GameEngine?> = _activeEngine.asStateFlow()

    val lastRecordedReplay = MutableStateFlow<List<ReplayFrame>>(emptyList())

    private var raceLoopJob: Job? = null
    val lastRaceReward = MutableStateFlow<RaceRewardSummary?>(null)

    init {
        viewModelScope.launch {
            gameSettings.collectLatest { s ->
                audioManager.updateSettings(s.sfxVolume, s.vibrationEnabled)
            }
        }
    }

    fun selectBike(bikeId: String) {
        viewModelScope.launch {
            selectBikeUseCase(bikeId)
            audioManager.playClick()
        }
    }

    fun unlockBike(bikeId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val playerId = player.value?.id ?: "guest_player_1"
            val success = unlockBikeUseCase(playerId, bikeId)
            if (success) {
                audioManager.playVictory()
                selectBikeUseCase(bikeId)
            }
            onResult(success)
        }
    }

    fun upgradeBike(bikeId: String, type: BikeUpgradeType, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val playerId = player.value?.id ?: "guest_player_1"
            val success = upgradeBikeUseCase(playerId, bikeId, type)
            if (success) {
                audioManager.playClick()
            }
            onResult(success)
        }
    }

    fun updatePlayerLivery(livery: PlayerLivery) {
        playerLivery.value = livery
        _activeEngine.value?.updatePlayerLivery(livery)
    }

    fun saveBikeLivery(bikeId: String, livery: PlayerLivery) {
        playerLivery.value = livery
        _activeEngine.value?.updatePlayerLivery(livery)
        audioManager.playClick()
    }

    fun startRace(
        track: Track,
        bikeStats: EffectiveBikeStats,
        laps: Int = 3,
        mode: RaceMode = RaceMode.QUICK_RACE
    ) {
        selectedTrack.value = track
        selectedBikeStats.value = bikeStats

        raceLoopJob?.cancel()
        val engine = GameEngine(
            track = track,
            playerBikeStats = bikeStats,
            totalLaps = laps,
            mode = mode,
            audioManager = audioManager,
            playerLivery = playerLivery.value
        )
        _activeEngine.value = engine

        raceLoopJob = viewModelScope.launch {
            var lastTime = System.nanoTime()
            while (isActive) {
                val now = System.nanoTime()
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = now

                engine.update(dt)

                // Check failure state (Crash into obstacle / barrier -> mission fail)
                if (engine.engineState.value == GameEngineState.MISSION_FAILED) {
                    val pId = player.value?.id ?: "guest_player_1"
                    val distKm = engine.totalDistanceMeters.value / 1000f
                    val topSpeed = engine.topSpeedKmh.value
                    if (distKm > 0.02f) {
                        playerRepo.addDistanceKm(distKm)
                        leaderboardRepo.recordPlayerDistance(pId, distKm, topSpeed)
                    }
                    lastRecordedReplay.value = engine.getRecordedReplayFrames()
                }

                // Check finish state
                if (engine.engineState.value == GameEngineState.FINISHED && lastRaceReward.value?.raceResult?.trackId != track.id) {
                    val pRank = engine.playerRank.value
                    val totalTime = engine.raceTimeMs.value
                    val bestLap = engine.bestLapMs.value
                    val pId = player.value?.id ?: "guest_player_1"
                    val distKm = engine.totalDistanceMeters.value / 1000f
                    val topSpeed = engine.topSpeedKmh.value

                    lastRecordedReplay.value = engine.getRecordedReplayFrames()

                    if (distKm > 0.02f) {
                        playerRepo.addDistanceKm(distKm)
                        leaderboardRepo.recordPlayerDistance(pId, distKm, topSpeed)
                    }

                    val rewardSummary = finishRaceUseCase(
                        playerId = pId,
                        track = track,
                        bike = bikeStats.bike,
                        position = pRank,
                        totalRacers = 6,
                        totalTimeMs = totalTime,
                        bestLapMs = bestLap,
                        laps = laps,
                        mode = mode
                    )
                    lastRaceReward.value = rewardSummary

                    if (mode == RaceMode.CHAMPIONSHIP) {
                        val points = when (pRank) { 1 -> 25; 2 -> 18; 3 -> 15; 4 -> 12; 5 -> 10; else -> 8 }
                        val isFinal = (championship.value?.currentRaceIndex ?: 0) >= 4
                        championshipRepo.recordChampionshipRace(pId, "season_1", points, isFinal)
                    } else if (mode == RaceMode.TIME_TRIAL && dailyChallenge.value?.trackId == track.id) {
                        if (totalTime <= (dailyChallenge.value?.targetTimeMs ?: Long.MAX_VALUE)) {
                            dailyChallengeRepo.completeChallenge("daily_challenge_today", totalTime)
                        }
                    }
                    break
                }
                delay(16) // ~60 FPS
            }
        }
    }

    fun updateRegisteredUserName(newName: String) {
        viewModelScope.launch {
            val pId = player.value?.id ?: "guest_player_1"
            playerRepo.updateDisplayName(newName)
            leaderboardRepo.updatePlayerName(pId, newName)
        }
    }

    fun refreshLeaderboard(trackId: String) {
        viewModelScope.launch {
            getLeaderboard.refresh(trackId)
        }
    }

    fun updatePlayerInput(steer: Float, throttle: Float, brake: Float, nitro: Boolean) {
        _activeEngine.value?.playerInput = com.example.game.physics.BikeInput(
            steer = steer,
            throttle = throttle,
            brake = brake,
            nitro = nitro
        )
    }

    fun speedUp() {
        _activeEngine.value?.speedUp()
    }

    fun speedDown() {
        _activeEngine.value?.speedDown()
    }

    fun pauseRace() {
        _activeEngine.value?.pause()
    }

    fun resumeRace() {
        _activeEngine.value?.resume()
    }

    fun restartRace() {
        lastRaceReward.value = null
        val tr = selectedTrack.value ?: return
        val b = selectedBikeStats.value ?: return
        startRace(tr, b, laps = _activeEngine.value?.totalLaps ?: 3, mode = _activeEngine.value?.mode ?: RaceMode.QUICK_RACE)
    }

    fun updateSettings(newSettings: GameSettings) {
        viewModelScope.launch {
            saveSettings(newSettings)
        }
    }

    override fun onCleared() {
        super.onCleared()
        raceLoopJob?.cancel()
        audioManager.release()
    }
}
