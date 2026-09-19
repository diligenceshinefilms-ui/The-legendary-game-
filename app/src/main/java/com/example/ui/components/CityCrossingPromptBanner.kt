package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.environment.CityMilestone
import com.example.ui.theme.CarbonDark
import com.example.ui.theme.TextWhite

/**
 * Animated High-Tech City Crossing Milestone Prompt Banner.
 * Appears when crossing every 10KM milestone into a famous city!
 */
@Composable
fun CityCrossingPromptBanner(
    city: CityMilestone,
    milestoneKm: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "banner_pulse")
    val borderGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = city.primaryColor)
            .testTag("city_crossing_banner"),
        color = CarbonDark.copy(alpha = 0.94f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            city.primaryColor.copy(alpha = borderGlowAlpha)
        )
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            city.primaryColor.copy(alpha = 0.18f),
                            Color.Transparent,
                            city.secondaryColor.copy(alpha = 0.18f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Milestone Badge Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = null,
                    tint = city.secondaryColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "★ $milestoneKm KM MILESTONE UNLOCKED ★",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = city.secondaryColor,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // City Emoji & Name Headline
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = city.emoji,
                    fontSize = 28.sp
                )
                Column {
                    Text(
                        text = "WELCOME TO ${city.name}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = TextWhite,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = city.tagline,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = city.primaryColor,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Famous For Description & Landmark
            Text(
                text = "Famous for: ${city.famousFor} • ${city.landmarkName}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 9.5.sp
                )
            )
        }
    }
}
