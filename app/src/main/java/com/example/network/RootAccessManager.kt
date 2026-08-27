package com.example.network

import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

enum class RootVerificationStatus(val labelAr: String, val labelEn: String) {
    CHECKING("جاري فحص صلاحيات الروت...", "Verifying Root Access..."),
    VERIFIED_ROOT("صلاحية روت مؤكدة (libsu ACTIVE)", "Superuser Verified (libsu ACTIVE)"),
    SIMULATION_MODE("وضع المحاكاة (Simulation Fallback)", "Simulation Mode (No Root)"),
    DENIED("تم رفض صلاحية الروت", "Root Permission Denied")
}

object RootAccessManager {
    private const val TAG = "RootAccessManager"

    private val _verificationStatus = MutableStateFlow(RootVerificationStatus.CHECKING)
    val verificationStatus: StateFlow<RootVerificationStatus> = _verificationStatus.asStateFlow()

    private val _rootValidationInfo = MutableStateFlow<RootValidationInfo?>(null)
    val rootValidationInfo: StateFlow<RootValidationInfo?> = _rootValidationInfo.asStateFlow()

    /**
     * Verifies superuser permissions using libsu upon app launch or manual refresh.
     * Updates [verificationStatus] and [rootValidationInfo].
     */
    suspend fun verifyRootAccessOnLaunch(): RootVerificationStatus = withContext(Dispatchers.IO) {
        _verificationStatus.value = RootVerificationStatus.CHECKING
        Log.d(TAG, "Initiating libsu superuser permission verification on launch...")
        
        try {
            RootServices.resetRootCache()
            val (status, info) = RootDetector.detectRoot()
            _rootValidationInfo.value = info
            _verificationStatus.value = status
            Log.i(TAG, "Root verification completed: $status (UID: ${info.shellUid})")
            status
        } catch (e: Throwable) {
            Log.e(TAG, "Failed during libsu root verification", e)
            val fallbackInfo = RootValidationInfo(
                isRootGranted = false,
                shellUid = "تم رفض الإذن (Access Denied)",
                shellVersion = "libsu Error",
                selinuxMode = "Unknown",
                suBinaryPath = "غير متاح"
            )
            _rootValidationInfo.value = fallbackInfo
            _verificationStatus.value = RootVerificationStatus.DENIED
            RootVerificationStatus.DENIED
        }
    }

    /**
     * Quick check whether root is active and verified.
     */
    fun isRootVerified(): Boolean {
        return _verificationStatus.value == RootVerificationStatus.VERIFIED_ROOT
    }

    /**
     * Executes shell commands safely through libsu shell manager.
     */
    suspend fun executeCommand(commandStr: String): ShellCommandResult {
        return RootServices.executeLibsuCommand(commandStr)
    }
}
