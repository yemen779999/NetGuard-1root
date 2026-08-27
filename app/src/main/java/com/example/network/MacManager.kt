package com.example.network

import android.util.Log
import com.topjohnwu.superuser.Shell
import java.util.Locale

/**
 * MacManager object handling MAC address retrieval via 'ip link show wlan0',
 * verifying root permissions via Shell.rootAccess(), and managing MAC address spoofing.
 */
object MacManager {
    private const val TAG = "MacManager"

    /**
     * Checks if root access is granted using Shell.rootAccess().
     * Throws a SecurityException if root access is not available.
     */
    @Throws(SecurityException::class)
    fun checkRootAccessOrThrow() {
        val isRoot = isRootGranted()
        if (!isRoot) {
            throw SecurityException("رفض الصلاحية: صلاحيات الروت غير متاحة (Root Access Required). يلزم وجود الروت لتغيير أو إدارة عنوان MAC.")
        }
    }

    /**
     * Verifies if root permissions are granted using Shell.rootAccess().
     * Returns true if root is granted, false otherwise.
     */
    fun isRootGranted(): Boolean {
        return RootServices.isRootAvailable()
    }

    /**
     * Parses the MAC address from the command output of 'ip link show wlan0' or similar interface.
     */
    fun parseMacFromIpLinkOutput(output: List<String>): String? {
        val macRegex = Regex("(?i)([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}")
        // 1. Look specifically for the line with link/ether or ether
        for (line in output) {
            if (line.contains("link/ether", ignoreCase = true) || line.contains("ether", ignoreCase = true)) {
                val match = macRegex.find(line)
                if (match != null) {
                    return match.value.uppercase(Locale.ROOT)
                }
            }
        }
        // 2. Fallback search across all lines excluding broadcast ff:ff:ff:ff:ff:ff
        for (line in output) {
            val match = macRegex.find(line)
            if (match != null && !match.value.equals("ff:ff:ff:ff:ff:ff", ignoreCase = true)) {
                return match.value.uppercase(Locale.ROOT)
            }
        }
        return null
    }

    /**
     * Displays the current MAC address of the device by parsing the output of 'ip link show <iface>'.
     * Returns the parsed MAC address string or falls back to hardware/sysfs MAC if parsing fails.
     */
    fun getCurrentMacAddress(iface: String = "wlan0"): String {
        try {
            val cmdResult = Shell.cmd("ip link show $iface").exec()
            if (cmdResult.isSuccess && !cmdResult.out.isNullOrEmpty()) {
                val parsedMac = parseMacFromIpLinkOutput(cmdResult.out)
                if (parsedMac != null) {
                    return parsedMac
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error running ip link show $iface", e)
        }
        // Fallback to cat /sys/class/net/$iface/address or Hardware MAC
        return RootServices.getCurrentMacAddress(iface)
    }

    /**
     * Spoofs the device MAC address. Checks root access first via Shell.rootAccess().
     * If root access is not available, returns false with an error message.
     */
    suspend fun setMacAddress(
        iface: String = "wlan0",
        newMac: String,
        logsList: MutableList<String> = mutableListOf()
    ): Pair<Boolean, String> {
        if (!isRootGranted()) {
            val errMsg = "رفض الصلاحية: صلاحيات الروت غير متاحة (Root Access Required via Shell.rootAccess())."
            logsList.add(errMsg)
            return Pair(false, errMsg)
        }
        val success = RootServices.changeMacAddress(iface, newMac, logsList)
        return if (success) {
            Pair(true, "تم تغيير MAC إلى $newMac بنجاح.")
        } else {
            Pair(false, "فشل تغيير عنوان MAC.")
        }
    }

    /**
     * Randomizes the MAC address. Verifies root permissions using Shell.rootAccess().
     */
    suspend fun randomizeMacAddress(
        iface: String = "wlan0",
        logsList: MutableList<String> = mutableListOf()
    ): Pair<Boolean, String> {
        if (!isRootGranted()) {
            val errMsg = "رفض الصلاحية: يلزم وجود صلاحيات الروت (Shell.rootAccess() = false) لتوليد MAC عشوائي."
            logsList.add(errMsg)
            return Pair(false, errMsg)
        }
        val randomMac = RootServices.generateRandomMacAddress()
        val success = RootServices.changeMacAddress(iface, randomMac, logsList)
        return if (success) {
            Pair(true, randomMac)
        } else {
            Pair(false, "فشل تطبيق عنوان MAC العشوائي.")
        }
    }
}
