package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    letterSpacing: Double = 0.0,
) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    // Hebrew needs a little more breathing room than the Material defaults.
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

val Typography = Typography(
    displaySmall = style(30, 38, FontWeight.Black, -0.5),
    headlineMedium = style(25, 32, FontWeight.Bold, -0.3),
    headlineSmall = style(21, 28, FontWeight.Bold, -0.2),
    titleLarge = style(18, 25, FontWeight.Bold),
    titleMedium = style(16, 22, FontWeight.SemiBold),
    titleSmall = style(14, 20, FontWeight.SemiBold),
    bodyLarge = style(15, 23, FontWeight.Normal),
    bodyMedium = style(13, 20, FontWeight.Normal),
    bodySmall = style(12, 18, FontWeight.Normal),
    labelLarge = style(14, 18, FontWeight.Bold, 0.1),
    labelMedium = style(12, 16, FontWeight.SemiBold, 0.2),
    labelSmall = style(11, 14, FontWeight.Medium, 0.3),
)
