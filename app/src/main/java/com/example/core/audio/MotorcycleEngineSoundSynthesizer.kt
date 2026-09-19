package com.example.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.*

/**
 * Real-Time Procedural Motorcycle Engine Sound Synthesizer.
 * Generates dynamic pitch (frequency), harmonics, gear shift drops,
 * rev-limiter crackles, nitro turbine whine, and load-based volume modulation.
 */
class MotorcycleEngineSoundSynthesizer {

    private val sampleRate = 22050
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var audioThread: Thread? = null

    // Target parameters updated from GameEngine / Telemetry
    @Volatile var targetRpm: Float = 1200f
    @Volatile var targetSpeedKmh: Float = 0f
    @Volatile var targetGear: Int = 1
    @Volatile var isThrottleApplied: Boolean = false
    @Volatile var isBrakeApplied: Boolean = false
    @Volatile var isNitroActive: Boolean = false
    @Volatile var isRedlineLimit: Boolean = false
    @Volatile var masterSfxVolume: Float = 1.0f
    @Volatile var isMuted: Boolean = false

    // Previous gear to detect gear shift drops
    private var previousGear: Int = 1
    private var gearShiftTimer: Float = 0f // In seconds

    // Synthesis phase accumulators
    private var phase: Double = 0.0
    private var subPhase: Double = 0.0
    private var nitroPhase: Double = 0.0
    private var limiterTimer: Double = 0.0
    private var crackleNoisePhase: Double = 0.0

    // Realistic riding environmental audio filters
    private var windFilterState: Float = 0f
    private var roadFilterState: Float = 0f
    private var brakeScrubFilter: Float = 0f
    private var brakeScrubPhase: Double = 0.0

    // Smoothed audio parameters to eliminate clicks and steps
    private var currentFreq: Float = 42f
    private var currentVolume: Float = 0.2f

    fun start() {
        if (isPlaying) return
        isPlaying = true

        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = (minBufferSize * 2).coerceAtLeast(1024)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            audioTrack = AudioTrack(
                audioAttributes,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM,
                android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            audioTrack?.play()

            audioThread = Thread({
                runSynthesisLoop(bufferSize)
            }, "MotorcycleAudioSynthesizer").apply {
                priority = Thread.MAX_PRIORITY
                isDaemon = true
                start()
            }
        } catch (e: Exception) {
            Log.e("EngineSoundSynth", "Failed to initialize AudioTrack", e)
            isPlaying = false
        }
    }

    fun stop() {
        isPlaying = false
        try {
            audioThread?.interrupt()
            audioThread = null
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            // Ignore clean teardown
        }
    }

    /**
     * Updates real-time telemetry inputs from the engine.
     */
    fun updateTelemetry(
        speedKmh: Float,
        gearNumber: Int,
        rpm: Int,
        isThrottle: Boolean,
        isBrake: Boolean,
        isNitro: Boolean,
        isRedlining: Boolean
    ) {
        if (gearNumber != previousGear && gearNumber > 0 && previousGear > 0) {
            // Trigger quick gear shift dip
            gearShiftTimer = 0.12f // 120ms ignition dip
            previousGear = gearNumber
        } else {
            previousGear = gearNumber
        }

        targetSpeedKmh = speedKmh
        targetGear = gearNumber
        targetRpm = rpm.toFloat()
        isThrottleApplied = isThrottle
        isBrakeApplied = isBrake
        isNitroActive = isNitro
        isRedlineLimit = isRedlining
    }

    private fun runSynthesisLoop(bufferSize: Int) {
        val chunkSamples = 512
        val shortBuffer = ShortArray(chunkSamples)
        val dtSample = 1.0 / sampleRate

        while (isPlaying) {
            if (isMuted || masterSfxVolume <= 0.02f) {
                // Output silence without burning CPU
                shortBuffer.fill(0)
                audioTrack?.write(shortBuffer, 0, chunkSamples)
                continue
            }

            // Calculate target fundamental frequency based on RPM and Gear status
            // Idle 1200 RPM -> ~38 Hz
            // Mid 7000 RPM -> ~220 Hz
            // Top 14000 RPM -> ~440 Hz
            var rawFreq = (targetRpm / 60.0f) * 2.0f // 2 combustion pulses per rev for twin/4-cyl
            rawFreq = rawFreq.coerceIn(36.0f, 490.0f)

            // Dynamic gear shift simulation: momentary RPM dip and backfire pop
            if (gearShiftTimer > 0f) {
                val shiftProgress = (gearShiftTimer / 0.12f).coerceIn(0f, 1f)
                rawFreq *= (0.65f + 0.35f * (1f - shiftProgress))
                gearShiftTimer -= (chunkSamples * dtSample).toFloat()
            }

            // Pitch bend / Nitro frequency boost
            if (isNitroActive) {
                rawFreq *= 1.15f
            }

            // Calculate target volume based on throttle, speed, gear, and braking
            val baseVol = when {
                targetSpeedKmh < 3f && !isThrottleApplied -> 0.22f // Gentle idle throb
                isThrottleApplied -> 0.75f + (targetRpm / 14000f) * 0.25f // Full acceleration roar
                isBrakeApplied -> 0.35f // Engine deceleration brake drag
                else -> 0.45f + (targetSpeedKmh / 300f) * 0.30f // Coasting at speed
            }

            val targetVol = (baseVol * masterSfxVolume).coerceIn(0f, 1.0f)

            // Fill sample chunk
            for (i in 0 until chunkSamples) {
                // Smooth frequency and volume transitions (exponential moving average)
                currentFreq += (rawFreq - currentFreq) * 0.008f
                currentVolume += (targetVol - currentVolume) * 0.012f

                // Advance phase
                phase += (2.0 * Math.PI * currentFreq * dtSample)
                if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI

                subPhase += (Math.PI * currentFreq * dtSample)
                if (subPhase > 2.0 * Math.PI) subPhase -= 2.0 * Math.PI

                // 1. Multi-Harmonic Superbike Engine Synthesis:
                // H1 Fundamental sine
                val h1 = sin(phase)
                // H2 Second Harmonic (High-rev roar)
                val h2 = sin(phase * 2.0) * 0.60
                // H3 Third Harmonic (Exhaust rasp)
                val h3 = sin(phase * 3.0) * 0.40
                // H4 Fourth Harmonic (Superbike metallic bite)
                val h4 = sin(phase * 4.0) * 0.25
                // Combustion throb pulse (sub-harmonic)
                val subThrob = sin(subPhase) * 0.35

                // Combine harmonics
                var rawWave = (h1 * 0.45 + h2 * 0.28 + h3 * 0.18 + h4 * 0.12 + subThrob * 0.15)

                // 2. Non-linear Wave Distortion (adds muscular growl and exhaust rasp)
                rawWave = tanh(rawWave * 1.6)

                // 3. Rev-Limiter Stutter / Backfire Crackle at Redline
                if (isRedlineLimit) {
                    limiterTimer += dtSample * 22.0 // ~22 Hz bounce
                    val limiterGate = if (sin(limiterTimer * 2.0 * Math.PI) > 0.15) 1.0 else 0.08
                    rawWave *= limiterGate

                    // Intermittent ignition crackle noise
                    crackleNoisePhase += dtSample * 440.0
                    val crackle = (Math.random() - 0.5) * 0.35
                    rawWave += crackle
                }

                // 4. Nitro High-Speed Whine / Supercharger Turbine Resonance
                if (isNitroActive) {
                    val nitroFreq = 2400.0 + (targetSpeedKmh * 6.0)
                    nitroPhase += (2.0 * Math.PI * nitroFreq * dtSample)
                    if (nitroPhase > 2.0 * Math.PI) nitroPhase -= 2.0 * Math.PI
                    val turbineWhine = sin(nitroPhase) * 0.28
                    rawWave = (rawWave * 0.85) + turbineWhine
                }

                // 5. Realistic Helmet Wind Noise (Quadratically rushes with speed > 40 km/h)
                val speedRatio = (targetSpeedKmh / 220f).coerceIn(0f, 1.4f)
                val windTargetVol = if (targetSpeedKmh > 35f) (speedRatio * speedRatio * 0.42f) else 0f
                val whiteNoise = (Math.random() - 0.5).toFloat() * 2f
                // Low-pass/bandpass smoothing for realistic helmet wind roar
                windFilterState += (whiteNoise - windFilterState) * 0.18f
                val windAudio = windFilterState * windTargetVol

                // 6. Asphalt Tire Rolling Rumble Noise
                val tireRollingTargetVol = if (targetSpeedKmh > 8f) ((targetSpeedKmh / 180f).coerceIn(0f, 1f) * 0.16f) else 0f
                roadFilterState += (whiteNoise - roadFilterState) * 0.05f
                val tireAudio = roadFilterState * tireRollingTargetVol

                // 7. Hard Braking Tire Scrub / Friction Sound
                var brakeScrubAudio = 0f
                if (isBrakeApplied && targetSpeedKmh > 25f) {
                    val brakeIntensity = (targetSpeedKmh / 140f).coerceIn(0.2f, 1f)
                    brakeScrubPhase += (2.0 * Math.PI * 1850.0 * dtSample)
                    if (brakeScrubPhase > 2.0 * Math.PI) brakeScrubPhase -= 2.0 * Math.PI
                    brakeScrubFilter += (whiteNoise - brakeScrubFilter) * 0.45f
                    val scrubTonal = sin(brakeScrubPhase).toFloat() * 0.35f + brakeScrubFilter * 0.65f
                    brakeScrubAudio = scrubTonal * (0.24f * brakeIntensity)
                }

                // Combine engine synthesis with wind rushing, tire rumble, and brake scrub
                val compositeWave = (rawWave * currentVolume) + (windAudio + tireAudio + brakeScrubAudio) * masterSfxVolume

                // Apply master volume and scale to 16-bit PCM integer range
                val sampleValue = (compositeWave * 30000.0).coerceIn(-32767.0, 32767.0).toInt()
                shortBuffer[i] = sampleValue.toShort()
            }

            audioTrack?.write(shortBuffer, 0, chunkSamples)
        }
    }
}
