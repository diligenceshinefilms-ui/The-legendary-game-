package com.example.game.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

data class TiltSensorReading(
    val steerValue: Float = 0f,      // -1.0 (Full Left) to +1.0 (Full Right)
    val rollDegrees: Float = 0f,     // Current physical roll angle in degrees (-Left, +Right)
    val isAvailable: Boolean = false,
    val isCalibrated: Boolean = false
)

/**
 * Real-Time Motion Tilt Sensor Manager.
 * Tilting phone down on the right side -> steer right (+1.0)
 * Tilting phone down on the left side -> steer left (-1.0)
 */
class MotionTiltSensorManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    // Prioritize Game Rotation Vector / Rotation Vector for smooth gyroscope/accelerometer fusion
    private val sensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val isRotationVectorSensor = sensor?.type == Sensor.TYPE_GAME_ROTATION_VECTOR ||
            sensor?.type == Sensor.TYPE_ROTATION_VECTOR

    private val _tiltReading = MutableStateFlow(TiltSensorReading(isAvailable = sensor != null))
    val tiltReading: StateFlow<TiltSensorReading> = _tiltReading.asStateFlow()

    var sensitivity: Float = 1.4f
    var deadzone: Float = 0.02f
    var isInverted: Boolean = false
    private var calibrationOffset: Float = 0f
    private var smoothedSteer: Float = 0f

    // Highly responsive smoothing factor for real-time instantaneous feel with zero lag
    private val smoothingFactor = 0.75f

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    fun startListening() {
        sensor?.let {
            sensorManager?.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    fun calibrateCenter() {
        calibrationOffset = _tiltReading.value.rollDegrees
    }

    fun resetCalibration() {
        calibrationOffset = 0f
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        var rawRollDegrees = 0f

        if (isRotationVectorSensor) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            // In Android getOrientation:
            // orientationAngles[2] is roll. Clockwise rotation (right side down) is negative radians in world frame.
            // Negating it maps right side down to positive degrees (+), left side down to negative (-).
            val rollRad = -orientationAngles[2]
            rawRollDegrees = Math.toDegrees(rollRad.toDouble()).toFloat()
        } else {
            // Accelerometer or Gravity vector
            val rawX = event.values[0]
            val rawY = event.values[1]
            val rawZ = event.values[2]

            // In portrait mode:
            // When right edge dips down: gravity pulls towards -X axis in screen coords -> -rawX is positive (+).
            // When left edge dips down: gravity pulls towards +X axis -> -rawX is negative (-).
            val denominator = sqrt((rawY * rawY + rawZ * rawZ).toDouble()).coerceAtLeast(0.5)
            rawRollDegrees = (atan2(-rawX.toDouble(), denominator) * (180.0 / Math.PI)).toFloat()
        }

        // Apply calibration offset
        val adjustedRoll = rawRollDegrees - calibrationOffset

        // Comfort tilt range: ~18 degrees tilt = 100% steering lock
        val maxAngle = (18.0f / sensitivity.coerceIn(0.5f, 3.0f))
        var normalized = (adjustedRoll / maxAngle).coerceIn(-1.0f, 1.0f)

        if (isInverted) {
            normalized = -normalized
        }

        // Apply deadzone
        if (abs(normalized) < deadzone) {
            normalized = 0f
        } else {
            val sign = if (normalized > 0f) 1f else -1f
            normalized = sign * ((abs(normalized) - deadzone) / (1f - deadzone))
        }

        // Ultra fast responsive interpolation
        smoothedSteer += (normalized - smoothedSteer) * smoothingFactor

        _tiltReading.value = TiltSensorReading(
            steerValue = smoothedSteer.coerceIn(-1f, 1f),
            rollDegrees = adjustedRoll,
            isAvailable = true,
            isCalibrated = calibrationOffset != 0f
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
