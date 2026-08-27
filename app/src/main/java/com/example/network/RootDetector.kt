package com.example.network

import android.os.Build
import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Robust RootDetector component for Android 12+ (API 31+) using libsu.
 * Performs non-crashing superuser verification with timeout handling,
 * exception trapping, and graceful fallback to DENIED or SIMULATION_MODE states.
 */
object RootDetector {
    private const val TAG = "RootDetector"
    private const val DEFAULT_TIMEOUT_MS = 5000L

    /**
     * Safely checks if superuser root access is available with timeout protection.
     * Never throws an exception and safely handles Android 12+ security boundaries.
     */
    suspend fun checkRootAccess(timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = withTimeoutOrNull(timeoutMs) {
                // First check if app is explicitly granted root by libsu
                val appGranted = try { Shell.isAppGrantedRoot() } catch (_: Throwable) { null }
                if (appGranted == true) {
                    true
                } else if (appGranted == false) {
                    false
                } else {
                    // Fallback to Shell.rootAccess() check
                    Shell.rootAccess()
                }
            }
            result == true
        } catch (t: Throwable) {
            Log.e(TAG, "Root detection failed safely without crashing", t)
            false
        }
    }

    /**
     * Performs a full non-crashing root verification scan and returns [RootVerificationStatus] and [RootValidationInfo].
     */
    suspend fun detectRoot(timeoutMs: Long = DEFAULT_TIMEOUT_MS): Pair<RootVerificationStatus, RootValidationInfo> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Performing robust root detection (Android SDK: ${Build.VERSION.SDK_INT})...")

        val isRootAvailable = checkRootAccess(timeoutMs)

        if (!isRootAvailable) {
            val isDenied = try {
                Shell.isAppGrantedRoot() == false
            } catch (_: Throwable) {
                false
            }

            val status = if (isDenied) RootVerificationStatus.DENIED else RootVerificationStatus.SIMULATION_MODE
            val info = RootValidationInfo(
                isRootGranted = false,
                shellUid = if (isDenied) "تم رفض الإذن (Access Denied)" else "غير متاح (محاكاة)",
                shellVersion = "libsu v5.2.2+",
                selinuxMode = if (Build.VERSION.SDK_INT >= 31) "Enforcing (Android 12+)" else "Enforcing",
                suBinaryPath = "غير متاح"
            )
            return@withContext Pair(status, info)
        }

        // Retrieve system shell parameters safely
        return@withContext try {
            val uidRes = withTimeoutOrNull(2000L) { Shell.cmd("id").exec() }
            val uidStr = if (uidRes?.isSuccess == true && !uidRes.out.isNullOrEmpty()) uidRes.out.joinToString(" ") else "uid=0(root) gid=0(root)"

            val suRes = withTimeoutOrNull(2000L) { Shell.cmd("su -v").exec() }
            val suVersion = if (suRes?.isSuccess == true && !suRes.out.isNullOrEmpty()) suRes.out.joinToString(" ") else "Magisk / KernelSU / APatch"

            val seRes = withTimeoutOrNull(2000L) { Shell.cmd("getenforce").exec() }
            val selinux = if (seRes?.isSuccess == true && !seRes.out.isNullOrEmpty()) seRes.out.joinToString(" ") else "Enforcing"

            val whichSu = withTimeoutOrNull(2000L) { Shell.cmd("which su").exec() }
            val suPath = if (whichSu?.isSuccess == true && !whichSu.out.isNullOrEmpty()) whichSu.out.joinToString(" ") else "/system/bin/su"

            val info = RootValidationInfo(
                isRootGranted = true,
                shellUid = uidStr,
                shellVersion = suVersion,
                selinuxMode = selinux,
                suBinaryPath = suPath
            )
            Pair(RootVerificationStatus.VERIFIED_ROOT, info)
        } catch (t: Throwable) {
            Log.e(TAG, "Error fetching root validation details", t)
            val info = RootValidationInfo(
                isRootGranted = true,
                shellUid = "uid=0(root)",
                shellVersion = "libsu",
                selinuxMode = "Enforcing",
                suBinaryPath = "/system/bin/su"
            )
            Pair(RootVerificationStatus.VERIFIED_ROOT, info)
        }
    }
}
