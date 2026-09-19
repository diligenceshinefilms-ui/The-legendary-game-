package com.example.game.engine

enum class ShiftPrompt {
    NEUTRAL,
    CRUISING,
    ACCELERATING,
    SHIFT_UP_SOON,
    SHIFT_UP_NOW,
    DECELERATING,
    DOWNSHIFT_SOON,
    DOWNSHIFT_NOW,
    MAX_GEAR
}

data class BikeTelemetry(
    val speedKmh: Float,
    val gear: String,        // "N", "1", "2", "3", "4", "5", "6"
    val gearNumber: Int,     // 0 to 6
    val rpm: Int,            // 1,000 to 14,000
    val rpmFraction: Float,  // 0.0 to 1.0
    val isRedlining: Boolean, // true when > 88% of gear max RPM
    val minGearSpeed: Float = 0f,
    val maxGearSpeed: Float = 60f,
    val powerBandFraction: Float = 0f,
    val shiftPrompt: ShiftPrompt = ShiftPrompt.CRUISING
)

object TelemetryCalculator {

    fun calculate(
        speedKmh: Float,
        isNitroActive: Boolean = false,
        speedDelta: Float = 0f,
        isThrottle: Boolean = false,
        isBrake: Boolean = false
    ): BikeTelemetry {
        val speed = speedKmh.coerceAtLeast(0f)

        val (gear, gearNum, minSpd, maxSpd) = when {
            speed < 2f -> Quadruple("N", 0, 0f, 5f)
            speed < 60f -> Quadruple("1", 1, 0f, 60f)
            speed < 110f -> Quadruple("2", 2, 48f, 110f)
            speed < 165f -> Quadruple("3", 3, 98f, 165f)
            speed < 225f -> Quadruple("4", 4, 150f, 225f)
            speed < 290f -> Quadruple("5", 5, 210f, 290f)
            else -> Quadruple("6", 6, 275f, 380f)
        }

        val rpmFraction: Float
        val rpm: Int
        val powerBandFraction: Float

        if (gearNum == 0) {
            rpmFraction = if (isThrottle) 0.65f else 0.12f
            rpm = if (isThrottle) 6500 else 1200
            powerBandFraction = 0f
        } else {
            val range = (maxSpd - minSpd).coerceAtLeast(1f)
            val ratio = ((speed - minSpd) / range).coerceIn(0f, 1f)
            powerBandFraction = ratio
            val boostBonus = if (isNitroActive) 0.08f else 0f
            rpmFraction = (0.35f + ratio * 0.65f + boostBonus).coerceIn(0.2f, 1.0f)
            rpm = (3500 + ratio * 10500).toInt()
        }

        val isRedlining = rpmFraction > 0.88f

        val isAcc = speedDelta > 0.2f || (isThrottle && !isBrake)
        val isDec = speedDelta < -0.2f || isBrake

        val shiftPrompt = when {
            gearNum == 0 -> ShiftPrompt.NEUTRAL
            gearNum == 6 && rpmFraction > 0.92f -> ShiftPrompt.MAX_GEAR
            isAcc && isRedlining -> ShiftPrompt.SHIFT_UP_NOW
            isAcc && rpmFraction > 0.76f && gearNum < 6 -> ShiftPrompt.SHIFT_UP_SOON
            isAcc -> ShiftPrompt.ACCELERATING
            isDec && gearNum > 1 && powerBandFraction < 0.18f -> ShiftPrompt.DOWNSHIFT_NOW
            isDec && gearNum > 1 && powerBandFraction < 0.35f -> ShiftPrompt.DOWNSHIFT_SOON
            isDec -> ShiftPrompt.DECELERATING
            else -> ShiftPrompt.CRUISING
        }

        return BikeTelemetry(
            speedKmh = speed,
            gear = gear,
            gearNumber = gearNum,
            rpm = rpm,
            rpmFraction = rpmFraction,
            isRedlining = isRedlining,
            minGearSpeed = minSpd,
            maxGearSpeed = maxSpd,
            powerBandFraction = powerBandFraction,
            shiftPrompt = shiftPrompt
        )
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
