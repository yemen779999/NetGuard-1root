import subprocess
import re
import os
from typing import Tuple, List

class RootAccessManager:
    """
    Root / Shell Command Engine for NetGuard
    Handles:
    - Root detection (su binary presence and execution test)
    - Strict input sanitization against shell command injection
    - MAC address format validation
    - Direct root execution & Simulation fallback
    """
    def __init__(self):
        self.is_root_available = self._check_root()

    def _check_root(self) -> bool:
        # Check standard Linux root / Android su
        if os.geteuid() == 0:
            return True
        try:
            res = subprocess.run(["which", "su"], stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=2)
            if res.returncode == 0:
                # Test su capability
                test = subprocess.run(["su", "-c", "id"], stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=2)
                return test.returncode == 0
        except Exception:
            pass
        return False

    @staticmethod
    def is_valid_mac(mac: str) -> bool:
        pattern = r"^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"
        return bool(re.match(pattern, mac.strip()))

    @staticmethod
    def is_valid_ip(ip: str) -> bool:
        pattern = r"^((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}$"
        return bool(re.match(pattern, ip.strip()))

    def execute_command(self, cmd: str, use_root: bool = False) -> Tuple[bool, List[str]]:
        """
        Executes a sanitized command. If root is unavailable, runs standard or simulation mode.
        """
        try:
            if use_root and self.is_root_available:
                process = subprocess.run(["su", "-c", cmd], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=10)
            else:
                process = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=10)

            output = process.stdout.splitlines()
            if process.returncode != 0:
                output.extend(process.stderr.splitlines())
            return (process.returncode == 0, output)
        except Exception as e:
            return (False, [f"Execution Error: {str(e)}"])

    def set_interface_mac(self, interface: str, new_mac: str) -> Tuple[bool, str]:
        if not self.is_valid_mac(new_mac):
            return (False, "صيغة عنوان MAC غير صالحة!")
        
        # Sanitize interface name (allow only alphanumeric)
        if not re.match(r"^[a-zA-Z0-9_]+$", interface):
            return (False, "اسم واجهة الشبكة غير صالح!")

        if not self.is_root_available:
            return (True, f"[محاكاة] تم تغيير الماك للواجهة {interface} إلى {new_mac} بنجاح (Simulation Mode)")

        cmd = f"ip link set dev {interface} down && ip link set dev {interface} address {new_mac} && ip link set dev {interface} up"
        success, out = self.execute_command(cmd, use_root=True)
        if success:
            return (True, f"تم تغيير MAC بنجاح إلى {new_mac}")
        else:
            return (False, f"فشل التغيير: {' '.join(out)}")
