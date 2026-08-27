package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.network.AnimationLevel

val Local3dEffectsEnabled = compositionLocalOf { true }
val LocalAnimationLevel = compositionLocalOf { AnimationLevel.FULL }

private val DarkColorScheme = darkColorScheme(
    primary = DarkGreenRaw,
    onPrimary = DarkBgRaw,
    secondary = DarkSurfaceVariantRaw,
    onSecondary = DarkTextPrimaryRaw,
    tertiary = DarkRedRaw,
    onTertiary = DarkBgRaw,
    background = DarkBgRaw,
    onBackground = DarkTextPrimaryRaw,
    surface = DarkSurfaceRaw,
    onSurface = DarkTextPrimaryRaw,
    surfaceVariant = DarkSurfaceVariantRaw,
    onSurfaceVariant = DarkTextSecondaryRaw,
    outline = DarkBorderRaw
)

private val LightColorScheme = lightColorScheme(
    primary = LightGreenRaw,
    onPrimary = Color.White,
    secondary = LightSurfaceVariantRaw,
    onSecondary = LightTextPrimaryRaw,
    tertiary = LightRedRaw,
    onTertiary = Color.White,
    background = LightBgRaw,
    onBackground = LightTextPrimaryRaw,
    surface = LightSurfaceRaw,
    onSurface = LightTextPrimaryRaw,
    surfaceVariant = LightSurfaceVariantRaw,
    onSurfaceVariant = LightTextSecondaryRaw,
    outline = LightBorderRaw
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    is3dEffectsEnabled: Boolean = true,
    animationLevel: AnimationLevel = AnimationLevel.FULL,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        Local3dEffectsEnabled provides is3dEffectsEnabled,
        LocalAnimationLevel provides animationLevel
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

