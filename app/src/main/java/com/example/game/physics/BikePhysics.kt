package com.example.game.physics

import com.example.core.model.EffectiveBikeStats
import com.example.core.model.RacerState
import com.example.game.engine.Vector2D
import com.example.game.environment.CityMilestoneCatalog
import com.example.game.tracks.TrackLayout
import kotlin.math.*

data class BikeInput(
    val steer: Float = 0f, // -1.0 (left) to 1.0 (right)
    val throttle: Float = 0f, // 0.0 to 1.0
    val brake: Float = 0f, // 0.0 to 1.0
    val nitro: Boolean = false
)

object BikePhysics {

    fun updateRacer(
        racer: RacerState,
        input: BikeInput,
        stats: EffectiveBikeStats,
        track: TrackLayout,
        dtSeconds: Float
    ): RacerState {
        val dt = dtSeconds.coerceIn(0.001f, 0.05f)

        // 1. Procedural Road Curvature at Current Track Position
        val roadCurve = CityMilestoneCatalog.getRoadCurvature(racer.totalRaceDistance)

        // 2. Steering & Angular Velocity (Rider Control + Road Curvature Lean Dynamics)
        val currentSpeed = racer.speedKmh
        // At high speeds, make steering smoother and less twitchy
        val speedHandlingMultiplier = (1.0f / (1.0f + (currentSpeed / 150f) * 0.65f)).coerceIn(0.42f, 1.0f)
        val steerSensitivity = (stats.handling / 100f) * 2.8f * speedHandlingMultiplier

        val gyroStability = (currentSpeed / 90f).coerceIn(0.2f, 1.0f)
        val targetLean = if (racer.isPlayer) {
            if (abs(input.steer) > 0.01f) {
                (input.steer * 0.78f * gyroStability).coerceIn(-0.85f, 0.85f)
            } else if (abs(roadCurve) > 0.15f && currentSpeed > 40f) {
                // Subtle natural banking into road curve even before full steer input
                (roadCurve * 0.22f * gyroStability).coerceIn(-0.4f, 0.4f)
            } else {
                0.0f
            }
        } else {
            (input.steer * 0.78f * gyroStability).coerceIn(-0.85f, 0.85f)
        }
        val leanReturnRate = if (abs(input.steer) <= 0.01f) 16f else 12f
        val newLeanAngleRad = racer.leanAngleRad + (targetLean - racer.leanAngleRad) * leanReturnRate * dt

        // Angular heading: steered smoothly via lean & handlebars
        val turnRate = (input.steer * 0.6f + racer.leanAngleRad * 0.4f) * steerSensitivity * dt
        var newAngleRad = racer.angleRad + turnRate
        while (newAngleRad > PI) newAngleRad -= (2 * PI).toFloat()
        while (newAngleRad < -PI) newAngleRad += (2 * PI).toFloat()

        // Dynamic Suspension Pitch & Weight Transfer (Acceleration Squat vs Brake Dive)
        val targetPitch = when {
            input.brake > 0.05f -> (input.brake * 0.14f).coerceIn(0f, 0.18f) // Brake dive
            currentSpeed > 10f -> (-0.08f * (currentSpeed / stats.topSpeedKmh).coerceIn(0f, 1f)) // Acceleration squat
            else -> 0f // Level
        }
        val newPitchAngleRad = racer.pitchAngleRad + (targetPitch - racer.pitchAngleRad) * 14f * dt

        // 3. Off-road & Rumble strip detection on highway shoulders
        val isOffRoad = abs(racer.posX) > 0.91f
        val surfaceGrip = if (isOffRoad) 0.78f else 1.0f

        // 4. Acceleration and Speed
        var newSpeedKmh = if (racer.isPlayer) {
            val speedLimit = stats.topSpeedKmh * (if (input.nitro && racer.isNitroActive) stats.nitroMultiplier else 1.0f)
            if (isOffRoad) {
                (racer.speedKmh - 15f * dt).coerceIn(0f, speedLimit * 0.82f)
            } else {
                racer.speedKmh.coerceIn(0f, speedLimit)
            }
        } else {
            var topSpeed = stats.topSpeedKmh * surfaceGrip
            var accelPower = (stats.acceleration * 1.6f) * surfaceGrip
            if (input.nitro && racer.isNitroActive) {
                topSpeed *= stats.nitroMultiplier
                accelPower *= 1.85f
            }
            var speed = currentSpeed
            if (input.throttle > 0.05f) {
                val speedRatio = (currentSpeed / topSpeed).coerceIn(0f, 1f)
                val effectiveAccel = accelPower * input.throttle * (1f - speedRatio * 0.82f)
                speed += effectiveAccel * dt
            }
            if (input.brake > 0.05f) {
                val brakeForce = stats.braking * 3.2f
                speed -= brakeForce * input.brake * dt
            }
            val naturalDecel = if (isOffRoad) 30f else 5f
            if (input.throttle <= 0.05f && input.brake <= 0.05f) {
                speed -= naturalDecel * dt
            }
            if (abs(racer.posX) >= 0.95f) {
                speed -= 40f * dt
            }
            speed.coerceIn(0f, topSpeed)
        }

        // 5. Procedural Curve Drift & Rider Steering Requirement
        // - Steer input provides responsive lateral authority across the wide multi-lane highway.
        // - Centrifugal inertia pushes the bike outward when road curves, requiring the player to counter-steer to stay on track.
        val steerSpeedFactor = (currentSpeed / 45f).coerceIn(0.40f, 1.35f)
        val steerVelocity = input.steer * 3.2f * speedHandlingMultiplier * steerSpeedFactor
        
        // Centrifugal inertia pulling outward relative to the curving road:
        // When road bends right (roadCurve > 0), centrifugal force pushes bike left (negative posX)
        // When road bends left (roadCurve < 0), centrifugal force pushes bike right (positive posX)
        val centrifugalDrift = if (currentSpeed > 10f) {
            -roadCurve * (currentSpeed / 130f).coerceIn(0.2f, 2.2f) * 1.55f
        } else {
            0f
        }

        val lateralVelocity = if (currentSpeed > 0f) {
            steerVelocity + centrifugalDrift
        } else {
            0f
        }

        val newPosX = (racer.posX + lateralVelocity * dt).coerceIn(-0.96f, 0.96f)

        // 6. Longitudinal World Distance (Stationary at speed 0)
        val speedMps = (newSpeedKmh * 1000f) / 3600f
        val totalDist = racer.totalRaceDistance + (speedMps * dt)

        return racer.copy(
            posX = newPosX,
            posY = racer.posY + speedMps * dt,
            speedKmh = newSpeedKmh,
            angleRad = newAngleRad,
            leanAngleRad = newLeanAngleRad,
            totalRaceDistance = totalDist,
            isNitroActive = input.nitro && racer.isNitroActive,
            pitchAngleRad = newPitchAngleRad,
            throttleRatio = if (newSpeedKmh > 0f) (newSpeedKmh / stats.topSpeedKmh).coerceIn(0.2f, 1f) else 0f,
            brakeRatio = input.brake
        )
    }

    fun handleRacerCollisions(racers: List<RacerState>): List<RacerState> {
        val count = racers.size
        val result = racers.toMutableList()

        for (i in 0 until count) {
            for (j in i + 1 until count) {
                val r1 = result[i]
                val r2 = result[j]

                val lateralDiff = r1.posX - r2.posX
                val longDist = abs(r1.totalRaceDistance - r2.totalRaceDistance)

                // Realistic highway collision bounding box: 0.20 lane width and 4.2m longitudinal distance
                if (abs(lateralDiff) < 0.20f && longDist < 4.2f) {
                    val push = if (lateralDiff >= 0f) 0.035f else -0.035f
                    val newR1X = (r1.posX + push).coerceIn(-0.95f, 0.95f)
                    val newR2X = (r2.posX - push).coerceIn(-0.95f, 0.95f)

                    result[i] = r1.copy(posX = newR1X, speedKmh = (r1.speedKmh * 0.95f).coerceAtLeast(0f))
                    result[j] = r2.copy(posX = newR2X, speedKmh = (r2.speedKmh * 0.95f).coerceAtLeast(0f))
                }
            }
        }
        return result
    }
}
