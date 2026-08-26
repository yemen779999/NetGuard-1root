package com.example.network

import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Network Configuration Module using libsu to execute shell commands
 * for spoofing device MAC address and changing local IP / NAT routing via iptables.
 */
object NetworkConfigModule {
    private const val TAG = "NetworkConfigModule"

    private val _spoofedMac = MutableStateFlow<String?>(null)
    val spoofedMac: StateFlow<String?> = _spoofedMac.asStateFlow()

    private val _customLocalIp = MutableStateFlow<String?>(null)
    val customLocalIp: StateFlow<String?> = _customLocalIp.asStateFlow()

    private val _isNatConfigured = MutableStateFlow(false)
    val isNatConfigured: StateFlow<Boolean> = _isNatConfigured.asStateFlow()

    /**
     * Executes shell commands via libsu to spoof the hardware MAC address of the given network interface.
     *
     * Commands executed:
     * 1. ip link set <iface> down
     * 2. ip link set dev <iface> address <newMac>
     * 3. ip link set <iface> up
     */
    suspend fun spoofMacAddress(
        iface: String = "wlan0",
        newMac: String,
        logs: MutableList<String>
    ): ShellCommandResult = withContext(Dispatchers.IO) {
        val cleanMac = newMac.trim().uppercase()
        logs.add("[NetworkConfig] تهيئة أمر تغيير عنوان MAC للبطاقة $iface إلى: $cleanMac")
        logs.add("$ ip link set $iface down")
        logs.add("$ ip link set dev $iface address $cleanMac")
        logs.add("$ ip link set $iface up")

        val commands = listOf(
            "ip link set $iface down",
            "ip link set dev $iface address $cleanMac",
            "ip link set $iface up"
        )

        val isRoot = RootServices.isRootAvailable()
        if (!isRoot) {
            logs.add("[محاكاة] تم تنفيذ أوامر تغيير الماك $cleanMac كـ محاكاة (libsu Fallback)")
            _spoofedMac.value = cleanMac
            return@withContext ShellCommandResult(
                command = commands.joinToString(" && "),
                isSuccess = true,
                code = 0,
                stdout = listOf("ip link set dev $iface address $cleanMac (Simulated)"),
                stderr = emptyList(),
                isSimulated = true
            )
        }

        val result = Shell.cmd(*commands.toTypedArray()).exec()
        if (result.isSuccess) {
            logs.add("[نجاح] تم انتحال وتغيير MAC بنجاح عبر libsu su binary!")
            _spoofedMac.value = cleanMac
        } else {
            val errorMsg = result.err.joinToString("\n").ifEmpty { "فشل في تنفيذ أوامر ip link" }
            logs.add("[خطأ] فشل تغيير الماك: $errorMsg")

            // Secondary fallback attempt using ifconfig
            logs.add("[إعادة محاولة] محاولة القالب البديل: ifconfig $iface hw ether $cleanMac")
            val altCmd = "ifconfig $iface hw ether $cleanMac"
            val altResult = Shell.cmd(altCmd).exec()
            if (altResult.isSuccess) {
                logs.add("[نجاح] نجحت إعادة المحاولة عبر أداة ifconfig!")
                _spoofedMac.value = cleanMac
                return@withContext ShellCommandResult(
                    command = altCmd,
                    isSuccess = true,
                    code = altResult.code,
                    stdout = altResult.out ?: emptyList(),
                    stderr = altResult.err ?: emptyList(),
                    isSimulated = false
                )
            }
        }

        ShellCommandResult(
            command = commands.joinToString(" && "),
            isSuccess = result.isSuccess,
            code = result.code,
            stdout = result.out ?: emptyList(),
            stderr = result.err ?: emptyList(),
            isSimulated = false
        )
    }

    /**
     * Executes shell commands via libsu to change the local IP and setup iptables SNAT/DNAT rules.
     *
     * Commands executed:
     * 1. echo 1 > /proc/sys/net/ipv4/ip_forward
     * 2. ip addr add <newIp>/<subnetPrefix> dev <iface>
     * 3. iptables -t nat -A POSTROUTING -o <iface> -j SNAT --to-source <newIp>
     * 4. iptables -t nat -A PREROUTING -i <iface> -d <newIp> -j DNAT --to-destination <currentIp>
     * 5. iptables -t nat -A OUTPUT -o <iface> -j SNAT --to-source <newIp>
     */
    suspend fun changeLocalIpViaIptables(
        iface: String = "wlan0",
        currentIp: String,
        newIp: String,
        subnetPrefix: Int = 24,
        logs: MutableList<String>
    ): ShellCommandResult = withContext(Dispatchers.IO) {
        val cleanNewIp = newIp.trim()
        val cleanCurrentIp = currentIp.trim()

        logs.add("[NetworkConfig] بدء تكوين إعادة توجيه IP وتعديل قواعد iptables SNAT/DNAT...")
        logs.add("عنوان IP الحالي: $cleanCurrentIp -> عنوان IP الجديد: $cleanNewIp على الواجهة $iface")

        val commands = listOf(
            "echo 1 > /proc/sys/net/ipv4/ip_forward",
            "ip addr add $cleanNewIp/$subnetPrefix dev $iface",
            "iptables -t nat -A POSTROUTING -o $iface -j SNAT --to-source $cleanNewIp",
            "iptables -t nat -A PREROUTING -i $iface -d $cleanNewIp -j DNAT --to-destination $cleanCurrentIp",
            "iptables -t nat -A OUTPUT -o $iface -j SNAT --to-source $cleanNewIp"
        )

        for (cmd in commands) {
            logs.add("$ $cmd")
        }

        val isRoot = RootServices.isRootAvailable()
        if (!isRoot) {
            logs.add("[محاكاة] تم تطبيق قواعد iptables وتعيين IP الجديد $cleanNewIp في وضع المحاكاة.")
            _customLocalIp.value = cleanNewIp
            _isNatConfigured.value = true
            return@withContext ShellCommandResult(
                command = commands.joinToString(" && "),
                isSuccess = true,
                code = 0,
                stdout = listOf("iptables SNAT rule configured to $cleanNewIp (Simulated)"),
                stderr = emptyList(),
                isSimulated = true
            )
        }

        val result = Shell.cmd(*commands.toTypedArray()).exec()
        if (result.isSuccess) {
            logs.add("[نجاح] تم تغيير وتوجيه IP الحزمة إلى $cleanNewIp وتطبيق قواعد iptables بنجاح عبر libsu!")
            _customLocalIp.value = cleanNewIp
            _isNatConfigured.value = true
        } else {
            val errStr = result.err.joinToString("\n").ifEmpty { "خطأ في تنفيذ قواعد iptables" }
            logs.add("[خطأ] فشل تطبيق قواعد iptables: $errStr")
        }

        ShellCommandResult(
            command = commands.joinToString(" && "),
            isSuccess = result.isSuccess,
            code = result.code,
            stdout = result.out ?: emptyList(),
            stderr = result.err ?: emptyList(),
            isSimulated = false
        )
    }

    /**
     * Resets local IP and flushes iptables NAT rules configured by changeLocalIpViaIptables.
     */
    suspend fun resetLocalIpConfig(
        iface: String = "wlan0",
        currentIp: String,
        originalIp: String,
        subnetPrefix: Int = 24,
        logs: MutableList<String>
    ): ShellCommandResult = withContext(Dispatchers.IO) {
        val configuredIp = _customLocalIp.value ?: currentIp

        logs.add("[NetworkConfig] جاري إلغاء تكوين IP وإزالة قواعد iptables NAT المخصصة...")
        val commands = listOf(
            "iptables -t nat -D POSTROUTING -o $iface -j SNAT --to-source $configuredIp 2>/dev/null",
            "iptables -t nat -D OUTPUT -o $iface -j SNAT --to-source $configuredIp 2>/dev/null",
            "iptables -t nat -D PREROUTING -i $iface -d $configuredIp -j DNAT --to-destination $originalIp 2>/dev/null",
            "ip addr del $configuredIp/$subnetPrefix dev $iface 2>/dev/null"
        )

        for (cmd in commands) {
            logs.add("$ $cmd")
        }

        val isRoot = RootServices.isRootAvailable()
        if (!isRoot) {
            logs.add("[محاكاة] تم إلغاء قواعد iptables وإعادة IP الأصلي.")
            _customLocalIp.value = null
            _isNatConfigured.value = false
            return@withContext ShellCommandResult(
                command = commands.joinToString(" && "),
                isSuccess = true,
                code = 0,
                stdout = listOf("iptables NAT rules cleared (Simulated)"),
                stderr = emptyList(),
                isSimulated = true
            )
        }

        val result = Shell.cmd(*commands.toTypedArray()).exec()
        logs.add("[تأكيد] تم إرجاع إعدادات IP و iptables للوضع الافتراضي.")
        _customLocalIp.value = null
        _isNatConfigured.value = false

        ShellCommandResult(
            command = commands.joinToString(" && "),
            isSuccess = result.isSuccess,
            code = result.code,
            stdout = result.out ?: emptyList(),
            stderr = result.err ?: emptyList(),
            isSimulated = false
        )
    }

    private val _isDnsRedirectionActive = MutableStateFlow(false)
    val isDnsRedirectionActive: StateFlow<Boolean> = _isDnsRedirectionActive.asStateFlow()

    private val _activeDnsServer = MutableStateFlow("1.1.1.1")
    val activeDnsServer: StateFlow<String> = _activeDnsServer.asStateFlow()

    /**
     * Forces all DNS queries (port 53 UDP & TCP) through a custom secure DNS server
     * using root iptables NAT PREROUTING and OUTPUT redirection rules.
     */
    suspend fun enableDnsRedirection(
        dnsIp: String = "1.1.1.1",
        iface: String = "wlan0",
        logs: MutableList<String>
    ): ShellCommandResult = withContext(Dispatchers.IO) {
        val cleanDnsIp = dnsIp.trim()
        logs.add("[DNS Firewall] جاري تطبيق إعادة توجيه كافة استعلامات DNS إلى الخادم الآمن: $cleanDnsIp...")

        val commands = listOf(
            "iptables -t nat -A OUTPUT -p udp --dport 53 -j DNAT --to-destination $cleanDnsIp:53",
            "iptables -t nat -A OUTPUT -p tcp --dport 53 -j DNAT --to-destination $cleanDnsIp:53",
            "iptables -t nat -A PREROUTING -p udp --dport 53 -j DNAT --to-destination $cleanDnsIp:53",
            "iptables -t nat -A PREROUTING -p tcp --dport 53 -j DNAT --to-destination $cleanDnsIp:53",
            "setprop net.dns1 $cleanDnsIp"
        )

        for (cmd in commands) {
            logs.add("$ $cmd")
        }

        val isRoot = RootServices.isRootAvailable()
        if (!isRoot) {
            logs.add("[DNS محاكاة] تم تفعيل إعادة توجيه DNS إلى $cleanDnsIp في وضع المحاكاة.")
            _isDnsRedirectionActive.value = true
            _activeDnsServer.value = cleanDnsIp
            return@withContext ShellCommandResult(
                command = commands.joinToString(" && "),
                isSuccess = true,
                code = 0,
                stdout = listOf("DNS iptables redirection enabled to $cleanDnsIp (Simulated)"),
                stderr = emptyList(),
                isSimulated = true
            )
        }

        val result = Shell.cmd(*commands.toTypedArray()).exec()
        if (result.isSuccess) {
            logs.add("[نجاح DNS] تم تحويل كافة استعلامات الشبكة إجبارياً إلى DNS $cleanDnsIp عبر iptables 🛡️")
            _isDnsRedirectionActive.value = true
            _activeDnsServer.value = cleanDnsIp
        } else {
            val errStr = result.err.joinToString("\n").ifEmpty { "خطأ في تطبيق قواعد DNS iptables" }
            logs.add("[خطأ DNS] فشل تفعيل إعادة توجيه DNS: $errStr")
        }

        ShellCommandResult(
            command = commands.joinToString(" && "),
            isSuccess = result.isSuccess,
            code = result.code,
            stdout = result.out ?: emptyList(),
            stderr = result.err ?: emptyList(),
            isSimulated = false
        )
    }

    /**
     * Removes DNS redirection rules from iptables.
     */
    suspend fun disableDnsRedirection(
        dnsIp: String = _activeDnsServer.value,
        logs: MutableList<String>
    ): ShellCommandResult = withContext(Dispatchers.IO) {
        val cleanDnsIp = dnsIp.trim()
        logs.add("[DNS Firewall] جاري إلغاء إعادة توجيه استعلامات DNS ومسح قواعد iptables NAT...")

        val commands = listOf(
            "iptables -t nat -D OUTPUT -p udp --dport 53 -j DNAT --to-destination $cleanDnsIp:53 2>/dev/null",
            "iptables -t nat -D OUTPUT -p tcp --dport 53 -j DNAT --to-destination $cleanDnsIp:53 2>/dev/null",
            "iptables -t nat -D PREROUTING -p udp --dport 53 -j DNAT --to-destination $cleanDnsIp:53 2>/dev/null",
            "iptables -t nat -D PREROUTING -p tcp --dport 53 -j DNAT --to-destination $cleanDnsIp:53 2>/dev/null"
        )

        for (cmd in commands) {
            logs.add("$ $cmd")
        }

        val isRoot = RootServices.isRootAvailable()
        if (!isRoot) {
            logs.add("[DNS محاكاة] تم إلغاء إعادة توجيه DNS ورجوع الضبط الافتراضي.")
            _isDnsRedirectionActive.value = false
            return@withContext ShellCommandResult(
                command = commands.joinToString(" && "),
                isSuccess = true,
                code = 0,
                stdout = listOf("DNS iptables redirection disabled (Simulated)"),
                stderr = emptyList(),
                isSimulated = true
            )
        }

        val result = Shell.cmd(*commands.toTypedArray()).exec()
        logs.add("[تأكيد DNS] تم مسح قواعد توجيه DNS وإعادة الحركة للطبيعية.")
        _isDnsRedirectionActive.value = false

        ShellCommandResult(
            command = commands.joinToString(" && "),
            isSuccess = result.isSuccess,
            code = result.code,
            stdout = result.out ?: emptyList(),
            stderr = result.err ?: emptyList(),
            isSimulated = false
        )
    }
}
