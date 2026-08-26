package com.example.network

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BackupRestoreManager
import com.example.data.ConfigChangeLog
import com.example.data.DeviceEntity
import com.example.data.FullBackupResult
import com.example.data.MacHistory
import com.example.data.MacManagerFileLogger
import com.example.data.MacManagerLog
import com.example.data.NetworkEntity
import com.example.data.NetworkProfile
import com.example.data.NetworkRepository
import com.example.data.NetworkWithDevices
import com.example.data.SavedMac
import com.example.data.ScannedDeviceRecord
import com.example.network.AuthManager
import com.example.network.FirestoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.ai.AiAnalyzer
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface AiAnalysisState {
    object Idle : AiAnalysisState
    object Analyzing : AiAnalysisState
    data class Success(val result: String) : AiAnalysisState
    data class Error(val message: String) : AiAnalysisState
}

sealed interface ScanState {
    object Idle : ScanState
    object Scanning : ScanState
    data class Success(val devices: List<NetworkDevice>) : ScanState
    data class Error(val message: String) : ScanState
}

class NetGuardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NetworkRepository(application)
    
    // UI Navigation State
    private val screenStack = mutableListOf<Screen>()
    private val _currentScreen = MutableStateFlow(Screen.Main)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun setScreen(screen: Screen) {
        if (_currentScreen.value != screen) {
            screenStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        return if (screenStack.isNotEmpty()) {
            _currentScreen.value = screenStack.removeAt(screenStack.lastIndex)
            true
        } else {
            false
        }
    }

    // Interface
    var selectedInterface = MutableStateFlow("wlan0")
    val availableInterfaces = MutableStateFlow<List<String>>(RootServices.getAvailableNetworkInterfaces())

    // Terminal Commands / Console Logs
    private val _terminalLogs = MutableStateFlow<List<String>>(
        listOf("بوابة حماية الشبكة NetGuard Root جاهزة للعمل...", "بانتظار تنفيذ الأوامر الهندسية...")
    )
    val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    // MAC Addresses States
    private val _currentMac = MutableStateFlow("")
    val currentMac: StateFlow<String> = _currentMac.asStateFlow()

    private val _hardwareMac = MutableStateFlow("")
    val hardwareMac: StateFlow<String> = _hardwareMac.asStateFlow()

    private val _localIp = MutableStateFlow("")
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    // NetworkConfigModule States
    val customLocalIp: StateFlow<String?> = NetworkConfigModule.customLocalIp
    val isIpNatConfigured: StateFlow<Boolean> = NetworkConfigModule.isNatConfigured

    // Room Database Audit Logs
    val configChangeLogs: StateFlow<List<ConfigChangeLog>> = repository.configChangeLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _subnet = MutableStateFlow("192.168.1")
    val subnet: StateFlow<String> = _subnet.asStateFlow()

    // Root Status State Flow
    private val _isRootGranted = MutableStateFlow(RootServices.isRootAvailable())
    val isRootGranted: StateFlow<Boolean> = _isRootGranted.asStateFlow()

    // Protection Setting States
    private val _isAntiBanEnabled = MutableStateFlow(false)
    val isAntiBanEnabled: StateFlow<Boolean> = _isAntiBanEnabled.asStateFlow()

    private val _isRandomMacDisabled = MutableStateFlow(false)
    val isRandomMacDisabled: StateFlow<Boolean> = _isRandomMacDisabled.asStateFlow()

    // Root-level Firewall Rule State
    private val _isFirewallEnabled = MutableStateFlow(false)
    val isFirewallEnabled: StateFlow<Boolean> = _isFirewallEnabled.asStateFlow()

    // Secure DNS Redirection State via Root iptables
    val isDnsRedirectionActive: StateFlow<Boolean> = NetworkConfigModule.isDnsRedirectionActive
    val activeDnsServer: StateFlow<String> = NetworkConfigModule.activeDnsServer

    // Biometric Authentication States & Prompt
    private val _isBiometricEnabled = MutableStateFlow(true)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isBiometricAuthenticated = MutableStateFlow(false)
    val isBiometricAuthenticated: StateFlow<Boolean> = _isBiometricAuthenticated.asStateFlow()

    fun setBiometricAuthenticated(authenticated: Boolean) {
        _isBiometricAuthenticated.value = authenticated
        if (authenticated) {
            addLog("تم تأكيد مصادقة البصمة/الوجه بنجاح (Biometric Access Granted)")
        }
    }

    fun toggleBiometricEnabled(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
        if (!enabled) {
            _isBiometricAuthenticated.value = true
            addLog("تم إيقاف قفل البصمة من إعدادات الأمان.")
        } else {
            _isBiometricAuthenticated.value = false
            addLog("تم تفعيل حماية البصمة/الوجه لجميع عمليات النظام والشبكة.")
        }
    }

    fun triggerBiometricPrompt(activity: FragmentActivity) {
        if (!_isBiometricEnabled.value) {
            _isBiometricAuthenticated.value = true
            return
        }

        try {
            val biometricManager = BiometricManager.from(activity)
            val canAuth = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )

            if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
                val executor = ContextCompat.getMainExecutor(activity)
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("تأمين NetGuard Root المشفر")
                    .setSubtitle("المصادقة بواسطة البصمة أو الوجه للدخول إلى أدوات الجذر والشبكة الحساسة")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()

                val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        setBiometricAuthenticated(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        addLog("تنبيه البصمة: $errString")
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        addLog("فشلت محاولة المصادقة بالبصمة/الوجه.")
                    }
                })

                biometricPrompt.authenticate(promptInfo)
            } else {
                addLog("تنبيه: أجهزة البصمة غير مسجلة أو غائبة. يمكن استخدام وضع المحاكاة/التجاوز.")
            }
        } catch (e: Exception) {
            addLog("خطأ في تشغيل واجهة المصادقة البيومترية: ${e.localizedMessage}")
        }
    }

    // Light/Dark/System Theme Preference State
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        val label = when (mode) {
            ThemeMode.SYSTEM -> "تلقائي حسب النظام (System Default)"
            ThemeMode.LIGHT -> "المظهر الفاتح (Light Mode)"
            ThemeMode.DARK -> "المظهر الداكن (Dark Mode)"
        }
        addLog("تم ضبط مظهر التطبيق إلى: $label")
    }

    // Connection Details States
    private val _connectionType = MutableStateFlow("مجهول")
    val connectionType: StateFlow<String> = _connectionType.asStateFlow()

    private val _interfaceStatus = MutableStateFlow("خامل")
    val interfaceStatus: StateFlow<String> = _interfaceStatus.asStateFlow()

    // Scan States
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    // AI Analysis States
    private val _aiAnalysisState = MutableStateFlow<AiAnalysisState>(AiAnalysisState.Idle)
    val aiAnalysisState: StateFlow<AiAnalysisState> = _aiAnalysisState.asStateFlow()

    // Scan Strategy Choice
    private val _selectedScanStrategy = MutableStateFlow(ScanStrategy.HYBRID)
    val selectedScanStrategy: StateFlow<ScanStrategy> = _selectedScanStrategy.asStateFlow()

    fun selectScanStrategy(strategy: ScanStrategy) {
        _selectedScanStrategy.value = strategy
        addLog("تغيير استراتيجية البحث والمسح إلى: ${strategy.displayName}")
    }

    fun analyzeNetworkWithAi() {
        val currentState = _scanState.value
        if (currentState is ScanState.Success) {
            _aiAnalysisState.value = AiAnalysisState.Analyzing
            addLog("بدء تحليل الشبكة بالذكاء الاصطناعي العالي (Gemini 3.1 Pro Thinking Mode)...")
            viewModelScope.launch {
                val devicesText = currentState.devices.joinToString("\\n") { "IP: ${it.ip}, MAC: ${it.mac}" }
                val result = AiAnalyzer.analyzeNetwork(devicesText)
                if (result.startsWith("Error")) {
                    _aiAnalysisState.value = AiAnalysisState.Error(result)
                    addLog("فشل في الوصول للذكاء الاصطناعي: $result")
                } else {
                    _aiAnalysisState.value = AiAnalysisState.Success(result)
                    addLog("اكتمل تحليل الذكاء الاصطناعي للشبكة بنجاح.")
                }
            }
        }
    }

    // Room Database Observables
    val savedMacs: StateFlow<List<SavedMac>> = repository.savedMacs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val macHistory: StateFlow<List<MacHistory>> = repository.macHistoryList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val networkProfiles: StateFlow<List<NetworkProfile>> = repository.networkProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scannedDevicesHistory: StateFlow<List<ScannedDeviceRecord>> = repository.scannedDevicesHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val macManagerLogs: StateFlow<List<MacManagerLog>> = repository.macManagerLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNetworksWithDevices: StateFlow<List<NetworkWithDevices>> = repository.allNetworksWithDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks currently blocked devices in App state
    private val _blockedDevices = MutableStateFlow<List<NetworkDevice>>(emptyList())
    val blockedDevices: StateFlow<List<NetworkDevice>> = _blockedDevices.asStateFlow()

    // Root Validation & Libsu Shell Management State via RootAccessManager
    val rootVerificationStatus: StateFlow<RootVerificationStatus> = RootAccessManager.verificationStatus
    val rootValidationInfo: StateFlow<RootValidationInfo?> = RootAccessManager.rootValidationInfo

    private val _commandHistory = MutableStateFlow<List<ShellCommandResult>>(emptyList())
    val commandHistory: StateFlow<List<ShellCommandResult>> = _commandHistory.asStateFlow()

    private val _isExecutingCommand = MutableStateFlow(false)
    val isExecutingCommand: StateFlow<Boolean> = _isExecutingCommand.asStateFlow()

    init {
        requestRootAccess()
    }

    fun executeShellCommand(commandStr: String) {
        if (commandStr.isBlank()) return
        viewModelScope.launch {
            _isExecutingCommand.value = true
            addLog("$ $commandStr")
            val result = RootAccessManager.executeCommand(commandStr)
            _commandHistory.value = listOf(result) + _commandHistory.value
            _isExecutingCommand.value = false
            
            if (result.isSuccess) {
                addLog("[نجاح libsu] تم تنفيذ الأمر: ${result.command} في ${result.durationMs}ms")
            } else {
                addLog("[خطأ libsu] فشل الأمر: ${result.command} كود: ${result.code}")
            }
        }
    }

    fun clearCommandHistory() {
        _commandHistory.value = emptyList()
        addLog("تم مسح سجل أوامر libsu Terminal.")
    }

    fun refreshRootValidationInfo() {
        viewModelScope.launch {
            val status = RootAccessManager.verifyRootAccessOnLaunch()
            val info = RootAccessManager.rootValidationInfo.value
            _isRootGranted.value = status == RootVerificationStatus.VERIFIED_ROOT
            addLog("تم فحص معايير الروت عبر RootAccessManager: ${status.labelAr} (${info?.suBinaryPath ?: ""})")
        }
    }

    fun addLog(log: String) {
        val current = _terminalLogs.value.toMutableList()
        current.add(0, "[${System.currentTimeMillis().formatTime()}] $log")
        if (current.size > 100) {
            current.removeAt(current.size - 1)
        }
        _terminalLogs.value = current
    }

    private fun Long.formatTime(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(this))
    }

    fun refreshNetworkInfo() {
        viewModelScope.launch {
            _isRootGranted.value = RootServices.isRootAvailable()
            val context = getApplication<Application>()
            val ip = RootServices.getLocalIpAddress(context)
            _localIp.value = ip
            
            // Use the more robust user recommended subnet detection
            var computedSubnet = RootServices.getCurrentSubnet(context)
            
            // Fallback if getCurrentSubnet returns default or blank and IP is available
            if (computedSubnet == "192.168.1" && ip != "192.168.1.105") {
                computedSubnet = RootServices.getSubnet(ip)
            }
            
            _subnet.value = computedSubnet

            val iface = selectedInterface.value
            _hardwareMac.value = RootServices.getHardwareMacAddress(iface)
            _currentMac.value = RootServices.getCurrentMacAddress(iface)
            
            _connectionType.value = RootServices.getNetworkConnectionType(context)
            _interfaceStatus.value = RootServices.getNetworkInterfaceStatus(iface)
            _isFirewallEnabled.value = RootServices.isFirewallEnabled()
            
            addLog("تم تحديث معلومات الشبكة: IP =$ip, Subnet =$computedSubnet.0/24")
        }
    }

    fun toggleRootFirewall(enable: Boolean) {
        viewModelScope.launch {
            val logs = mutableListOf<String>()
            val success = RootServices.setRootFirewallEnabled(enable, logs)
            for (line in logs) {
                addLog(line)
            }
            if (success) {
                _isFirewallEnabled.value = enable
                addLog("تم تحديث حالة جدار الحماية الجذري (Root Firewall) -> " + if (enable) "مفعّل 🛡️" else "معطّل 🔓")
            }
        }
    }

    fun requestRootAccess() {
        viewModelScope.launch {
            addLog("جاري التحقق من صلاحية الروت بواسطة RootAccessManager عبر مكتبة libsu...")
            val status = RootAccessManager.verifyRootAccessOnLaunch()
            val isGranted = status == RootVerificationStatus.VERIFIED_ROOT
            _isRootGranted.value = isGranted
            if (isGranted) {
                addLog("[تأكيد] تم توثيق تفويض الروت بنجاح!")
            } else {
                addLog("[خطأ] تعذر الحصول على صلاحيات الروت. التطبيق يتطلب روت للعمل.")
            }
            refreshNetworkInfo()
        }
    }

    fun isValidMac(mac: String): Boolean {
        val clean = mac.trim().uppercase().replace("-", ":")
        val regex = "^([0-9A-F]{2}:){5}[0-9A-F]{2}$".toRegex()
        return regex.matches(clean)
    }

    fun triggerMacChange(newMac: String, targetIface: String = selectedInterface.value) {
        val iface = targetIface.ifBlank { selectedInterface.value }
        val cleanMac = newMac.trim().uppercase().replace("-", ":")
        val timestamp = System.currentTimeMillis()
        val context = getApplication<Application>()

        if (!isValidMac(cleanMac)) {
            val errorMsg = "عنوان MAC غير صالح: '$newMac'"
            addLog("[خطأ] $errorMsg")
            viewModelScope.launch {
                repository.addMacManagerLog(
                    action = "CHANGE_MAC",
                    interfaceUsed = iface,
                    targetMac = newMac,
                    isSuccess = false,
                    message = "Failed attempt: Invalid MAC address format ($newMac)"
                )
                MacManagerFileLogger.log(
                    context = context,
                    action = "CHANGE_MAC",
                    isSuccess = false,
                    interfaceUsed = iface,
                    macAddress = newMac,
                    message = "Failed: Invalid MAC format ($newMac)",
                    timestamp = timestamp
                )
            }
            return
        }

        viewModelScope.launch {
            val oldMac = _currentMac.value
            val logs = mutableListOf<String>()
            val success = RootServices.changeMacAddress(iface, cleanMac, logs)

            for (line in logs) {
                addLog(line)
            }

            if (success) {
                _currentMac.value = cleanMac
                repository.addHistoryEntry(cleanMac, oldMac)
                repository.addConfigChangeLog(
                    changeType = "MAC",
                    interfaceName = iface,
                    oldValue = oldMac.ifEmpty { "00:00:00:00:00:00" },
                    newValue = cleanMac,
                    details = "libsu (ip link dev $iface address)"
                )
                repository.addMacManagerLog(
                    action = "CHANGE_MAC",
                    interfaceUsed = iface,
                    targetMac = cleanMac,
                    isSuccess = true,
                    message = "Successfully changed MAC on $iface to $cleanMac"
                )
                MacManagerFileLogger.log(
                    context = context,
                    action = "CHANGE_MAC",
                    isSuccess = true,
                    interfaceUsed = iface,
                    macAddress = cleanMac,
                    message = "Success: Changed MAC on $iface from $oldMac to $cleanMac",
                    timestamp = timestamp
                )
                addLog("[تأكيد] تم تحديث الهوية وحفظ السجل في قاعدة البيانات وملف السجل بنجاح.")
            } else {
                repository.addMacManagerLog(
                    action = "CHANGE_MAC",
                    interfaceUsed = iface,
                    targetMac = cleanMac,
                    isSuccess = false,
                    message = "Failed attempt: Command execution failed on $iface"
                )
                MacManagerFileLogger.log(
                    context = context,
                    action = "CHANGE_MAC",
                    isSuccess = false,
                    interfaceUsed = iface,
                    macAddress = cleanMac,
                    message = "Failed: Command execution error on $iface",
                    timestamp = timestamp
                )
                addLog("[خطأ] فشل في تطبيق تغيير الماك على الواجهة $iface")
            }
        }
    }

    fun restoreMac(macAddress: String) {
        triggerMacChange(macAddress)
    }

    fun changeLocalIpViaIptables(newIp: String) {
        if (!_isRootGranted.value) {
            addLog("[خطأ] صلاحيات الروت مطلوبة لتغيير IP.")
            return
        }
        if (newIp.isBlank()) return
        viewModelScope.launch {
            val iface = selectedInterface.value
            val currentIpStr = _localIp.value.ifEmpty { "192.168.1.105" }
            val logs = mutableListOf<String>()

            val success = RootServices.changeLocalIpViaIptables(
                iface = iface,
                currentIp = currentIpStr,
                newIp = newIp,
                subnetPrefix = 24,
                logsList = logs
            )

            for (line in logs) {
                addLog(line)
            }

            if (success) {
                _localIp.value = newIp
                repository.addConfigChangeLog(
                    changeType = "IP",
                    interfaceName = iface,
                    oldValue = currentIpStr,
                    newValue = newIp,
                    details = if (RootServices.isRootAvailable()) "libsu (iptables SNAT/DNAT)" else "Simulation (Fallback Mode)"
                )
                addLog("[تأكيد NetworkConfig] تم إعادة توجيه IP لتصبح: $newIp وحفظ سجل التغيير قاعدة Room!")
            }
        }
    }

    fun deleteConfigChangeLog(id: Int) {
        viewModelScope.launch {
            repository.deleteConfigChangeLog(id)
            addLog("تم حذف إدخال السجل بنجاح.")
        }
    }

    fun clearConfigChangeLogs() {
        viewModelScope.launch {
            repository.clearConfigChangeLogs()
            addLog("تم مسح كافة سجلات تغييرات الشبكة.")
        }
    }

    fun resetLocalIpConfig() {
        viewModelScope.launch {
            val iface = selectedInterface.value
            val currentIpStr = _localIp.value
            val originalIpStr = "192.168.1.105"
            val logs = mutableListOf<String>()

            val success = RootServices.resetLocalIpConfig(
                iface = iface,
                currentIp = currentIpStr,
                originalIp = originalIpStr,
                subnetPrefix = 24,
                logsList = logs
            )

            for (line in logs) {
                addLog(line)
            }

            if (success) {
                refreshNetworkInfo()
                addLog("[تأكيد NetworkConfig] تم مسح قواعد iptables NAT وتصفير IP العشوائي.")
            }
        }
    }

    fun generateRandomAndPreFill(): String {
        val randomMac = RootServices.generateRandomMacAddress()
        addLog("تم إنتاج عنوان ماك عشوائي ذكي بنجاح: $randomMac")
        return randomMac
    }

    // Save a custom MAC to DB safely
    fun saveMacAddress(title: String, mac: String, onResult: ((Boolean, String) -> Unit)? = null) {
        val cleanTitle = if (title.isBlank()) "جهاز مجهول" else title.trim()
        val cleanMac = mac.trim().uppercase().replace("-", ":")

        if (!isValidMac(cleanMac)) {
            val errorMsg = "عنوان MAC غير صالح: '$mac'"
            addLog("[خطأ] $errorMsg")
            onResult?.invoke(false, errorMsg)
            return
        }

        viewModelScope.launch {
            try {
                repository.insertSavedMac(cleanTitle, cleanMac)
                try {
                    AuthManager.currentUser?.let { user ->
                        FirestoreManager.saveMac(com.example.model.SavedMac(userId = user.uid, title = cleanTitle, macAddress = cleanMac))
                    }
                } catch (e: Throwable) {
                    addLog("تحذير: لم يتم المزامنة مع السحابة: ${e.message}")
                }
                addLog("تم حفظ العنوان [$cleanTitle -> $cleanMac] في قائمة العناوين المحفوظة.")
                onResult?.invoke(true, "تم حفظ MAC بنجاح: $cleanTitle")
            } catch (e: Throwable) {
                val errorMsg = "خطأ أثناء الحفظ: ${e.message ?: "خطأ غير معروف"}"
                addLog("[خطأ DB] $errorMsg")
                onResult?.invoke(false, errorMsg)
            }
        }
    }

    fun signIn(idToken: String) {
        // This is a simplified example. In a real app, use Google Sign-in to get the token, then pass to Firebase.
    }

    fun signOut() {
        try {
            AuthManager.signOut()
            addLog("تم تسجيل الخروج.")
        } catch (e: Throwable) {
            addLog("خطأ في تسجيل الخروج: ${e.message}")
        }
    }

    fun deleteSavedMac(savedMac: SavedMac) {
        viewModelScope.launch {
            repository.deleteSavedMac(savedMac)
            addLog("تم مسح العنوان [${savedMac.title}] من القائمة.")
        }
    }

    fun deleteHistoryEntry(id: Int) {
        viewModelScope.launch {
            repository.deleteHistoryEntry(id)
            addLog("تم حذف إدخال السجل بنجاح.")
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            addLog("تم مسح كود وسجل التغييرات بالكامل.")
        }
    }

    // Network Scanner Control
    fun startScan() {
        viewModelScope.launch {
            _scanState.value = ScanState.Scanning
            val context = getApplication<Application>()
            val logs = mutableListOf<String>()
            
            val subnet = RootServices.getCurrentSubnet(context)
            _subnet.value = subnet
            
            val devices = RootServices.scanNetwork("$subnet.0/24", context, logs, _selectedScanStrategy.value)
            
            for (log in logs) {
                addLog(log)
            }
            
            _scanState.value = ScanState.Success(devices)
            updateBlockedDevicesList(devices)
            
            if (devices.isNotEmpty()) {
                repository.saveScanResults(devices)
                val wifiSsid = "Wi-Fi Network (${_subnet.value})"
                val wifiBssid = "02:00:00:00:00:00"
                val deviceEntities = devices.map { dev ->
                    DeviceEntity(
                        ipAddress = dev.ip,
                        macAddress = dev.mac,
                        customName = dev.name,
                        isBlocked = dev.isBlocked
                    )
                }
                repository.saveNetworkScan(wifiSsid, wifiBssid, deviceEntities)
                addLog("تم حفظ نتائج مسح ${devices.size} جهاز لشبكة $wifiSsid تلقائياً في قاعدة البيانات (Room NetworkEntity & DeviceEntity).")
            }
        }
    }

    fun deleteScannedDeviceRecord(id: Int) {
        viewModelScope.launch {
            repository.deleteScannedDeviceRecord(id)
            addLog("تم حذف عنصر مسح الأجهزة من قاعدة بيانات Room.")
        }
    }

    fun clearScannedDevicesHistory() {
        viewModelScope.launch {
            repository.clearScannedDeviceHistory()
            addLog("تم مسح كود وسجل نتائج المسح المحفوظة بالكامل من قاعدة البيانات.")
        }
    }

    // NetCut Operations (Block/Unblock)
    fun toggleDeviceBlock(device: NetworkDevice) {
        val updatedDevice = device.copy(isBlocked = !device.isBlocked)
        
        val currentState = _scanState.value
        if (currentState is ScanState.Success) {
            val updatedList = currentState.devices.map {
                if (it.ip == device.ip) updatedDevice else it
            }
            _scanState.value = ScanState.Success(updatedList)
            updateBlockedDevicesList(updatedList)
        }
        
        addLog("Device ${device.ip} ${if (updatedDevice.isBlocked) "blocked" else "unblocked"}")
        
        // Ensure we actually call RootServices
        val context = getApplication<Application>()
        val gatewayIp = "${_subnet.value}.1"
        if (updatedDevice.isBlocked) {
             RootServices.startBlockDevice(device.ip, device.mac, gatewayIp, mutableListOf(), context)
        } else {
             RootServices.stopBlockDevice(device.ip, mutableListOf())
        }
    }

    private fun updateBlockedDevicesList(allDevices: List<NetworkDevice>) {
        _blockedDevices.value = allDevices.filter { it.isBlocked }
    }

    // Standalone root-based iptables IP Blocking for scanned devices
    fun blockIpViaIptables(ip: String) {
        viewModelScope.launch {
            val logs = mutableListOf<String>()
            val success = RootServices.blockIpViaIptables(ip, logs)
            for (line in logs) {
                addLog(line)
            }
            if (success) {
                addLog("[iptables Root Firewall] تم تطبيق حظر IP: $ip بنجاح 🛡️")
            }
        }
    }

    fun unblockIpViaIptables(ip: String) {
        viewModelScope.launch {
            val logs = mutableListOf<String>()
            val success = RootServices.unblockIpViaIptables(ip, logs)
            for (line in logs) {
                addLog(line)
            }
            if (success) {
                addLog("[iptables Root Firewall] تم إلغاء حظر IP: $ip بنجاح 🔓")
            }
        }
    }

    // Toggle Protection Settings
    fun toggleAntiBan(enable: Boolean) {
        viewModelScope.launch {
            val logs = mutableListOf<String>()
            val gatewayIp = "${_subnet.value}.1"
            val success = if (enable) {
                // Find potential gateway mac in active list
                val routerMac = "00:11:22:33:44:55"
                RootServices.enableAntiBan(gatewayIp, routerMac, logs)
            } else {
                RootServices.disableAntiBan(gatewayIp, logs)
            }

            for (line in logs) {
                addLog(line)
            }

            if (success) {
                _isAntiBanEnabled.value = enable
                addLog("تم تحديث وضع حماية الجهاز -> " + if (enable) "مفعّل" else "ملغى")
            }
        }
    }

    fun toggleRandomMacPrevention(enable: Boolean, targetIface: String = selectedInterface.value) {
        val iface = targetIface.ifBlank { selectedInterface.value }
        val context = getApplication<Application>()
        val timestamp = System.currentTimeMillis()
        val actionName = if (enable) "DISABLE_RANDOM_MAC" else "ENABLE_RANDOM_MAC"

        viewModelScope.launch {
            val logs = mutableListOf<String>()
            val success = RootServices.disableMacRandomization(enable, logs)
            
            for (line in logs) {
                addLog(line)
            }
            
            val targetMacStr = _currentMac.value.ifBlank { "00:00:00:00:00:00" }
            val statusMsg = if (enable) {
                if (success) "Random MAC randomization disabled (Fixed MAC Mode)" else "Failed to disable MAC randomization"
            } else {
                if (success) "Random MAC randomization enabled (Dynamic MAC Mode)" else "Failed to enable MAC randomization"
            }

            if (success) {
                _isRandomMacDisabled.value = enable
                addLog("تم تعديل منع الماك العشوائي -> " + if (enable) "مفعّل (الماك ثابت)" else "معطّل (الماك عشوائي)")
            }

            repository.addMacManagerLog(
                action = actionName,
                interfaceUsed = iface,
                targetMac = targetMacStr,
                isSuccess = success,
                message = statusMsg
            )
            MacManagerFileLogger.log(
                context = context,
                action = actionName,
                isSuccess = success,
                interfaceUsed = iface,
                macAddress = targetMacStr,
                message = statusMsg,
                timestamp = timestamp
            )
        }
    }

    fun disableRandomMac(targetIface: String = selectedInterface.value) {
        toggleRandomMacPrevention(true, targetIface)
    }

    fun clearMacManagerLogs() {
        viewModelScope.launch {
            repository.clearMacManagerLogs()
            MacManagerFileLogger.clearLogs(getApplication())
            addLog("تم مسح كافة سجلات MacManager من قاعدة البيانات والملف.")
        }
    }

    fun deleteMacManagerLog(id: Int) {
        viewModelScope.launch {
            repository.deleteMacManagerLog(id)
        }
    }

    fun getMacManagerLogFilePath(): String {
        return MacManagerFileLogger.getLogFilePath(getApplication())
    }

    fun readMacManagerLogFile(): String {
        return MacManagerFileLogger.readLogs(getApplication())
    }

    // Network Profiles Management (Room Database)
    fun saveNetworkProfile(profileName: String, interfaceName: String, mac: String, ip: String = "") {
        if (profileName.isBlank() || mac.isBlank()) return
        viewModelScope.launch {
            repository.insertNetworkProfile(profileName, interfaceName, mac, ip)
            addLog("تم حفظ بروفايل إعدادات الشبكة: '$profileName' (MAC: $mac, IP: ${if (ip.isBlank()) "افتراضي" else ip}) في قاعدة البيانات.")
        }
    }

    fun deleteNetworkProfile(id: Int) {
        viewModelScope.launch {
            repository.deleteNetworkProfile(id)
            addLog("تم حذف بروفايل الشبكة من قاعدة بيانات Room.")
        }
    }

    fun applyNetworkProfile(profile: NetworkProfile) {
        viewModelScope.launch {
            addLog("جاري تطبيق بروفايل الشبكة: '${profile.profileName}' على الواجهة ${profile.interfaceName}...")
            triggerMacChange(profile.customMac)
            if (profile.customIp.isNotBlank()) {
                val logsList = mutableListOf<String>()
                NetworkConfigModule.changeLocalIpViaIptables(
                    iface = profile.interfaceName,
                    currentIp = _localIp.value.ifEmpty { "192.168.1.100" },
                    newIp = profile.customIp,
                    subnetPrefix = 24,
                    logs = logsList
                )
                for (l in logsList) { addLog(l) }
                repository.addConfigChangeLog(
                    changeType = "PROFILE_APPLY",
                    interfaceName = profile.interfaceName,
                    oldValue = _localIp.value,
                    newValue = profile.customIp,
                    details = "تم تطبيق بروفايل Room Database: ${profile.profileName}"
                )
            }
        }
    }

    // MAC Randomization via LibSuCommandExecutor
    fun randomizeMacViaLibSu(iface: String = selectedInterface.value) {
        viewModelScope.launch {
            addLog("جاري توليد عنوان MAC عشوائي وتطبيقه عبر ip link و libsu...")
            val (success, resultMac) = LibSuCommandExecutor.randomizeMacAddress(iface)
            if (success) {
                val oldMac = _currentMac.value
                _currentMac.value = resultMac
                repository.addHistoryEntry(resultMac, oldMac)
                repository.addConfigChangeLog(
                    changeType = "MAC_RANDOMIZE",
                    interfaceName = iface,
                    oldValue = oldMac,
                    newValue = resultMac,
                    details = "تغيير عشوائي تلقائي مكسور الحماية"
                )
                addLog("[نجاح MAC Randomization] تم تعيين العنوان العشوائي: $resultMac بنجاح 🎲")
            } else {
                addLog("[خطأ MAC Randomization] $resultMac")
            }
        }
    }

    // IP and iptables Flushing via LibSuCommandExecutor
    fun flushIpAndIptablesViaLibSu(iface: String = selectedInterface.value) {
        viewModelScope.launch {
            addLog("جاري تنظيف وتفريغ كافة قواعد iptables وجداول الجيران (IP Flush)...")
            val result = LibSuCommandExecutor.flushIpAndIptables(iface)
            _commandHistory.value = listOf(result) + _commandHistory.value
            if (result.isSuccess) {
                addLog("[نجاح IP/iptables Flush] تم تفريغ كافة قواعد الجدار الناري والتوجيه النواة بنجاح 🧹")
            } else {
                addLog("[تنبيه IP/iptables Flush] ${result.stderr.firstOrNull() ?: "اكتمل تفريغ الجداول مع ملاحظات"}")
            }
        }
    }

    // Toggle Secure DNS Redirection via Root iptables
    fun toggleDnsRedirection(enable: Boolean, dnsIp: String = "1.1.1.1") {
        viewModelScope.launch {
            val logsList = mutableListOf<String>()
            if (enable) {
                val res = NetworkConfigModule.enableDnsRedirection(dnsIp, selectedInterface.value, logsList)
                _commandHistory.value = listOf(res) + _commandHistory.value
                for (l in logsList) addLog(l)
                repository.addConfigChangeLog(
                    changeType = "DNS_REDIRECT_ENABLE",
                    interfaceName = selectedInterface.value,
                    oldValue = "DEFAULT",
                    newValue = dnsIp,
                    details = "إعادة توجيه كافة حركة DNS إلى الخادم الآمن عبر iptables"
                )
            } else {
                val res = NetworkConfigModule.disableDnsRedirection(dnsIp, logsList)
                _commandHistory.value = listOf(res) + _commandHistory.value
                for (l in logsList) addLog(l)
                repository.addConfigChangeLog(
                    changeType = "DNS_REDIRECT_DISABLE",
                    interfaceName = selectedInterface.value,
                    oldValue = dnsIp,
                    newValue = "DEFAULT",
                    details = "إلغاء إعادة توجيه حركة DNS ومسح قواعد iptables"
                )
            }
        }
    }

    // Export Full Application Backup Data to JSON String
    fun exportFullAppBackup(): String {
        val macs = savedMacs.value
        val history = macHistory.value
        val logs = configChangeLogs.value
        val profiles = networkProfiles.value
        return BackupRestoreManager.exportFullAppBackupToJson(macs, history, logs, profiles)
    }

    // Import Full Application Backup Data from JSON String
    fun importFullAppBackup(jsonString: String, clearExisting: Boolean = true): Boolean {
        return try {
            val result = BackupRestoreManager.importFullAppBackupFromJson(jsonString)
            viewModelScope.launch {
                repository.restoreFullBackup(
                    savedMacs = result.savedMacs,
                    historyList = result.history,
                    logsList = result.logs,
                    profilesList = result.profiles,
                    clearExisting = clearExisting
                )
                addLog("[نسخ احتياطي] تم استعادة بيانات التطبيق بالكامل بنجاح (${result.savedMacs.size} MAC, ${result.profiles.size} بروفايل) 📦")
            }
            true
        } catch (e: Exception) {
            addLog("[خطأ نسخ احتياطي] فشل استعادة البيانات: ${e.localizedMessage}")
            false
        }
    }

    // Export All Saved MACs to JSON String
    fun exportSavedMacs(): String {
        val macs = savedMacs.value
        return BackupRestoreManager.exportSavedMacsToJson(macs)
    }

    // Import Saved MACs from JSON String
    fun importSavedMacs(jsonString: String, clearExisting: Boolean = false): Boolean {
        return try {
            val macs = BackupRestoreManager.importSavedMacsFromJson(jsonString)
            viewModelScope.launch {
                repository.restoreSavedMacs(macs, clearExisting)
                addLog("[تصدير/استعادة] تم استعادة ${macs.size} عنوان MAC بنجاح إلى القائمة المحفوظة 💾")
            }
            true
        } catch (e: Exception) {
            addLog("[خطأ استعادة MACs] فشل استيراد العناوين: ${e.localizedMessage}")
            false
        }
    }
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class Screen {
    Main, MacManager, Logs, SavedMacs, History, NetworkRadar, BlockedDevices, Settings, RootConsole, SignIn, NetworkIdentity
}
