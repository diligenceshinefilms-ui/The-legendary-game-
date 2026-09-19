package com.example.core.network

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int? = null, val message: String, val cause: Throwable? = null) : ApiResult<Nothing>()
    object NetworkUnavailable : ApiResult<Nothing>()
}

data class ProfileDto(
    val id: String,
    val display_name: String,
    val avatar_url: String? = null,
    val level: Int = 1,
    val xp: Long = 0,
    val coins: Long = 1000,
    val wins: Int = 0,
    val races_completed: Int = 0,
    val selected_bike_id: String? = "bike_legend_x1"
)

data class BikeDto(
    val id: String,
    val name: String,
    val description: String,
    val top_speed: Int,
    val acceleration: Int,
    val handling: Int,
    val braking: Int,
    val nitro: Int,
    val price: Long,
    val unlock_level: Int
)

data class PlayerBikeDto(
    val id: String,
    val player_id: String,
    val bike_id: String,
    val unlocked: Boolean,
    val engine_level: Int,
    val tire_level: Int,
    val brake_level: Int,
    val handling_level: Int,
    val nitro_level: Int
)

data class TrackDto(
    val id: String,
    val name: String,
    val description: String,
    val difficulty: String,
    val laps: Int,
    val environment: String,
    val unlock_level: Int
)

data class RaceResultDto(
    val id: String,
    val player_id: String,
    val track_id: String,
    val bike_id: String,
    val position: Int,
    val time_ms: Long,
    val laps: Int,
    val coins_earned: Long,
    val xp_earned: Long,
    val created_at: String? = null
)

data class LeaderboardDto(
    val id: String,
    val player_id: String,
    val track_id: String,
    val time_ms: Long,
    val player_name: String? = null,
    val track_name: String? = null,
    val bike_name: String? = null
)

data class AuthSession(
    val accessToken: String?,
    val userId: String?,
    val userEmail: String?,
    val isGuest: Boolean
)
