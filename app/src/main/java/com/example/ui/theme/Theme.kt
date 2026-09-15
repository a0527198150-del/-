package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val KosherColorScheme = darkColorScheme(
    primary = Accent.Primary,
    onPrimary = Ivory.Strong,
    primaryContainer = Accent.PrimaryDeep,
    onPrimaryContainer = Ivory.Strong,
    secondary = Accent.Gold,
    onSecondary = Ink.Base,
    secondaryContainer = Accent.GoldDeep,
    onSecondaryContainer = Accent.GoldSoft,
    tertiary = Accent.Teal,
    onTertiary = Ink.Base,
    background = Ink.Base,
    onBackground = Ivory.Strong,
    surface = Ink.Surface,
    onSurface = Ivory.Strong,
    surfaceVariant = Ink.SurfaceHigh,
    onSurfaceVariant = Ivory.Muted,
    surfaceContainer = Ink.Elevated,
    surfaceContainerHigh = Ink.SurfaceHigh,
    surfaceContainerHighest = Ink.SurfaceTop,
    outline = Line.Regular,
    outlineVariant = Line.Soft,
    error = Accent.Danger,
    onError = Ivory.Strong,
    errorContainer = Accent.DangerDeep,
    onErrorContainer = Accent.Danger,
    scrim = Ink.Base,
)

private val KosherShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * The app is Hebrew-first, so the whole tree is laid out right-to-left.
 * Islands that must stay left-to-right (the player scrubber, for example)
 * opt back in locally.
 */
@Composable
fun KosherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KosherColorScheme,
        typography = Typography,
        shapes = KosherShapes,
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            content = content,
        )
    }
}

/** Kept so existing entry points and tests keep compiling. */
@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) = KosherTheme(content)
