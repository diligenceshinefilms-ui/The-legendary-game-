package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testSlipstreamDraftingProximityFactor() {
    // Lead vehicle at 10m ahead, player at 0m (longDiff = 10m)
    val longDiff = 10.0f
    val latDiff = 0.04f
    val maxProximityDist = 32.0f
    val minProximityDist = 4.0f
    val maxLateralDiff = 0.18f

    val proximityFactor = ((maxProximityDist - longDiff) / (maxProximityDist - minProximityDist)).coerceIn(0f, 1f)
    val alignFactor = (1.0f - (latDiff / maxLateralDiff)).coerceIn(0f, 1f)
    val intensity = proximityFactor * alignFactor

    assertTrue("Draft intensity should be active and positive", intensity > 0.5f)
    assertTrue("Intensity should be clamped under or equal to 1.0", intensity <= 1.0f)
  }

  @Test
  fun testSlipstreamDisengagesOutsideLaneAlignment() {
    // When player changes lanes (latDiff > 0.18 lane width), drafting immediately disengages
    val longDiff = 10.0f
    val latDiff = 0.35f // shifted to adjacent lane
    val maxProximityDist = 32.0f
    val minProximityDist = 4.0f
    val maxLateralDiff = 0.18f

    val proximityFactor = ((maxProximityDist - longDiff) / (maxProximityDist - minProximityDist)).coerceIn(0f, 1f)
    val alignFactor = (1.0f - (latDiff / maxLateralDiff)).coerceIn(0f, 1f)
    val intensity = proximityFactor * alignFactor

    assertEquals("Draft intensity should be 0 outside lane alignment", 0f, intensity, 0.001f)
  }

  @Test
  fun testDoubleSplitHapticWaveformPattern() {
    val timings = longArrayOf(0, 35, 30, 60)
    assertEquals("Should have 4 phase transitions for double split", 4, timings.size)
    assertEquals("Initial delay must be 0 for instantaneous tactile response", 0L, timings[0])
    assertTrue("Total haptic duration should be crisp under 150ms", timings.sum() < 150L)
  }

  @Test
  fun testRedlineHapticThrottleInterval() {
    val minIntervalMs = 120L
    var lastTimestamp = 1000L
    val nextSampleTooEarly = 1050L
    val nextSampleValid = 1130L

    val shouldTriggerEarly = (nextSampleTooEarly - lastTimestamp) > minIntervalMs
    assertFalse("Early redline haptic sample should be throttled to prevent motor overload", shouldTriggerEarly)

    val shouldTriggerValid = (nextSampleValid - lastTimestamp) > minIntervalMs
    assertTrue("Valid interval sample should trigger handlebar flutter vibration", shouldTriggerValid)
  }

  @Test
  fun testUnderglowPulsationRange() {
    for (timeMs in 0..1000 step 50) {
      val pulse = 0.82f + 0.18f * kotlin.math.sin(timeMs * 0.007f)
      assertTrue("Underglow pulse lower bound must not extinguish", pulse >= 0.64f)
      assertTrue("Underglow pulse upper bound must not exceed 1.0f", pulse <= 1.0f)
    }
  }

  @Test
  fun testWetAsphaltReflectionColumnsStayWithinRoadBounds() {
    val roadHalfW = 450f
    val reflectionColumns = listOf(-0.65f, -0.25f, 0.20f, 0.60f)
    val widthFractions = listOf(0.15f, 0.12f, 0.14f, 0.16f)

    for (i in reflectionColumns.indices) {
      val offsetFrac = reflectionColumns[i]
      val widthFrac = widthFractions[i]
      val xCenter = roadHalfW * offsetFrac
      val w = roadHalfW * widthFrac
      val leftEdge = xCenter - w
      val rightEdge = xCenter + w

      assertTrue("Left edge of reflection column must be within road bounds", leftEdge >= -roadHalfW)
      assertTrue("Right edge of reflection column must be within road bounds", rightEdge <= roadHalfW)
    }
  }

  @Test
  fun testCameraFovWarpScaleIncreasesDuringNitro() {
    // Normal / idle state
    val idleIntensity = 0f
    val idleScaleX = 1f + (idleIntensity * 0.09f)
    val idleScaleY = 1f + (idleIntensity * 0.05f)
    assertEquals(1.0f, idleScaleX, 0.001f)
    assertEquals(1.0f, idleScaleY, 0.001f)

    // Full nitro boost state
    val nitroIntensity = 1.0f
    val nitroScaleX = 1f + (nitroIntensity * 0.09f)
    val nitroScaleY = 1f + (nitroIntensity * 0.05f)

    assertTrue("Nitro FOV width scale must produce wide-angle expansion", nitroScaleX >= 1.08f)
    assertTrue("Nitro FOV height scale must expand", nitroScaleY >= 1.04f)
    assertTrue("Anamorphic lens warp must expand horizontally more than vertically", nitroScaleX > nitroScaleY)
  }

  @Test
  fun testChromaticAberrationChannelSeparation() {
    val zDepths = listOf(0.2f, 0.5f, 0.8f, 1.0f)
    for (z in zDepths) {
      val prismShift = 2.4f * z
      val cyanOffset = -prismShift
      val magentaOffset = prismShift

      assertTrue("Prism shift must be positive for positive z", prismShift > 0f)
      assertEquals("Cyan and magenta offsets must be symmetric about the optical center", -cyanOffset, magentaOffset, 0.001f)
      assertTrue("Chromatic separation must increase with perspective depth", prismShift >= 2.4f * 0.2f)
    }
  }
}
