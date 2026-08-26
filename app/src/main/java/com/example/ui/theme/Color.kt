package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Raw Color Definitions for Obsidian Cyber (Dark Mode)
val DarkBgRaw = Color(0xFF0A0C10)
val DarkSurfaceRaw = Color(0xFF12161F)
val DarkSurfaceVariantRaw = Color(0xFF1C2230)
val DarkGreenRaw = Color(0xFFC4FF60)
val DarkRedRaw = Color(0xFFFF5757)
val DarkOrangeRaw = Color(0xFFFFB03A)
val DarkTextPrimaryRaw = Color(0xFFE6EDF3)
val DarkTextSecondaryRaw = Color(0xFF8B949E)
val DarkBorderRaw = Color(0xFF30363D)

// Raw Color Definitions for Clean Cyber Light (Light Mode)
val LightBgRaw = Color(0xFFF1F5F9)
val LightSurfaceRaw = Color(0xFFFFFFFF)
val LightSurfaceVariantRaw = Color(0xFFE2E8F0)
val LightGreenRaw = Color(0xFF16A34A)
val LightRedRaw = Color(0xFFDC2626)
val LightOrangeRaw = Color(0xFFD97706)
val LightTextPrimaryRaw = Color(0xFF0F172A)
val LightTextSecondaryRaw = Color(0xFF475569)
val LightBorderRaw = Color(0xFFCBD5E1)

// Dynamic Composable Color getters to seamlessly support full Dark and Light modes across all UI components
val CyberDarkBg: Color @Composable get() = MaterialTheme.colorScheme.background
val CyberSurface: Color @Composable get() = MaterialTheme.colorScheme.surface
val CyberSurfaceVariant: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val CyberGreen: Color @Composable get() = MaterialTheme.colorScheme.primary
val CyberRed: Color @Composable get() = MaterialTheme.colorScheme.tertiary
val CyberOrange: Color @Composable get() = if (MaterialTheme.colorScheme.background == LightBgRaw) LightOrangeRaw else DarkOrangeRaw
val CyberTextPrimary: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val CyberTextSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val CyberBorder: Color @Composable get() = MaterialTheme.colorScheme.outline

// Direct Raw Color References
val CyberLightBg = LightBgRaw
val CyberLightSurface = LightSurfaceRaw
val CyberLightSurfaceVariant = LightSurfaceVariantRaw
val CyberLightTextPrimary = LightTextPrimaryRaw
val CyberLightTextSecondary = LightTextSecondaryRaw
val CyberLightBorder = LightBorderRaw
val CyberLightGreen = LightGreenRaw

// Backward compatibility or default references
val Purple80 = DarkGreenRaw
val PurpleGrey80 = DarkSurfaceVariantRaw
val Pink80 = DarkRedRaw

val Purple40 = DarkGreenRaw
val PurpleGrey40 = DarkSurfaceRaw
val Pink40 = DarkRedRaw

