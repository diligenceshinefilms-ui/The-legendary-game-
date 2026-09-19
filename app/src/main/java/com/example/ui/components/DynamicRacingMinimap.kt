package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.RacerState
import com.example.core.model.HighwayObstacle
import com.example.ui.theme.*

/**
 * Dynamic Racing Mini-Map HUD component:
 * - Real-time track layout curve (S-turns, highway straights)
 * - Dynamic player dot shifting laterally based on rider's steering choices (unconstrained left/right/center)
 * - Relative opponent bike markers ahead & behind player
 * - Traffic car / obstacle hazard radar dots
 * - Tactical cyber-grid background & compass heading indicator
 */
@Composable
fun DynamicRacingMinimap(
    playerRacer: RacerState?,
    racers: List<RacerState>,
    obstacles: List<HighwayObstacle> = emptyList(),
    roadCurvature: Float = 0f,
    modifier: Modifier = Modifier
) {
    val playerPos = playerRacer?.posX ?: 0f
    val playerDist = playerRacer?.totalRaceDistance ?: 0f

    Surface(
        color = GalaxyVoid.copy(alpha = 0.92f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, GalaxyPink.copy(alpha = 0.85f)),
        modifier = modifier
            .testTag("dynamic_racing_minimap")
            .size(width = 110.dp, height = 100.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
            ) {
                val w = size.width
                val h = size.height
                val centerX = w / 2f
                val playerY = h * 0.75f // Player anchored near lower 3/4 of radar canvas

                // 1. Grid Radar Lines & Crosshair Background
                val gridColor = Color(0xFF2D1B4E).copy(alpha = 0.6f)
                drawLine(gridColor, Offset(centerX, 0f), Offset(centerX, h), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, playerY), Offset(w, playerY), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, h * 0.35f), Offset(w, h * 0.35f), strokeWidth = 1f)

                // Concentric Radar Rings
                drawCircle(color = gridColor, radius = h * 0.35f, center = Offset(centerX, playerY), style = Stroke(1f))

                // 2. Dynamic Track Layout Curve
                // Path maps road curvature projecting ahead up to +300m
                val trackPath = Path().apply {
                    moveTo(centerX, h)
                    val curveOffset = roadCurvature * (w * 0.30f)
                    cubicTo(
                        centerX + curveOffset * 0.2f, h * 0.7f,
                        centerX + curveOffset * 0.8f, h * 0.35f,
                        centerX + curveOffset, h * 0.05f
                    )
                }

                // Outer Road Boundary / Shoulder Guidelines
                drawPath(
                    path = trackPath,
                    color = Color(0xFF3B1D64),
                    style = Stroke(width = 14f, cap = StrokeCap.Round)
                )
                // Main Track Route Line
                drawPath(
                    path = trackPath,
                    color = GalaxyCyan.copy(alpha = 0.85f),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )

                // 3. Traffic / Obstacle Hazard Dots ahead on track (-30m to +250m)
                obstacles.forEach { obs ->
                    val distDiff = obs.distanceMeters - playerDist
                    if (distDiff in -30f..250f) {
                        val relY = playerY - (distDiff / 250f) * (playerY - 10f)
                        val roadX = centerX + (roadCurvature * (w * 0.30f) * (1f - (relY / h)))
                        val laneX = roadX + (obs.lane * (w * 0.28f))

                        if (relY in 0f..h) {
                            drawCircle(color = GalaxyOrange, radius = 3f, center = Offset(laneX, relY))
                        }
                    }
                }

                // 4. Opponent Bike Dots (Positioned relative to player distance and lateral lane posX)
                racers.forEach { racer ->
                    if (!racer.isPlayer) {
                        val distDiff = racer.totalRaceDistance - playerDist
                        if (distDiff in -100f..280f) {
                            val relY = playerY - (distDiff / 280f) * (playerY - 10f)
                            val roadX = centerX + (roadCurvature * (w * 0.30f) * (1f - (relY / h)))
                            val racerX = roadX + (racer.posX * (w * 0.28f))

                            if (relY in 0f..h) {
                                val oppColor = Color(racer.primaryColorHex)
                                // Opponent dot outer glow
                                drawCircle(color = oppColor.copy(alpha = 0.4f), radius = 6f, center = Offset(racerX, relY))
                                // Core dot
                                drawCircle(color = oppColor, radius = 3.5f, center = Offset(racerX, relY))
                                drawCircle(color = Color.White, radius = 1.8f, center = Offset(racerX, relY))
                            }
                        }
                    }
                }

                // 5. Player Dot (Dynamic lateral offset based on rider's steering choice)
                val playerRoadX = centerX + (playerPos * (w * 0.28f))

                // Pulsing Radar Sweep Ring around player
                val sweepPulse = ((System.currentTimeMillis() % 1200) / 1200f) * 12f + 5f
                drawCircle(
                    color = GalaxyPink.copy(alpha = 0.45f),
                    radius = sweepPulse,
                    center = Offset(playerRoadX, playerY)
                )

                // Player Directional Arrow / Dot
                val arrowPath = Path().apply {
                    moveTo(playerRoadX, playerY - 6f)
                    lineTo(playerRoadX - 4.5f, playerY + 5f)
                    lineTo(playerRoadX, playerY + 3f)
                    lineTo(playerRoadX + 4.5f, playerY + 5f)
                    close()
                }
                drawPath(path = arrowPath, color = GalaxyCyan)
                drawPath(path = arrowPath, color = Color.White, style = Stroke(1.2f))
            }

            // Top Status Badge: Compass & Radar Label
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 4.dp, start = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GALAXY RADAR",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 7.5.sp,
                        color = GalaxyPink,
                        letterSpacing = 0.8.sp
                    )
                )
            }

            // Bottom Status Badge: Player Position Indicator
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 3.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sideLabel = when {
                    playerPos < -0.3f -> "LEFT"
                    playerPos > 0.3f -> "RIGHT"
                    else -> "MID"
                }
                Text(
                    text = sideLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 7.5.sp,
                        color = TextLavender
                    )
                )
            }
        }
    }
}
