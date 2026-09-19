package com.example.core.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class GameAudioManager(private val context: Context) {
    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null
    private var isVibrationEnabled: Boolean = true
    private var sfxVolume: Float = 1.0f

    // Real-time procedural motorcycle engine sound synthesizer
    val engineSynthesizer = MotorcycleEngineSoundSynthesizer()

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, (sfxVolume * 100).toInt())
        } catch (e: Exception) {
            Log.e("GameAudioManager", "ToneGenerator init failed", e)
        }

        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            Log.e("GameAudioManager", "Vibrator init failed", e)
        }
    }

    fun updateSettings(sfxVol: Float, vibration: Boolean) {
        sfxVolume = sfxVol
        isVibrationEnabled = vibration
        engineSynthesizer.masterSfxVolume = sfxVol
        try {
            toneGenerator?.release()
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, (sfxVolume * 100).toInt().coerceIn(0, 100))
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun startEngineAudio() {
        engineSynthesizer.isMuted = false
        engineSynthesizer.masterSfxVolume = sfxVolume
        engineSynthesizer.start()
    }

    fun stopEngineAudio() {
        engineSynthesizer.stop()
    }

    fun pauseEngineAudio() {
        engineSynthesizer.isMuted = true
    }

    fun resumeEngineAudio() {
        engineSynthesizer.isMuted = false
    }

    fun updateEngineAudio(
        speedKmh: Float,
        gearNumber: Int,
        rpm: Int,
        isThrottle: Boolean,
        isBrake: Boolean,
        isNitro: Boolean,
        isRedlining: Boolean
    ) {
        engineSynthesizer.updateTelemetry(
            speedKmh = speedKmh,
            gearNumber = gearNumber,
            rpm = rpm,
            isThrottle = isThrottle,
            isBrake = isBrake,
            isNitro = isNitro,
            isRedlining = isRedlining
        )
    }

    fun playCountdownBeep(isFinal: Boolean = false) {
        if (sfxVolume <= 0.05f) return
        try {
            if (isFinal) {
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_D, 350)
                vibrate(100)
            } else {
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_1, 150)
                vibrate(40)
            }
        } catch (e: Exception) {
            // Ignore tone errors
        }
    }

    fun playNitroBoost() {
        if (sfxVolume <= 0.05f) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
            vibrate(80)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun playCheckpoint() {
        if (sfxVolume <= 0.05f) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
            vibrate(30)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun playCollision() {
        if (sfxVolume <= 0.05f) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 150)
            vibrate(120)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun playCrash() {
        if (sfxVolume <= 0.05f) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 500)
            vibrate(400)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun playVictory() {
        if (sfxVolume <= 0.05f) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_0, 400)
            vibrate(250)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun playClick() {
        if (sfxVolume <= 0.05f) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
            vibrate(15)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun vibrate(millis: Long) {
        if (!isVibrationEnabled || vibrator == null || !vibrator!!.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(millis)
            }
        } catch (e: Exception) {
            // Ignore vibration failure
        }
    }

    fun release() {
        try {
            engineSynthesizer.stop()
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}
