package com.example.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.core.model.Player
import com.example.core.model.RaceResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SupabaseClientProvider(
    private val context: Context,
    var supabaseUrl: String = "https://legend-racer-demo.supabase.co",
    var anonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.dummy-anon-key-legend-racer"
) {
    var authToken: String? = null
    var currentUserId: String? = "guest_player_1"
    var isGuestUser: Boolean = true

    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && !supabaseUrl.contains("dummy") && anonKey.isNotBlank() && !anonKey.contains("dummy")

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("apikey", anonKey)
            .header("Content-Type", "application/json")
            .header("Prefer", "return=representation")

        val token = authToken ?: anonKey
        requestBuilder.header("Authorization", "Bearer $token")

        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Direct Safe HTTP Methods with Offline fallback
    suspend fun getProfile(userId: String): ApiResult<ProfileDto> {
        if (!isNetworkAvailable()) return ApiResult.NetworkUnavailable
        if (!isConfigured) {
            return ApiResult.Success(
                ProfileDto(
                    id = userId,
                    display_name = "Racer Apex",
                    level = 1,
                    xp = 0,
                    coins = 1500,
                    wins = 0,
                    races_completed = 0
                )
            )
        }

        return try {
            val url = "$supabaseUrl/rest/v1/profiles?id=eq.$userId&select=*"
            val request = Request.Builder().url(url).get().build()
            val response: Response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            if (response.isSuccessful) {
                val array = JSONArray(body)
                if (array.length() > 0) {
                    val item = array.getJSONObject(0)
                    ApiResult.Success(
                        ProfileDto(
                            id = item.optString("id", userId),
                            display_name = item.optString("display_name", "Racer"),
                            avatar_url = item.optString("avatar_url", null),
                            level = item.optInt("level", 1),
                            xp = item.optLong("xp", 0L),
                            coins = item.optLong("coins", 1000L),
                            wins = item.optInt("wins", 0),
                            races_completed = item.optInt("races_completed", 0),
                            selected_bike_id = item.optString("selected_bike_id", "bike_legend_x1")
                        )
                    )
                } else {
                    ApiResult.Error(404, "Profile not found")
                }
            } else {
                ApiResult.Error(response.code, "Failed to load profile: ${response.message}")
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "getProfile failed", e)
            ApiResult.Error(cause = e, message = e.localizedMessage ?: "Network error")
        }
    }

    suspend fun updateProfile(player: Player): ApiResult<Boolean> {
        if (!isNetworkAvailable()) return ApiResult.NetworkUnavailable
        if (!isConfigured) return ApiResult.Success(true)

        return try {
            val url = "$supabaseUrl/rest/v1/profiles?id=eq.${player.id}"
            val json = JSONObject().apply {
                put("display_name", player.displayName)
                put("level", player.level)
                put("xp", player.xp)
                put("coins", player.coins)
                put("wins", player.wins)
                put("races_completed", player.racesCompleted)
                put("selected_bike_id", player.selectedBikeId)
            }
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).patch(requestBody).build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                ApiResult.Success(true)
            } else {
                ApiResult.Error(response.code, "Failed to update profile")
            }
        } catch (e: Exception) {
            ApiResult.Error(cause = e, message = e.localizedMessage ?: "Network error")
        }
    }

    suspend fun submitRaceResult(result: RaceResult): ApiResult<Boolean> {
        if (!isNetworkAvailable()) return ApiResult.NetworkUnavailable
        if (!isConfigured) return ApiResult.Success(true)

        return try {
            val url = "$supabaseUrl/rest/v1/race_results"
            val json = JSONObject().apply {
                put("id", result.id)
                put("player_id", result.playerId)
                put("track_id", result.trackId)
                put("bike_id", result.bikeId)
                put("position", result.position)
                put("time_ms", result.timeMs)
                put("laps", result.laps)
                put("coins_earned", result.coinsEarned)
                put("xp_earned", result.xpEarned)
            }
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                ApiResult.Success(true)
            } else {
                ApiResult.Error(response.code, "Failed to submit race result")
            }
        } catch (e: Exception) {
            ApiResult.Error(cause = e, message = e.localizedMessage ?: "Network error")
        }
    }

    suspend fun fetchLeaderboard(trackId: String): ApiResult<List<LeaderboardDto>> {
        if (!isNetworkAvailable()) return ApiResult.NetworkUnavailable
        if (!isConfigured) return ApiResult.Success(emptyList())

        return try {
            val url = "$supabaseUrl/rest/v1/leaderboard_scores?track_id=eq.$trackId&order=time_ms.asc&limit=50&select=*"
            val request = Request.Builder().url(url).get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val list = mutableListOf<LeaderboardDto>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        LeaderboardDto(
                            id = obj.optString("id"),
                            player_id = obj.optString("player_id"),
                            track_id = obj.optString("track_id"),
                            time_ms = obj.optLong("time_ms")
                        )
                    )
                }
                ApiResult.Success(list)
            } else {
                ApiResult.Error(response.code, "Failed to load leaderboard")
            }
        } catch (e: Exception) {
            ApiResult.Error(cause = e, message = e.localizedMessage ?: "Network error")
        }
    }

    suspend fun loginWithEmail(email: String, password: String): ApiResult<AuthSession> {
        if (!isNetworkAvailable()) return ApiResult.NetworkUnavailable
        if (!isConfigured) {
            // Local Guest Auth Session
            currentUserId = "guest_player_1"
            isGuestUser = false
            return ApiResult.Success(
                AuthSession(
                    accessToken = "offline_demo_token",
                    userId = "player_${email.hashCode()}",
                    userEmail = email,
                    isGuest = false
                )
            )
        }

        return try {
            val url = "$supabaseUrl/auth/v1/token?grant_type=password"
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            if (response.isSuccessful) {
                val obj = JSONObject(body)
                val token = obj.optString("access_token")
                val userObj = obj.optJSONObject("user")
                val uid = userObj?.optString("id") ?: "user_1"
                authToken = token
                currentUserId = uid
                isGuestUser = false
                ApiResult.Success(AuthSession(token, uid, email, false))
            } else {
                ApiResult.Error(response.code, "Authentication failed: ${response.message}")
            }
        } catch (e: Exception) {
            ApiResult.Error(cause = e, message = e.localizedMessage ?: "Network error")
        }
    }

    suspend fun registerWithEmail(email: String, password: String): ApiResult<AuthSession> {
        if (!isNetworkAvailable()) return ApiResult.NetworkUnavailable
        if (!isConfigured) {
            return ApiResult.Success(
                AuthSession(
                    accessToken = "offline_demo_token",
                    userId = "player_${email.hashCode()}",
                    userEmail = email,
                    isGuest = false
                )
            )
        }

        return try {
            val url = "$supabaseUrl/auth/v1/signup"
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            if (response.isSuccessful) {
                val obj = JSONObject(body)
                val token = obj.optString("access_token")
                val userObj = obj.optJSONObject("user")
                val uid = userObj?.optString("id") ?: "user_${System.currentTimeMillis()}"
                authToken = token
                currentUserId = uid
                isGuestUser = false
                ApiResult.Success(AuthSession(token, uid, email, false))
            } else {
                ApiResult.Error(response.code, "Signup failed")
            }
        } catch (e: Exception) {
            ApiResult.Error(cause = e, message = e.localizedMessage ?: "Network error")
        }
    }
}
