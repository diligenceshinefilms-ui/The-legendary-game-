package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlin.math.*

enum class StudioViewMode {
    PHOTOREALISTIC_8K_RENDER,
    INTERACTIVE_3D_VECTOR
}

enum class StudioLightingScheme(val label: String, val primaryGlow: Color, val rimAccent: Color) {
    CYAN_RED_STUDIO("Moody Studio (Cyan / Red)", Color(0xFF00F0FF), Color(0xFFFF1744)),
    NEON_GOLD("Turbina Gold & Cyan", Color(0xFFFFD600), Color(0xFF00F0FF)),
    MIDNIGHT_NOIR("Midnight Storm", Color(0xFF38BDF8), Color(0xFF818CF8)),
    SOLAR_PLASMA("Plasma Blaze", Color(0xFFFF1B7A), Color(0xFFFF6B00))
}

@Composable
fun TurbinaBikeStudioView(
    modifier: Modifier = Modifier,
    onEquipTurbina: (() -> Unit)? = null,
    isEquipped: Boolean = false
) {
    var viewMode by remember { mutableStateOf(StudioViewMode.PHOTOREALISTIC_8K_RENDER) }
    var lightingScheme by remember { mutableStateOf(StudioLightingScheme.CYAN_RED_STUDIO) }
    var isRevving by remember { mutableStateOf(false) }
    var lightsOn by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "studio_anim")
    val studioAtmospherePulse by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "atmosphere_pulse"
    )

    val wheelRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isRevving) 400 else 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wheel_spin"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Brush.horizontalGradient(listOf(Color(0xFF0B797D), Color(0xFFFFD600), Color(0xFFFF1744))), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = CarbonCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Title & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0B797D))
                            .border(1.dp, Color(0xFFFFD600), CircleShape)
                    )
                    Column {
                        Text(
                            text = "TURBINA HYPER-ELECTRIC EX-1",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                color = TextWhite
                            )
                        )
                        Text(
                            text = "8K CGI Studio Master Render • Deep Metallic Teal (#0B797D)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF00F0FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // View Mode Toggle Button
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                    modifier = Modifier.clickable {
                        viewMode = if (viewMode == StudioViewMode.PHOTOREALISTIC_8K_RENDER) {
                            StudioViewMode.INTERACTIVE_3D_VECTOR
                        } else {
                            StudioViewMode.PHOTOREALISTIC_8K_RENDER
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (viewMode == StudioViewMode.PHOTOREALISTIC_8K_RENDER) Icons.Default.ViewInAr else Icons.Default.Photo,
                            contentDescription = null,
                            tint = Color(0xFFFFD600),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (viewMode == StudioViewMode.PHOTOREALISTIC_8K_RENDER) "3D VECTOR" else "8K RENDER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD600)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Display Canvas / Image Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF080B10))
                    .border(1.dp, CarbonCardBorder, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (viewMode == StudioViewMode.PHOTOREALISTIC_8K_RENDER) {
                    // 8K Photorealistic CGI Render Asset
                    Image(
                        painter = painterResource(id = R.drawable.img_turbina_studio),
                        contentDescription = "TURBINA Electric Racing Motorcycle and Rider",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Atmospheric Gradient Rim Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        lightingScheme.primaryGlow.copy(alpha = 0.18f * studioAtmospherePulse),
                                        Color.Transparent,
                                        lightingScheme.rimAccent.copy(alpha = 0.18f * studioAtmospherePulse)
                                    )
                                )
                            )
                    )

                    // Wet floor reflection sheen line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF00F0FF).copy(alpha = 0.6f),
                                        Color(0xFFFFD600).copy(alpha = 0.8f),
                                        Color(0xFFFF1744).copy(alpha = 0.6f)
                                    )
                                )
                            )
                    )
                } else {
                    // Interactive 2D/3D Vector Side Profile Studio Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawTurbinaStudioScene(
                            wheelAngleDeg = wheelRotation,
                            isRevving = isRevving,
                            lightsOn = lightsOn,
                            lightingScheme = lightingScheme,
                            atmospherePulse = studioAtmospherePulse
                        )
                    }
                }

                // Watermark & Specs Overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD600).copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "GOLD 'TURBINA' DECALS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFFFD600),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "P ZERO TIRES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF00F0FF),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Bottom Left Spec Tag
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "3-LOOP TREFOIL RIMS • MONOCOQUE TEAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextWhite,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Studio Specifications Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TurbinaSpecChip(
                    title = "AERODYNAMICS",
                    value = "Monocoque Teal",
                    accent = Color(0xFF0B797D),
                    modifier = Modifier.weight(1f)
                )
                TurbinaSpecChip(
                    title = "WHEEL RIMS",
                    value = "3-Loop Turbina",
                    accent = Color(0xFFFFD600),
                    modifier = Modifier.weight(1f)
                )
                TurbinaSpecChip(
                    title = "FRONT LIGHTING",
                    value = "Cyan LED Bar",
                    accent = Color(0xFF00F0FF),
                    modifier = Modifier.weight(1f)
                )
                TurbinaSpecChip(
                    title = "TAIL LIGHT",
                    value = "Red LED Strip",
                    accent = Color(0xFFFF1744),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Studio Interactive Controls (Rev, Lighting, Equip)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rev Electric Turbine Button
                Button(
                    onClick = { isRevving = !isRevving },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRevving) Color(0xFFFF6B00) else CarbonSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isRevving) Color(0xFFFF6B00) else CarbonCardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (isRevving) Color.White else Color(0xFFFF6B00),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRevving) "SPOOLING..." else "REV TURBINE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Toggle Lighting Scheme Button
                IconButton(
                    onClick = {
                        val schemes = StudioLightingScheme.values()
                        val nextIdx = (lightingScheme.ordinal + 1) % schemes.size
                        lightingScheme = schemes[nextIdx]
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CarbonSurface)
                        .border(1.dp, CarbonCardBorder, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.LightMode,
                        contentDescription = "Studio Lighting",
                        tint = lightingScheme.primaryGlow
                    )
                }

                // Equip Turbina Button
                if (onEquipTurbina != null) {
                    Button(
                        onClick = onEquipTurbina,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEquipped) EmeraldGreen else Color(0xFF0B797D)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD600)),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(
                            imageVector = if (isEquipped) Icons.Default.Check else Icons.Default.TwoWheeler,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEquipped) "EQUIPPED" else "EQUIP TURBINA",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TurbinaSpecChip(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = CarbonSurface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextGray,
                    fontSize = 8.sp
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * Procedural Vector Draw Logic for Turbina Hyper-Electric EX-1 Side Profile
 */
private fun DrawScope.drawTurbinaStudioScene(
    wheelAngleDeg: Float,
    isRevving: Boolean,
    lightsOn: Boolean,
    lightingScheme: StudioLightingScheme,
    atmospherePulse: Float
) {
    val cx = size.width / 2f
    val cy = size.height * 0.52f
    val s = min(size.width / 420f, size.height / 240f)

    // 1. Studio Background Gradient & Dramatic Moody Rim Lighting
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF141C24),
                Color(0xFF090D12),
                Color(0xFF040608)
            ),
            center = Offset(cx, cy - 20f * s),
            radius = size.width * 0.8f
        )
    )

    // Atmospheric Smoke / Mist Rim Glow
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                lightingScheme.primaryGlow.copy(alpha = 0.15f * atmospherePulse),
                Color.Transparent
            ),
            center = Offset(cx - 100f * s, cy - 40f * s),
            radius = 160f * s
        ),
        topLeft = Offset(cx - 220f * s, cy - 140f * s),
        size = Size(240f * s, 200f * s)
    )

    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                lightingScheme.rimAccent.copy(alpha = 0.15f * atmospherePulse),
                Color.Transparent
            ),
            center = Offset(cx + 120f * s, cy - 20f * s),
            radius = 140f * s
        ),
        topLeft = Offset(cx + 20f * s, cy - 120f * s),
        size = Size(200f * s, 180f * s)
    )

    // 2. Wet Reflective Ground Floor
    val floorY = cy + 62f * s
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0E1720),
                Color(0xFF06090D),
                Color.Black
            ),
            startY = floorY,
            endY = size.height
        ),
        topLeft = Offset(0f, floorY),
        size = Size(size.width, size.height - floorY)
    )

    // Wet floor specular puddles with cyan/red reflection
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF0B797D).copy(alpha = 0.35f),
                lightingScheme.primaryGlow.copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = Offset(cx, floorY + 10f * s),
            radius = 160f * s
        ),
        topLeft = Offset(cx - 180f * s, floorY),
        size = Size(360f * s, 40f * s)
    )

    // Wheel positions in side profile
    val frontWheelCenter = Offset(cx - 95f * s, cy + 24f * s)
    val rearWheelCenter = Offset(cx + 95f * s, cy + 24f * s)
    val wheelRadius = 38f * s

    // 3. FRONT WHEEL (3-Loop Turbina Pattern + P ZERO Tires + Gold Rim Stripe)
    drawTurbinaWheel(
        center = frontWheelCenter,
        radius = wheelRadius,
        angleDeg = wheelAngleDeg,
        scale = s
    )

    // 4. REAR WHEEL (3-Loop Turbina Pattern + P ZERO Tires + Gold Rim Stripe)
    drawTurbinaWheel(
        center = rearWheelCenter,
        radius = wheelRadius,
        angleDeg = wheelAngleDeg,
        scale = s
    )

    // 5. UNDERGLOW CYAN LIGHTING ON WET FLOOR
    if (lightsOn) {
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF00F0FF).copy(alpha = 0.6f * atmospherePulse),
                    Color(0xFF0B797D).copy(alpha = 0.25f * atmospherePulse),
                    Color.Transparent
                ),
                center = Offset(cx, cy + 45f * s),
                radius = 100f * s
            ),
            topLeft = Offset(cx - 110f * s, cy + 30f * s),
            size = Size(220f * s, 30f * s)
        )
    }

    // 6. MAIN MONOCOQUE CHASSIS & AERODYNAMIC FAIRINGS (#0B797D Deep Metallic Teal)
    val deepMetallicTeal = Color(0xFF0B797D)
    val darkCarbonFiber = Color(0xFF141920)
    val goldDecalColor = Color(0xFFFFD600)
    val cyanLedColor = Color(0xFF00F0FF)
    val redLedColor = Color(0xFFFF1744)

    // Lower belly pan / battery casing
    val bellyPanPath = Path().apply {
        moveTo(frontWheelCenter.x + 12f * s, cy + 35f * s)
        lineTo(rearWheelCenter.x - 10f * s, cy + 36f * s)
        lineTo(rearWheelCenter.x - 20f * s, cy + 18f * s)
        lineTo(cx, cy + 12f * s)
        lineTo(frontWheelCenter.x + 20f * s, cy + 10f * s)
        close()
    }
    drawPath(bellyPanPath, Brush.verticalGradient(listOf(deepMetallicTeal, darkCarbonFiber)))

    // Main monocoque body swooping from front nose cowl to rear tapered tail
    val monocoqueBodyPath = Path().apply {
        moveTo(frontWheelCenter.x - 24f * s, cy - 14f * s) // Front nose tip
        cubicTo(
            frontWheelCenter.x - 10f * s, cy - 35f * s,
            cx - 30f * s, cy - 38f * s,
            cx, cy - 25f * s // Center tank saddle
        )
        cubicTo(
            cx + 25f * s, cy - 20f * s,
            cx + 55f * s, cy - 28f * s,
            rearWheelCenter.x + 20f * s, cy - 14f * s // Tapered rear aerodynamic tail
        )
        lineTo(rearWheelCenter.x + 24f * s, cy - 8f * s) // Tail underside
        lineTo(rearWheelCenter.x - 10f * s, cy + 2f * s) // Above rear tire
        lineTo(cx + 20f * s, cy + 18f * s) // Under seat channel
        lineTo(frontWheelCenter.x + 10f * s, cy + 15f * s) // Fairing lower boundary
        lineTo(frontWheelCenter.x - 18f * s, cy - 4f * s) // Front lower cowl
        close()
    }

    // Gradient metallic teal shading with dynamic highlight
    drawPath(
        path = monocoqueBodyPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF14A3A8), // Specular metallic teal highlight
                deepMetallicTeal,
                Color(0xFF075457), // Deep shadow teal
                deepMetallicTeal
            ),
            start = Offset(cx - 80f * s, cy - 40f * s),
            end = Offset(cx + 80f * s, cy + 20f * s)
        )
    )

    // Dark Carbon Fiber Recessed Intake Vent on side fairing
    val carbonIntakePath = Path().apply {
        moveTo(cx - 25f * s, cy - 10f * s)
        lineTo(cx + 20f * s, cy - 6f * s)
        lineTo(cx + 12f * s, cy + 6f * s)
        lineTo(cx - 20f * s, cy + 2f * s)
        close()
    }
    drawPath(carbonIntakePath, darkCarbonFiber)
    drawPath(carbonIntakePath, Color.Black.copy(alpha = 0.5f), style = Stroke(width = 1.5f * s))

    // Gold "TURBINA" typographic decal along the side fairing recess
    val decalCenter = Offset(cx - 2f * s, cy - 3f * s)
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                goldDecalColor.copy(alpha = 0.9f),
                goldDecalColor,
                Color.Transparent
            )
        ),
        topLeft = Offset(decalCenter.x - 22f * s, decalCenter.y - 3.5f * s),
        size = Size(44f * s, 7f * s)
    )

    // Front Shroud & Recessed Handlebar Grip
    val frontForkCover = Path().apply {
        moveTo(frontWheelCenter.x - 8f * s, cy - 22f * s)
        lineTo(frontWheelCenter.x + 8f * s, cy - 18f * s)
        lineTo(frontWheelCenter.x + 4f * s, frontWheelCenter.y)
        lineTo(frontWheelCenter.x - 4f * s, frontWheelCenter.y)
        close()
    }
    drawPath(frontForkCover, Brush.verticalGradient(listOf(deepMetallicTeal, darkCarbonFiber)))

    // 7. LIGHTING: Front Cyan LED Light Bar & Rear Red LED Tail Strip
    if (lightsOn) {
        // Front Horizontal Cyan LED Light Bar
        val frontLightNose = Offset(frontWheelCenter.x - 24f * s, cy - 14f * s)
        drawLine(
            color = cyanLedColor,
            start = Offset(frontLightNose.x - 4f * s, frontLightNose.y - 2f * s),
            end = Offset(frontLightNose.x + 10f * s, frontLightNose.y + 4f * s),
            strokeWidth = 3.5f * s,
            cap = StrokeCap.Round
        )

        // Forward luminous cyan headlight beam cast
        drawPath(
            path = Path().apply {
                moveTo(frontLightNose.x, frontLightNose.y)
                lineTo(0f, frontLightNose.y - 40f * s)
                lineTo(0f, floorY)
                lineTo(frontLightNose.x - 40f * s, floorY)
                close()
            },
            brush = Brush.linearGradient(
                colors = listOf(
                    cyanLedColor.copy(alpha = 0.35f * atmospherePulse),
                    Color.Transparent
                ),
                start = frontLightNose,
                end = Offset(0f, cy)
            )
        )

        // Rear Horizontal Red LED Tail Strip
        val rearTailTip = Offset(rearWheelCenter.x + 22f * s, cy - 12f * s)
        drawLine(
            color = redLedColor,
            start = Offset(rearTailTip.x - 8f * s, rearTailTip.y - 1f * s),
            end = Offset(rearTailTip.x + 3f * s, rearTailTip.y + 2f * s),
            strokeWidth = 3f * s,
            cap = StrokeCap.Round
        )

        // Rear red ambient tail glow
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    redLedColor.copy(alpha = 0.4f * atmospherePulse),
                    Color.Transparent
                ),
                center = rearTailTip,
                radius = 35f * s
            ),
            topLeft = Offset(rearTailTip.x - 20f * s, rearTailTip.y - 20f * s),
            size = Size(40f * s, 40f * s)
        )
    }

    // 8. RIDER IN AGGRESSIVE FORWARD RACING TUCK
    drawTurbinaRider(
        center = Offset(cx, cy),
        scale = s,
        atmospherePulse = atmospherePulse
    )
}

/**
 * Draws the 3-Loop "Turbina" Trefoil pattern wheel rim + P ZERO tire + Gold rim stripe
 */
private fun DrawScope.drawTurbinaWheel(
    center: Offset,
    radius: Float,
    angleDeg: Float,
    scale: Float
) {
    // Outer Tire (P ZERO low profile black rubber)
    drawCircle(
        color = Color(0xFF101418),
        radius = radius,
        center = center
    )

    // Tire tread edge
    drawCircle(
        color = Color(0xFF222830),
        radius = radius,
        center = center,
        style = Stroke(width = 3f * scale)
    )

    // Thin Gold Rim Pinstripe (#FFD600)
    drawCircle(
        color = Color(0xFFFFD600),
        radius = radius - 5f * scale,
        center = center,
        style = Stroke(width = 1.6f * scale)
    )

    // Inner Rim Bed / Ventilated Brake Disc
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF2A313C), Color(0xFF161A22)),
            center = center,
            radius = radius - 6f * scale
        ),
        radius = radius - 6f * scale,
        center = center
    )

    // Cross-drilled brake rotor vents
    for (i in 0 until 12) {
        val ventAngle = Math.toRadians((i * 30).toDouble())
        val vr = radius * 0.58f
        drawCircle(
            color = Color(0xFF0F1318),
            radius = 1.2f * scale,
            center = Offset(center.x + vr * cos(ventAngle).toFloat(), center.y + vr * sin(ventAngle).toFloat())
        )
    }

    // 3-Loop "Turbina" Trefoil Rim Spoke Pattern (Rotated dynamically with wheel spin)
    withTransform({
        rotate(degrees = angleDeg, pivot = center)
    }) {
        val loopRadius = radius * 0.44f
        val trefoilDarkTeal = Color(0xFF075457)
        val trefoilTeal = Color(0xFF0B797D)

        for (i in 0..2) {
            val spokeAngle = Math.toRadians((i * 120).toDouble())
            val loopCenter = Offset(
                center.x + (radius * 0.32f) * cos(spokeAngle).toFloat(),
                center.y + (radius * 0.32f) * sin(spokeAngle).toFloat()
            )

            // Loop petal outline
            drawCircle(
                color = trefoilTeal,
                radius = loopRadius,
                center = loopCenter,
                style = Stroke(width = 3.2f * scale)
            )
            drawCircle(
                color = trefoilDarkTeal,
                radius = loopRadius - 2f * scale,
                center = loopCenter,
                style = Stroke(width = 1.2f * scale)
            )
        }

        // Center Hub Cap & Gold Turbina Axis Cap
        drawCircle(
            color = Color(0xFF0B797D),
            radius = 8f * scale,
            center = center
        )
        drawCircle(
            color = Color(0xFFFFD600),
            radius = 3.5f * scale,
            center = center
        )
    }

    // Brake Caliper with Gold 'TURBINA' emblem
    val caliperOffset = Offset(center.x, center.y + radius * 0.55f)
    drawRoundRect(
        color = Color(0xFF075457),
        topLeft = Offset(caliperOffset.x - 14f * scale, caliperOffset.y - 6f * scale),
        size = Size(28f * scale, 12f * scale),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * scale, 3f * scale)
    )
    drawRect(
        color = Color(0xFFFFD600),
        topLeft = Offset(caliperOffset.x - 9f * scale, caliperOffset.y - 1f * scale),
        size = Size(18f * scale, 2.5f * scale)
    )
}

/**
 * Draws the rider in aggressive forward racing tuck wearing color-matched cyan-teal suit & helmet
 */
private fun DrawScope.drawTurbinaRider(
    center: Offset,
    scale: Float,
    atmospherePulse: Float
) {
    val suitTeal = Color(0xFF0B797D)
    val suitTealHighlight = Color(0xFF14A3A8)
    val darkUndersuit = Color(0xFF161C24)
    val helmetVisor = Color(0xFF0D1117)

    val hipX = center.x + 38f * scale
    val hipY = center.y - 20f * scale
    val shoulderX = center.x - 22f * scale
    val shoulderY = center.y - 36f * scale
    val headX = center.x - 42f * scale
    val headY = center.y - 48f * scale

    // 1. Lower Body: Legs in forward bent tuck on footpegs
    val legPath = Path().apply {
        moveTo(hipX, hipY)
        lineTo(hipX + 16f * scale, hipY + 18f * scale) // Knee forward
        lineTo(hipX + 8f * scale, hipY + 36f * scale) // Ankle at footpeg
        lineTo(hipX - 6f * scale, hipY + 36f * scale) // Boot toe
        lineTo(hipX + 2f * scale, hipY + 30f * scale)
        lineTo(hipX + 6f * scale, hipY + 16f * scale)
        close()
    }
    drawPath(legPath, darkUndersuit)

    // Teal Boot Accent & Slider
    drawRoundRect(
        color = suitTeal,
        topLeft = Offset(hipX - 4f * scale, hipY + 32f * scale),
        size = Size(14f * scale, 6f * scale),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * scale, 2f * scale)
    )

    // 2. Torso in aggressive forward racing tuck
    val torsoPath = Path().apply {
        moveTo(hipX, hipY)
        lineTo(shoulderX, shoulderY)
        lineTo(shoulderX - 6f * scale, shoulderY + 14f * scale)
        lineTo(hipX - 10f * scale, hipY + 4f * scale)
        close()
    }
    drawPath(
        path = torsoPath,
        brush = Brush.linearGradient(
            colors = listOf(suitTealHighlight, suitTeal, Color(0xFF075457)),
            start = Offset(shoulderX, shoulderY),
            end = Offset(hipX, hipY)
        )
    )

    // Aerodynamic shoulder cap
    drawCircle(
        color = suitTealHighlight,
        radius = 8f * scale,
        center = Offset(shoulderX, shoulderY)
    )

    // 3. Arm reaching forward to low handlebar clip-on
    val armPath = Path().apply {
        moveTo(shoulderX, shoulderY)
        lineTo(shoulderX - 16f * scale, shoulderY + 12f * scale) // Elbow
        lineTo(center.x - 48f * scale, center.y - 18f * scale) // Hand gripping bar
        lineTo(center.x - 44f * scale, center.y - 14f * scale)
        lineTo(shoulderX - 12f * scale, shoulderY + 14f * scale)
        close()
    }
    drawPath(
        path = armPath,
        brush = Brush.linearGradient(
            colors = listOf(suitTeal, darkUndersuit),
            start = Offset(shoulderX, shoulderY),
            end = Offset(center.x - 48f * scale, center.y - 18f * scale)
        )
    )

    // Glove
    drawCircle(
        color = Color(0xFF101418),
        radius = 4f * scale,
        center = Offset(center.x - 48f * scale, center.y - 18f * scale)
    )

    // 4. Color-matched Full-Face Aerodynamic Helmet
    val helmetCenter = Offset(headX, headY)
    val helmetRadius = 14f * scale

    // Helmet Shell (Teal metallic finish)
    drawOval(
        brush = Brush.linearGradient(
            colors = listOf(suitTealHighlight, suitTeal, Color(0xFF075457)),
            start = Offset(headX - helmetRadius, headY - helmetRadius),
            end = Offset(headX + helmetRadius, headY + helmetRadius)
        ),
        topLeft = Offset(headX - helmetRadius * 1.15f, headY - helmetRadius * 0.9f),
        size = Size(helmetRadius * 2.3f, helmetRadius * 1.8f)
    )

    // Dark Smoked Panoramic Visor
    val visorPath = Path().apply {
        moveTo(headX - helmetRadius * 1.1f, headY - 2f * scale)
        cubicTo(
            headX - helmetRadius * 0.5f, headY - helmetRadius * 0.7f,
            headX + 2f * scale, headY - helmetRadius * 0.5f,
            headX + 6f * scale, headY + 3f * scale
        )
        lineTo(headX - helmetRadius * 0.8f, headY + 6f * scale)
        close()
    }
    drawPath(visorPath, helmetVisor)

    // Specular Visor Reflection Streak (Cyan studio rim light)
    drawLine(
        color = Color(0xFF00F0FF).copy(alpha = 0.8f * atmospherePulse),
        start = Offset(headX - helmetRadius * 0.9f, headY - 1f * scale),
        end = Offset(headX - helmetRadius * 0.2f, headY - 5f * scale),
        strokeWidth = 1.8f * scale,
        cap = StrokeCap.Round
    )
}
