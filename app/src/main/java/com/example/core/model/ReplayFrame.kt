package com.example.core.model

/**
 * Snapshot frame representing 1 tick of recorded player inputs and bike physics state.
 * Used for 30-second post-race Cinematic Replay playback.
 */
data class ReplayFrame(
    val timestampMs: Long,
    val steer: Float,
    val throttle: Float,
    val brake: Float,
    val nitro: Boolean,
    val posX: Float,
    val totalRaceDistance: Float,
    val speedKmh: Float,
    val leanAngleRad: Float
)
