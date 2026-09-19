package com.example.game.ai

import com.example.core.config.GameBalanceConfig
import com.example.core.model.Difficulty
import com.example.core.model.EffectiveBikeStats
import com.example.core.model.RacerState
import com.example.game.engine.Vector2D
import com.example.game.physics.BikeInput
import com.example.game.tracks.TrackLayout
import kotlin.math.*
import kotlin.random.Random

class OpponentAI(
    val racerId: String,
    val difficulty: Difficulty,
    private val preferredLaneOffset: Float = (Random.nextFloat() - 0.5f) * 40f
) {
    private val tuning = GameBalanceConfig.getAITuning(difficulty)
    private var randomMistakeTimer = 0f
    private var isMakingMistake = false

    fun computeInput(
        racer: RacerState,
        stats: EffectiveBikeStats,
        track: TrackLayout,
        playerRacer: RacerState?,
        dtSeconds: Float
    ): BikeInput {
        randomMistakeTimer -= dtSeconds
        if (randomMistakeTimer <= 0f) {
            isMakingMistake = Random.nextFloat() < tuning.mistakeProbability
            randomMistakeTimer = Random.nextFloat() * 4f + 2f
        }

        val waypoints = track.waypoints
        val wpCount = waypoints.size
        val currentPos = Vector2D(racer.posX, racer.posY)

        // Find closest waypoint
        var closestIdx = 0
        var closestDist = Float.MAX_VALUE
        for (i in 0 until wpCount) {
            val d = currentPos.distanceTo(waypoints[i])
            if (d < closestDist) {
                closestDist = d
                closestIdx = i
            }
        }

        // Lookahead target based on speed
        val lookaheadSteps = ((racer.speedKmh / 25f) + 4).toInt().coerceIn(3, 14)
        val targetIdx = (closestIdx + lookaheadSteps) % wpCount
        val targetWp = waypoints[targetIdx]
        val nextWp = waypoints[(targetIdx + 1) % wpCount]

        // Add lateral offset for organic multi-line racing
        val segmentDir = (nextWp - targetWp).normalized()
        val normal = Vector2D(-segmentDir.y, segmentDir.x)
        val adjustedTarget = targetWp + (normal * preferredLaneOffset)

        // Desired heading angle
        val toTarget = (adjustedTarget - currentPos)
        val targetAngle = atan2(toTarget.y, toTarget.x)

        var angleDiff = targetAngle - racer.angleRad
        while (angleDiff > PI) angleDiff -= (2 * PI).toFloat()
        while (angleDiff < -PI) angleDiff += (2 * PI).toFloat()

        // Steer calculation (combining waypoint angle, highway lane maintenance, and road curve compensation)
        val roadCurvature = com.example.game.environment.CityMilestoneCatalog.getRoadCurvature(racer.totalRaceDistance)
        val targetNormalizedLane = (preferredLaneOffset / 50f).coerceIn(-0.65f, 0.65f)
        val laneCorrection = (targetNormalizedLane - racer.posX) * 1.6f
        val curveCounterSteer = roadCurvature * 0.65f

        var steer = ((angleDiff * 1.4f) + laneCorrection + curveCounterSteer).coerceIn(-1.0f, 1.0f) * tuning.corneringSteerSmoothness
        if (isMakingMistake) {
            steer *= 0.35f
        }

        // Corner sharpness lookahead (check angle change between next few waypoints)
        val p1 = waypoints[(closestIdx + 4) % wpCount]
        val p2 = waypoints[(closestIdx + 8) % wpCount]
        val p3 = waypoints[(closestIdx + 12) % wpCount]
        val v1 = (p2 - p1).normalized()
        val v2 = (p3 - p2).normalized()
        val cornerSharpness = (1f - v1.dot(v2)).coerceIn(0f, 1f)

        // Throttle & Brake logic
        var throttle = 1.0f * tuning.accelerationRatio
        var brake = 0.0f

        val safeCornerSpeed = stats.topSpeedKmh * (1f - cornerSharpness * 0.45f) * tuning.topSpeedRatio
        if (racer.speedKmh > safeCornerSpeed && cornerSharpness > 0.15f) {
            throttle = 0.2f
            brake = (cornerSharpness * 0.8f).coerceIn(0f, 1f)
        }

        // Rubber-banding: adjust slightly relative to player
        if (playerRacer != null) {
            val distToPlayer = currentPos.distanceTo(Vector2D(playerRacer.posX, playerRacer.posY))
            if (racer.totalRaceDistance < playerRacer.totalRaceDistance - 150f) {
                // Falling behind - slight boost
                throttle = (throttle * 1.15f).coerceIn(0f, 1f)
            } else if (racer.totalRaceDistance > playerRacer.totalRaceDistance + 250f) {
                // Too far ahead - slight ease
                throttle *= 0.92f
            }
        }

        // Nitro usage on long straightaways
        val shouldUseNitro = cornerSharpness < 0.08f && racer.speedKmh > 120f && Random.nextFloat() < tuning.nitroUsageChance
        val nitro = shouldUseNitro && racer.isNitroActive

        return BikeInput(
            steer = steer,
            throttle = throttle,
            brake = brake,
            nitro = nitro
        )
    }
}
