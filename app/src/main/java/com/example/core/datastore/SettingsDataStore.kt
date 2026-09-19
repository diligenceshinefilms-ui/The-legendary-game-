package com.example.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.model.ControlType
import com.example.core.model.GameSettings
import com.example.core.model.GraphicsQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "legend_racer_settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val KEY_MUSIC_VOL = floatPreferencesKey("music_volume")
        val KEY_SFX_VOL = floatPreferencesKey("sfx_volume")
        val KEY_VIBRATION = booleanPreferencesKey("vibration_enabled")
        val KEY_CONTROL_TYPE = stringPreferencesKey("control_type")
        val KEY_GRAPHICS_QUALITY = stringPreferencesKey("graphics_quality")
        val KEY_TILT_ENABLED = booleanPreferencesKey("tilt_enabled")
        val KEY_AUTO_ACCEL = booleanPreferencesKey("auto_accelerate")
        val KEY_TILT_SENSITIVITY = floatPreferencesKey("tilt_sensitivity")
        val KEY_TILT_DEADZONE = floatPreferencesKey("tilt_deadzone")
        val KEY_INVERT_TILT = booleanPreferencesKey("invert_tilt")
    }

    val settingsFlow: Flow<GameSettings> = context.dataStore.data.map { prefs ->
        GameSettings(
            musicVolume = prefs[KEY_MUSIC_VOL] ?: 0.8f,
            sfxVolume = prefs[KEY_SFX_VOL] ?: 1.0f,
            vibrationEnabled = prefs[KEY_VIBRATION] ?: true,
            controlType = try {
                ControlType.valueOf(prefs[KEY_CONTROL_TYPE] ?: ControlType.BUTTONS.name)
            } catch (e: Exception) {
                ControlType.BUTTONS
            },
            graphicsQuality = try {
                GraphicsQuality.valueOf(prefs[KEY_GRAPHICS_QUALITY] ?: GraphicsQuality.HIGH.name)
            } catch (e: Exception) {
                GraphicsQuality.HIGH
            },
            tiltEnabled = prefs[KEY_TILT_ENABLED] ?: false,
            autoAccelerate = prefs[KEY_AUTO_ACCEL] ?: false,
            tiltSensitivity = prefs[KEY_TILT_SENSITIVITY] ?: 1.0f,
            tiltDeadzone = prefs[KEY_TILT_DEADZONE] ?: 0.05f,
            invertTilt = prefs[KEY_INVERT_TILT] ?: false
        )
    }

    suspend fun saveSettings(settings: GameSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MUSIC_VOL] = settings.musicVolume
            prefs[KEY_SFX_VOL] = settings.sfxVolume
            prefs[KEY_VIBRATION] = settings.vibrationEnabled
            prefs[KEY_CONTROL_TYPE] = settings.controlType.name
            prefs[KEY_GRAPHICS_QUALITY] = settings.graphicsQuality.name
            prefs[KEY_TILT_ENABLED] = settings.tiltEnabled
            prefs[KEY_AUTO_ACCEL] = settings.autoAccelerate
            prefs[KEY_TILT_SENSITIVITY] = settings.tiltSensitivity
            prefs[KEY_TILT_DEADZONE] = settings.tiltDeadzone
            prefs[KEY_INVERT_TILT] = settings.invertTilt
        }
    }
}
