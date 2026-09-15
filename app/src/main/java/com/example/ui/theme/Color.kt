package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * "Midnight Scroll" palette: a deep ink background with an indigo primary and a
 * warm gold accent reserved for anything that signals the kosher/supervised
 * nature of the app.
 */
object Ink {
    val Base = Color(0xFF07080F)
    val Elevated = Color(0xFF0E1020)
    val Surface = Color(0xFF14172A)
    val SurfaceHigh = Color(0xFF1B1F38)
    val SurfaceTop = Color(0xFF232847)
}

object Accent {
    val Primary = Color(0xFF6D5BF6)
    val PrimarySoft = Color(0xFF9A8CFF)
    val PrimaryDeep = Color(0xFF3B2FA8)
    val Gold = Color(0xFFF5C451)
    val GoldSoft = Color(0xFFFFE6A6)
    val GoldDeep = Color(0xFF3A2E10)
    val Teal = Color(0xFF34D1BF)
    val Danger = Color(0xFFFF5F73)
    val DangerDeep = Color(0xFF3A1620)
    val Success = Color(0xFF3ED598)
}

object Ivory {
    val Strong = Color(0xFFF3F5FF)
    val Muted = Color(0xFF9FA8CE)
    val Faint = Color(0xFF6A7295)
}

object Line {
    val Soft = Color(0xFF1E2238)
    val Regular = Color(0xFF282D49)
    val Strong = Color(0xFF373D63)
}

/** Shared gradients so hero surfaces look identical everywhere they appear. */
object Gradients {
    val Hero = Brush.linearGradient(
        colors = listOf(Color(0xFF2B1F7A), Color(0xFF181B3D), Color(0xFF0E1020))
    )
    val Royal = Brush.linearGradient(
        colors = listOf(Accent.Primary, Color(0xFF8B5CF6))
    )
    val GoldSheen = Brush.linearGradient(
        colors = listOf(Accent.Gold, Color(0xFFE0A227))
    )
    val CardGlow = Brush.verticalGradient(
        colors = listOf(Ink.SurfaceHigh, Ink.Surface)
    )
    val ScrimBottom = Brush.verticalGradient(
        colors = listOf(Color.Transparent, Color(0xCC05060C), Color(0xF205060C))
    )
    val ScrimTop = Brush.verticalGradient(
        colors = listOf(Color(0xB305060C), Color.Transparent)
    )
}
