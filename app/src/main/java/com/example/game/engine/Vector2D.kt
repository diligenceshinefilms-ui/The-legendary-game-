package com.example.game.engine

import kotlin.math.*

data class Vector2D(val x: Float = 0f, val y: Float = 0f) {
    operator fun plus(other: Vector2D) = Vector2D(x + other.x, y + other.y)
    operator fun minus(other: Vector2D) = Vector2D(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vector2D(x * scalar, y * scalar)
    operator fun div(scalar: Float) = Vector2D(x / scalar, y / scalar)

    fun length(): Float = sqrt(x * x + y * y)
    fun lengthSquared(): Float = x * x + y * y

    fun normalized(): Vector2D {
        val len = length()
        return if (len > 0.0001f) Vector2D(x / len, y / len) else Vector2D(0f, 0f)
    }

    fun distanceTo(other: Vector2D): Float = (this - other).length()

    fun dot(other: Vector2D): Float = x * other.x + y * other.y

    fun angle(): Float = atan2(y, x)

    fun rotated(angleRad: Float): Vector2D {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        return Vector2D(x * cosA - y * sinA, x * sinA + y * cosA)
    }

    companion object {
        fun fromAngle(angleRad: Float, length: Float = 1f): Vector2D {
            return Vector2D(cos(angleRad) * length, sin(angleRad) * length)
        }

        fun lerp(a: Vector2D, b: Vector2D, t: Float): Vector2D {
            val clampedT = t.coerceIn(0f, 1f)
            return Vector2D(a.x + (b.x - a.x) * clampedT, a.y + (b.y - a.y) * clampedT)
        }
    }
}
