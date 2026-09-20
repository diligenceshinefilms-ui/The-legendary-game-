package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.core.model.BikeUpgradeType
import com.example.core.model.RaceMode
import com.example.ui.screens.*
import com.example.ui.theme.CarbonDark
import com.example.ui.theme.LegendRacerTheme
import com.example.ui.viewmodel.GameViewModel

object NavigationRoutes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val QUICK_RACE = "quick_race"
    const val GARAGE = "garage"
    const val RACE = "race"
    const val RACE_RESULTS = "race_results"
    const val REPLAY = "replay"
    const val CHAMPIONSHIP = "championship"
    const val TIME_TRIAL = "time_trial"
    const val PRACTICE = "practice"
    const val LEADERBOARD = "leaderboard"
    const val ACHIEVEMENTS = "achievements"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val DAILY_CHALLENGE = "daily_challenge"
    const val MULTIPLAYER = "multiplayer"
    const val AUTH = "auth"
}

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LegendRacerTheme {
                val navController = rememberNavController()

                val player by viewModel.player.collectAsState()
                val bikes by viewModel.bikes.collectAsState()
                val tracks by viewModel.tracks.collectAsState()
                val gameSettings by viewModel.gameSettings.collectAsState()
                val achievements by viewModel.achievements.collectAsState()
                val championship by viewModel.championship.collectAsState()
                val dailyChallenge by viewModel.dailyChallenge.collectAsState()
                val lastRaceReward by viewModel.lastRaceReward.collectAsState()
                val activeEngineState by viewModel.activeEngine.collectAsState()
                val lastRecordedReplayState by viewModel.lastRecordedReplay.collectAsState()
                val selectedTrackState by viewModel.selectedTrack.collectAsState()
                val selectedBikeStatsState by viewModel.selectedBikeStats.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CarbonDark)
                        .safeDrawingPadding()
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = NavigationRoutes.SPLASH
                    ) {
                        composable(NavigationRoutes.SPLASH) {
                            SplashScreen(
                                onSplashFinished = {
                                    navController.navigate(NavigationRoutes.HOME) {
                                        popUpTo(NavigationRoutes.SPLASH) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(NavigationRoutes.HOME) {
                            HomeScreen(
                                player = player,
                                bikes = bikes,
                                onQuickRace = { navController.navigate(NavigationRoutes.QUICK_RACE) },
                                onGarage = { navController.navigate(NavigationRoutes.GARAGE) },
                                onChampionship = { navController.navigate(NavigationRoutes.CHAMPIONSHIP) },
                                onTimeTrial = { navController.navigate(NavigationRoutes.TIME_TRIAL) },
                                onPractice = {
                                    val firstTrack = tracks.firstOrNull()
                                    val equippedBike = bikes.firstOrNull { it.bike.id == player?.selectedBikeId } ?: bikes.firstOrNull()
                                    if (firstTrack != null && equippedBike != null) {
                                        viewModel.startRace(firstTrack, equippedBike, laps = 1, mode = RaceMode.PRACTICE)
                                        navController.navigate(NavigationRoutes.RACE)
                                    }
                                },
                                onLeaderboard = { navController.navigate(NavigationRoutes.LEADERBOARD) },
                                onAchievements = { navController.navigate(NavigationRoutes.ACHIEVEMENTS) },
                                onProfile = { navController.navigate(NavigationRoutes.PROFILE) },
                                onSettings = { navController.navigate(NavigationRoutes.SETTINGS) },
                                onDailyChallenge = { navController.navigate(NavigationRoutes.DAILY_CHALLENGE) },
                                onMultiplayer = { navController.navigate(NavigationRoutes.MULTIPLAYER) },
                                onAuth = { navController.navigate(NavigationRoutes.AUTH) }
                            )
                        }

                        composable(NavigationRoutes.QUICK_RACE) {
                            QuickRaceScreen(
                                player = player,
                                tracks = tracks,
                                bikes = bikes,
                                onBack = { navController.popBackStack() },
                                onStartRace = { track, bikeStats, laps ->
                                    viewModel.startRace(track, bikeStats, laps = laps, mode = RaceMode.QUICK_RACE)
                                    navController.navigate(NavigationRoutes.RACE)
                                }
                            )
                        }

                        composable(NavigationRoutes.GARAGE) {
                            val playerLivery by viewModel.playerLivery.collectAsState()
                            GarageScreen(
                                player = player,
                                bikes = bikes,
                                onBack = { navController.popBackStack() },
                                onSelectBike = { bikeId -> viewModel.selectBike(bikeId) },
                                onUnlockBike = { bikeId -> viewModel.unlockBike(bikeId) {} },
                                onUpgradeBike = { bikeId, type -> viewModel.upgradeBike(bikeId, type) {} },
                                onSaveLivery = { bikeId, livery -> viewModel.saveBikeLivery(bikeId, livery) },
                                initialLivery = playerLivery
                            )
                        }

                        composable(NavigationRoutes.RACE) {
                            val activeEngine = activeEngineState
                            if (activeEngine != null) {
                                RaceScreen(
                                    engine = activeEngine,
                                    settings = gameSettings,
                                    playerCoins = player?.coins ?: 45670L,
                                    onFinishRace = {
                                        navController.navigate(NavigationRoutes.RACE_RESULTS) {
                                            popUpTo(NavigationRoutes.RACE) { inclusive = true }
                                        }
                                    },
                                    onExitToMenu = {
                                        navController.navigate(NavigationRoutes.HOME) {
                                            popUpTo(NavigationRoutes.HOME) { inclusive = true }
                                        }
                                    },
                                    onInputUpdate = { steer, throttle, brake, nitro ->
                                        viewModel.updatePlayerInput(steer, throttle, brake, nitro)
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(CarbonDark),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = com.example.ui.theme.NeonCyan)
                                }
                                LaunchedEffect(Unit) {
                                    if (viewModel.activeEngine.value == null) {
                                        navController.popBackStack()
                                    }
                                }
                            }
                        }

                        composable(NavigationRoutes.RACE_RESULTS) {
                            RaceResultsScreen(
                                summary = lastRaceReward,
                                onWatchReplay = {
                                    navController.navigate(NavigationRoutes.REPLAY)
                                },
                                onRestartRace = {
                                    viewModel.restartRace()
                                    navController.navigate(NavigationRoutes.RACE) {
                                        popUpTo(NavigationRoutes.RACE_RESULTS) { inclusive = true }
                                    }
                                },
                                onGarage = {
                                    navController.navigate(NavigationRoutes.GARAGE) {
                                        popUpTo(NavigationRoutes.HOME)
                                    }
                                },
                                onHome = {
                                    navController.navigate(NavigationRoutes.HOME) {
                                        popUpTo(NavigationRoutes.HOME) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(NavigationRoutes.REPLAY) {
                            CinematicReplayScreen(
                                replayFrames = lastRecordedReplayState,
                                track = selectedTrackState,
                                playerBike = selectedBikeStatsState,
                                onBackToResults = { navController.popBackStack() }
                            )
                        }

                        composable(NavigationRoutes.CHAMPIONSHIP) {
                            val equippedBike = bikes.firstOrNull { it.bike.id == player?.selectedBikeId } ?: bikes.firstOrNull()
                            ChampionshipScreen(
                                championship = championship,
                                tracks = tracks,
                                playerBike = equippedBike,
                                onBack = { navController.popBackStack() },
                                onStartStage = { track, bikeStats, laps ->
                                    viewModel.startRace(track, bikeStats, laps = laps, mode = RaceMode.CHAMPIONSHIP)
                                    navController.navigate(NavigationRoutes.RACE)
                                }
                            )
                        }

                        composable(NavigationRoutes.TIME_TRIAL) {
                            val equippedBike = bikes.firstOrNull { it.bike.id == player?.selectedBikeId } ?: bikes.firstOrNull()
                            TimeTrialScreen(
                                tracks = tracks,
                                playerBike = equippedBike,
                                onBack = { navController.popBackStack() },
                                onStartTimeTrial = { track, bikeStats ->
                                    viewModel.startRace(track, bikeStats, laps = 1, mode = RaceMode.TIME_TRIAL)
                                    navController.navigate(NavigationRoutes.RACE)
                                }
                            )
                        }

                        composable(NavigationRoutes.LEADERBOARD) {
                            var currentTrackId by remember { mutableStateOf("track_city_rush") }
                            val leaderboards by viewModel.getLeaderboard(currentTrackId).collectAsState(initial = emptyList())
                            LeaderboardScreen(
                                tracks = tracks,
                                leaderboardEntries = leaderboards,
                                registeredPlayer = player,
                                onUpdateRegisteredName = { newName ->
                                    viewModel.updateRegisteredUserName(newName)
                                },
                                onBack = { navController.popBackStack() },
                                onTrackSelect = { tid -> currentTrackId = tid },
                                onRefresh = { tid -> viewModel.refreshLeaderboard(tid) }
                            )
                        }

                        composable(NavigationRoutes.ACHIEVEMENTS) {
                            AchievementsScreen(
                                achievements = achievements,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(NavigationRoutes.PROFILE) {
                            ProfileScreen(
                                player = player,
                                onBack = { navController.popBackStack() },
                                onAuthClick = { navController.navigate(NavigationRoutes.AUTH) }
                            )
                        }

                        composable(NavigationRoutes.SETTINGS) {
                            SettingsScreen(
                                settings = gameSettings,
                                onBack = { navController.popBackStack() },
                                onSaveSettings = { viewModel.updateSettings(it) }
                            )
                        }

                        composable(NavigationRoutes.DAILY_CHALLENGE) {
                            val equippedBike = bikes.firstOrNull { it.bike.id == player?.selectedBikeId } ?: bikes.firstOrNull()
                            DailyChallengeScreen(
                                challenge = dailyChallenge,
                                tracks = tracks,
                                playerBike = equippedBike,
                                onBack = { navController.popBackStack() },
                                onStartChallenge = { track, bikeStats ->
                                    viewModel.startRace(track, bikeStats, laps = 1, mode = RaceMode.TIME_TRIAL)
                                    navController.navigate(NavigationRoutes.RACE)
                                }
                            )
                        }

                        composable(NavigationRoutes.MULTIPLAYER) {
                            MultiplayerRoadmapScreen(
                                onBack = { navController.popBackStack() },
                                onJoinBetaWaitlist = { navController.popBackStack() }
                            )
                        }

                        composable(NavigationRoutes.AUTH) {
                            AuthScreen(
                                authUseCase = viewModel.authUseCase,
                                onBack = { navController.popBackStack() },
                                onAuthSuccess = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
