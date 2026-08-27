package com.example.ui.theme

import com.example.network.AnimationLevel
import com.example.network.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global Theme State Manager for controlling Theme Mode, 3D Effects,
 * and Animation Level scaling across the NetGuard Root application.
 */
object ThemeStateManager {
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _is3dEffectsEnabled = MutableStateFlow(true)
    val is3dEffectsEnabled: StateFlow<Boolean> = _is3dEffectsEnabled.asStateFlow()

    private val _animationLevel = MutableStateFlow(AnimationLevel.FULL)
    val animationLevel: StateFlow<AnimationLevel> = _animationLevel.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun set3dEffectsEnabled(enabled: Boolean) {
        _is3dEffectsEnabled.value = enabled
    }

    fun setAnimationLevel(level: AnimationLevel) {
        _animationLevel.value = level
    }
}
