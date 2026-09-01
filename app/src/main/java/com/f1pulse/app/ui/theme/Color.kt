package com.f1pulse.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Accents
val F1Red = Color(0xFFE10600)
val F1RedDark = Color(0xFFB00500)
val F1Cyan = Color(0xFF00D2FF)
val F1Grey = Color(0xFF8E8E96)
val F1Green = Color(0xFF2DD4BF)
val F1Yellow = Color(0xFFFFD23F)

// Position gradient: P1 full red → P2 medium red → P3 light red
val F1Pos1 = Color(0xFFE10600)  // F1 official red
val F1Pos2 = Color(0xFFFF5945)  // lighter red
val F1Pos3 = Color(0xFFFF8C80)  // pale red

// Surfaces
val F1DarkBackground = Color(0xFF0A0A0C)
val F1DarkSurface = Color(0xFF121215)
val F1DarkSurfaceRaised = Color(0xFF1A1A1E)
val F1DarkSurfaceOverlay = Color(0xFF222228)

// Text
val F1TextPrimary = Color(0xFFEAEAEF)
val F1TextSecondary = Color(0xFF8E8E96)
val F1TextMuted = Color(0xFF5A5A64)

// Borders
val F1BorderSubtle = Color(0x0FFFFFFF)
val F1BorderStrong = Color(0x1FFFFFFF)
val F1Divider = Color(0x0DFFFFFF)

val LightColorScheme = lightColorScheme(
    primary = F1Red,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410001),
    secondary = F1Cyan,
    onSecondary = Color.Black,
    tertiary = Color(0xFF006A6A),
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF3F3F5),
    onSurfaceVariant = Color(0xFF4A4A50),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

val DarkColorScheme = darkColorScheme(
    primary = F1Red,
    onPrimary = Color.White,
    primaryContainer = F1RedDark,
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = F1Cyan,
    onSecondary = Color.Black,
    tertiary = F1Cyan,
    background = F1DarkBackground,
    onBackground = F1TextPrimary,
    surface = F1DarkSurface,
    onSurface = F1TextPrimary,
    surfaceVariant = F1DarkSurfaceRaised,
    onSurfaceVariant = F1TextSecondary,
    error = Color(0xFFFFB4AB),
    onError = Color.Black,
)
