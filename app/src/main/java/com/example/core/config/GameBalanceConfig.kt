package com.example.core.config

import com.example.core.model.Bike
import com.example.core.model.BikeUpgradeType
import com.example.core.model.Difficulty
import com.example.core.model.EffectiveBikeStats
import com.example.core.model.PlayerBike

object GameBalanceConfig {
    const val MAX_UPGRADE_LEVEL = 5

    // Upgrade Cost multiplier per level
    fun getUpgradeCost(type: BikeUpgradeType, currentLevel: Int): Long {
        if (currentLevel >= MAX_UPGRADE_LEVEL) return 0L
        val baseCost = when (type) {
            BikeUpgradeType.ENGINE -> 500L
            BikeUpgradeType.TIRES -> 350L
            BikeUpgradeType.BRAKES -> 300L
            BikeUpgradeType.HANDLING -> 400L
            BikeUpgradeType.NITRO -> 600L
        }
        return (baseCost * Math.pow(1.5, currentLevel.toDouble())).toLong()
    }

    // Calculate effective stats from base bike + upgrades
    fun calculateEffectiveStats(bike: Bike, upgrades: PlayerBike): EffectiveBikeStats {
        // Each engine upgrade adds +6% top speed & +5% acceleration
        val engineBonus = (upgrades.engineLevel - 1) * 0.06f
        val tireBonus = (upgrades.tireLevel - 1) * 0.05f
        val brakeBonus = (upgrades.brakeLevel - 1) * 0.07f
        val handlingBonus = (upgrades.handlingLevel - 1) * 0.06f
        val nitroBonus = (upgrades.nitroLevel - 1) * 0.08f

        val topSpeedKmh = bike.baseTopSpeed * (1f + engineBonus + (tireBonus * 0.3f))
        val acceleration = bike.baseAcceleration * (1f + engineBonus * 0.8f + tireBonus * 0.4f)
        val handling = bike.baseHandling * (1f + handlingBonus + tireBonus * 0.5f)
        val braking = bike.baseBraking * (1f + brakeBonus + tireBonus * 0.3f)
        val nitroMultiplier = 1.35f + (bike.baseNitro / 100f) * 0.25f + nitroBonus * 0.2f
        val nitroDurationSec = 4.0f + (upgrades.nitroLevel - 1) * 0.75f

        return EffectiveBikeStats(
            bike = bike,
            topSpeedKmh = topSpeedKmh,
            acceleration = acceleration,
            handling = handling,
            braking = braking,
            nitroMultiplier = nitroMultiplier,
            nitroDurationSec = nitroDurationSec,
            upgradeLevels = upgrades
        )
    }

    // Rewards Calculation
    fun calculateRaceRewards(
        position: Int,
        totalRacers: Int,
        laps: Int,
        difficulty: Difficulty
    ): Pair<Long, Long> {
        val difficultyMultiplier = when (difficulty) {
            Difficulty.EASY -> 1.0f
            Difficulty.NORMAL -> 1.35f
            Difficulty.HARD -> 1.8f
        }

        val baseCoins = when (position) {
            1 -> 600L
            2 -> 400L
            3 -> 250L
            4 -> 150L
            5 -> 100L
            else -> 60L
        } * laps

        val baseXP = when (position) {
            1 -> 400L
            2 -> 250L
            3 -> 180L
            4 -> 120L
            else -> 70L
        } * laps

        val coins = (baseCoins * difficultyMultiplier).toLong()
        val xp = (baseXP * difficultyMultiplier).toLong()
        return Pair(coins, xp)
    }

    // AI tuning factors
    data class AITuning(
        val topSpeedRatio: Float,
        val accelerationRatio: Float,
        val corneringSteerSmoothness: Float,
        val mistakeProbability: Float,
        val nitroUsageChance: Float
    )

    fun getAITuning(difficulty: Difficulty): AITuning {
        return when (difficulty) {
            Difficulty.EASY -> AITuning(
                topSpeedRatio = 0.82f,
                accelerationRatio = 0.78f,
                corneringSteerSmoothness = 0.70f,
                mistakeProbability = 0.12f,
                nitroUsageChance = 0.05f
            )
            Difficulty.NORMAL -> AITuning(
                topSpeedRatio = 0.94f,
                accelerationRatio = 0.92f,
                corneringSteerSmoothness = 0.88f,
                mistakeProbability = 0.04f,
                nitroUsageChance = 0.15f
            )
            Difficulty.HARD -> AITuning(
                topSpeedRatio = 1.02f,
                accelerationRatio = 1.0f,
                corneringSteerSmoothness = 0.96f,
                mistakeProbability = 0.01f,
                nitroUsageChance = 0.30f
            )
        }
    }
}
