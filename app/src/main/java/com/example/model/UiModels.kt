package com.example.model

sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    data class Success(val devices: List<com.example.network.NetworkDevice>) : ScanState()
    data class Error(val message: String) : ScanState()
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}
