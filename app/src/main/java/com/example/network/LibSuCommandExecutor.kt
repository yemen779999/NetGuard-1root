package com.example.network

import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper class using libsu to execute root commands safely, handle output streams efficiently,
 * and perform network configuration commands like MAC address randomization and IP/iptables flushing.
 */
object LibSuCommandExecutor {
    private const val TAG = "LibSuCommandExecutor"

    /**
     * Checks su binary availability using libsu.
     */
    suspend fun checkSuAvailability(): Boolean = withContext(Dispatchers.IO) {
        try {
            val isRootGranted = RootServices.isRootAvailable()
            if (isRootGranted) {
                val res = Shell.cmd("id").exec()
                res.isSuccess && res.out.any { it.contains("uid=0") }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking su availability", e)
            false
        }
    }

    /**
     * Safe execution wrapper for a single root command.
     */
    suspend fun executeCommand(command: String): ShellCommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val isRoot = RootServices.isRootAvailable()

        if (!isRoot) {
            return@withContext ShellCommandResult(
                command = command,
                isSuccess = false,
                code = -1,
                stdout = emptyList(),
                stderr = listOf("رفض الصلاحية: يلزم وجود الروت (Root Access Required) لتنفيذ هذا الأمر."),
                timestamp = System.currentTimeMillis(),
                durationMs = System.currentTimeMillis() - startTime,
                isSimulated = true
            )
        }

        try {
            val result = Shell.cmd(command).exec()
            val endTime = System.currentTimeMillis()
            ShellCommandResult(
                command = command,
                isSuccess = result.isSuccess,
                code = result.code,
                stdout = result.out ?: emptyList(),
                stderr = result.err ?: emptyList(),
                timestamp = System.currentTimeMillis(),
                durationMs = endTime - startTime,
                isSimulated = false
            )
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            ShellCommandResult(
                command = command,
                isSuccess = false,
                code = -1,
                stdout = emptyList(),
                stderr = listOf(e.localizedMessage ?: "فشل تنفيذ الأمر عبر libsu"),
                timestamp = System.currentTimeMillis(),
                durationMs = endTime - startTime,
                isSimulated = false
            )
        }
    }

    /**
     * Executes a batch of commands safely via libsu.
     */
    suspend fun executeBatch(commands: List<String>): ShellCommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val isRoot = RootServices.isRootAvailable()

        if (!isRoot) {
            return@withContext ShellCommandResult(
                command = commands.joinToString(" && "),
                isSuccess = false,
                code = -1,
                stdout = emptyList(),
                stderr = listOf("رفض الصلاحية: لا توجد صلاحيات روت لتنفيذ الدفعة."),
                timestamp = System.currentTimeMillis(),
                durationMs = System.currentTimeMillis() - startTime,
                isSimulated = true
            )
        }

        try {
            val result = Shell.cmd(*commands.toTypedArray()).exec()
            val endTime = System.currentTimeMillis()
            ShellCommandResult(
                command = commands.joinToString(" ; "),
                isSuccess = result.isSuccess,
                code = result.code,
                stdout = result.out ?: emptyList(),
                stderr = result.err ?: emptyList(),
                timestamp = System.currentTimeMillis(),
                durationMs = endTime - startTime,
                isSimulated = false
            )
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            ShellCommandResult(
                command = commands.joinToString(" ; "),
                isSuccess = false,
                code = -1,
                stdout = emptyList(),
                stderr = listOf(e.localizedMessage ?: "فشل تنفيذ حزمة الأوامر عبر libsu"),
                timestamp = System.currentTimeMillis(),
                durationMs = endTime - startTime,
                isSimulated = false
            )
        }
    }

    /**
     * Performs MAC address randomization via shell commands.
     * Ensures this action is triggered ONLY if root access is granted.
     */
    suspend fun randomizeMacAddress(iface: String = "wlan0"): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!RootServices.isRootAvailable()) {
            return@withContext Pair(false, "لا تتوفر صلاحيات الروت (Root Access Required) لتغيير عنوان MAC عشوائياً.")
        }

        // Generate locally administered unicast MAC address (second digit 2, 6, A, or E)
        val hexChars = "0123456789ABCDEF"
        val firstByteSecondDigit = listOf('2', '6', 'A', 'E').random()
        val randomMac = StringBuilder()
            .append("0").append(firstByteSecondDigit)
            .append(":")
            .append((1..5).joinToString(":") {
                "${hexChars.random()}${hexChars.random()}"
            })
            .toString()

        val commands = listOf(
            "ip link set $iface down",
            "ip link set $iface address $randomMac",
            "ip link set $iface up",
            "settings put global wifi_connected_mac_randomization_enabled 1"
        )

        val result = executeBatch(commands)
        if (result.isSuccess) {
            Pair(true, randomMac)
        } else {
            Pair(false, result.stderr.firstOrNull() ?: "فشل تغيير عنوان MAC عشوائياً عبر ip link")
        }
    }

    /**
     * Performs IP and iptables flushing via root command wrapper.
     * Clears all iptables rules (FORWARD, INPUT, OUTPUT, NAT, MANGLE) and flushes neighbor cache.
     * Ensures this action is triggered ONLY if root access is granted.
     */
    suspend fun flushIpAndIptables(iface: String = "wlan0"): ShellCommandResult = withContext(Dispatchers.IO) {
        if (!RootServices.isRootAvailable()) {
            return@withContext ShellCommandResult(
                command = "iptables flush & ip neigh flush",
                isSuccess = false,
                code = -1,
                stdout = emptyList(),
                stderr = listOf("تنبيه: يتطلب مسح جداول IP و iptables وجود صلاحيات الروت."),
                timestamp = System.currentTimeMillis(),
                durationMs = 0L,
                isSimulated = true
            )
        }

        val commands = listOf(
            "iptables -F",
            "iptables -X",
            "iptables -t nat -F",
            "iptables -t nat -X",
            "iptables -t mangle -F",
            "iptables -t mangle -X",
            "ip neigh flush all",
            "ip route flush cache"
        )

        executeBatch(commands)
    }
}
