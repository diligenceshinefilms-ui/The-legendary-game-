package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// GALAXY BIKE & COSMIC NEBULA THEME PALETTE
// Inspired by Galaxy Bike & High-Contrast Ride UI
// ==========================================

// Cosmic Core Gradients & Accents
val GalaxyPink = Color(0xFFFF1B7A)           // Hot Neon Magenta/Pink (Active indicators, CTA)
val GalaxyMagenta = Color(0xFFD946EF)        // Vibrant Nebula Magenta
val GalaxyViolet = Color(0xFF8B5CF6)         // Cosmic Purple/Violet
val GalaxyDeepPurple = Color(0xFF6D28D9)     // Deep Nebula Purple
val GalaxyCyan = Color(0xFF00F5FF)           // Electric Cyan (Telemetry & navigation)
val GalaxyCyanVariant = Color(0xFF06B6D4)    // Electric Aqua
val GalaxyOrange = Color(0xFFFF6A00)         // Warm Nebula Orange (Button gradient start)
val GalaxyGold = Color(0xFFFFB703)           // Galaxy Starlight Gold
val GalaxyEmerald = Color(0xFF10B981)        // Live trip / Emerald Green
val GalaxyRed = Color(0xFFFF2A6D)            // Superbike Redline / Alert

// Deep Cosmic Canvas & Glassmorphic Surfaces
val GalaxyVoid = Color(0xFF080413)           // Deepest Cosmic Space Background
val GalaxyCanvas = Color(0xFF0E0822)         // Primary Galaxy Screen Canvas
val GalaxyCard = Color(0xFF17102D)           // Translucent Dark Galaxy Surface
val GalaxyCardBorder = Color(0xFF331F58)     // Luminous Purple/Violet Outline
val GalaxySurface = Color(0xFF1F143B)         // Elevated Surface
val GalaxySurfaceHighlight = Color(0xFF2B1C52)// Surface Hover / Selection Highlight

// High-Contrast Galaxy Typography Colors
val TextWhite = Color(0xFFF8FAFC)
val TextLavender = Color(0xFFDDD6FE)         // Soft high-contrast Lavender
val TextGray = Color(0xFFA78BFA)             // Nebula Secondary Gray/Lavender
val TextMuted = Color(0xFF7C6FA0)            // Subtle Violet-Muted Text

// Luminous Glass Overlays
val GlassOverlay = Color(0xCC0E0822)
val GlassCard = Color(0xB317102D)

// Signature Galaxy Gradient Brushes
val GalaxyPrimaryGradient = Brush.horizontalGradient(
    listOf(GalaxyOrange, GalaxyPink, GalaxyMagenta, GalaxyViolet)
)
val GalaxyNeonGradient = Brush.horizontalGradient(
    listOf(GalaxyCyan, GalaxyPink, GalaxyViolet)
)
val GalaxyRouteGradient = Brush.horizontalGradient(
    listOf(GalaxyCyan, GalaxyMagenta, GalaxyOrange)
)
val GalaxyCardGradient = Brush.verticalGradient(
    listOf(GalaxyCard, Color(0xFF130B26))
)

// Backward Compatibility Aliases with Galaxy Enhancements
val NeonCyan = GalaxyCyan
val NeonCyanVariant = GalaxyCyanVariant
val NeonOrange = GalaxyOrange
val NeonOrangeVariant = Color(0xFFFF3D00)
val NitroPurple = GalaxyPink
val NitroPurpleLight = GalaxyMagenta
val ElectricYellow = GalaxyGold
val RacingRed = GalaxyRed
val EmeraldGreen = GalaxyEmerald

val CarbonDark = GalaxyVoid
val CarbonCard = GalaxyCard
val CarbonCardBorder = GalaxyCardBorder
val CarbonSurface = GalaxySurface
val CarbonSurfaceHighlight = GalaxySurfaceHighlight


