package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Player
import com.example.ui.theme.*

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = GalaxyCyan,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    testTag: String = "neon_button"
) {
    val bgBrush = if (isPrimary && enabled) {
        if (color == GalaxyCyan || color == NeonCyan || color == GalaxyPink) {
            GalaxyPrimaryGradient
        } else {
            Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.8f)))
        }
    } else {
        Brush.horizontalGradient(listOf(GalaxySurface, GalaxyCard))
    }

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp))
            .background(bgBrush)
            .border(
                width = if (isPrimary) 1.5.dp else 1.dp,
                color = if (isPrimary && enabled) GalaxyPink.copy(alpha = 0.6f) else if (enabled) color else TextMuted,
                shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isPrimary) Color.White else color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 1.2.sp,
                    color = if (isPrimary) Color.White else if (enabled) TextWhite else TextMuted
                )
            )
        }
    }
}

@Composable
fun StatBar(
    label: String,
    value: Int, // 0..100
    modifier: Modifier = Modifier,
    previewBonus: Int = 0,
    color: Color = NeonCyan,
    icon: ImageVector? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                )
                if (previewBonus > 0) {
                    Text(
                        text = " (+$previewBonus)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(CarbonDark)
                .border(0.5.dp, CarbonCardBorder, RoundedCornerShape(4.dp))
        ) {
            // Base Value Fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (value / 100f).coerceIn(0f, 1f))
                    .background(
                        Brush.horizontalGradient(
                            listOf(color.copy(alpha = 0.7f), color)
                        )
                    )
            )
            // Bonus Preview Fill
            if (previewBonus > 0) {
                val totalFraction = ((value + previewBonus) / 100f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = totalFraction)
                        .background(EmeraldGreen.copy(alpha = 0.4f))
                )
            }
        }
    }
}

@Composable
fun CurrencyHeader(
    player: Player?,
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = CarbonCard.copy(alpha = 0.9f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player Profile Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onProfileClick)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CutCornerShape(8.dp))
                        .background(NeonCyan)
                        .border(1.dp, NeonOrange, CutCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "L${player?.level ?: 1}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = CarbonDark
                        )
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = player?.displayName ?: "Racer Apex",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    // Mini XP bar
                    val xpProgress = (player?.xp?.toFloat() ?: 0f) / ((player?.xpToNextLevel ?: 1000L).toFloat())
                    LinearProgressIndicator(
                        progress = { xpProgress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .width(80.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = NeonCyan,
                        trackColor = CarbonDark
                    )
                }
            }

            // Coins Balance Badge
            Surface(
                color = CarbonDark,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricYellow.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Coins",
                        tint = ElectricYellow,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "%,d".format(player?.coins ?: 1000L),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = ElectricYellow
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun PositionBadge(
    position: Int,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (position) {
        1 -> Triple(ElectricYellow, CarbonDark, "1ST")
        2 -> Triple(Color(0xFFE2E8F0), CarbonDark, "2ND")
        3 -> Triple(NeonOrange, CarbonDark, "3RD")
        else -> Triple(CarbonSurface, TextWhite, "${position}TH")
    }

    Box(
        modifier = modifier
            .clip(CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
            .background(bgColor)
            .border(1.dp, Color.White.copy(alpha = 0.4f), CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                color = textColor
            )
        )
    }
}
