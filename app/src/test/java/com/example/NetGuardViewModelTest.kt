package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.network.NetGuardViewModel
import com.example.network.RootServices
import com.example.network.Screen
import com.example.network.ThemeMode
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NetGuardViewModelTest {

    private lateinit var viewModel: NetGuardViewModel

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        viewModel = NetGuardViewModel(application)
    }

    @Test
    fun testNavigationStack() {
        assertEquals(Screen.Main, viewModel.currentScreen.value)

        viewModel.setScreen(Screen.SavedMacs)
        assertEquals(Screen.SavedMacs, viewModel.currentScreen.value)

        viewModel.setScreen(Screen.NetworkRadar)
        assertEquals(Screen.NetworkRadar, viewModel.currentScreen.value)

        val navigatedBack1 = viewModel.navigateBack()
        assertTrue(navigatedBack1)
        assertEquals(Screen.SavedMacs, viewModel.currentScreen.value)

        val navigatedBack2 = viewModel.navigateBack()
        assertTrue(navigatedBack2)
        assertEquals(Screen.Main, viewModel.currentScreen.value)

        val navigatedBack3 = viewModel.navigateBack()
        assertFalse(navigatedBack3)
        assertEquals(Screen.Main, viewModel.currentScreen.value)
    }

    @Test
    fun testThemeModeSwitching() {
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)

        viewModel.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)

        viewModel.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
    }

    @Test
    fun testMacAddressValidation() {
        assertTrue(RootServices.isValidMacAddress("00:11:22:33:44:55"))
        assertTrue(RootServices.isValidMacAddress("AA:BB:CC:DD:EE:FF"))
        assertTrue(RootServices.isValidMacAddress("aa:bb:cc:dd:ee:ff"))

        assertFalse(RootServices.isValidMacAddress("00:11:22:33:44"))
        assertFalse(RootServices.isValidMacAddress("00:11:22:33:44:55:66"))
        assertFalse(RootServices.isValidMacAddress("ZZ:11:22:33:44:55"))
        assertFalse(RootServices.isValidMacAddress("00-11-22-33-44-55"))
    }

    @Test
    fun testRandomMacGeneration() {
        val randomMac = RootServices.generateRandomMac()
        assertTrue(RootServices.isValidMacAddress(randomMac))
    }

    @Test
    fun testTerminalLogs() {
        val initialSize = viewModel.terminalLogs.value.size
        viewModel.addLog("Test log entry")
        val newLogs = viewModel.terminalLogs.value
        assertEquals(initialSize + 1, newLogs.size)
        assertTrue(newLogs.first().contains("Test log entry"))
    }

    @Test
    fun testRootDiagnosticState() {
        assertNotNull(viewModel.isRootGranted.value)
        assertNotNull(viewModel.rootVerificationStatus.value)
        viewModel.refreshRootValidationInfo()
        assertNotNull(viewModel.rootVerificationStatus.value)
    }
}
