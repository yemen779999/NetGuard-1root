import random
from typing import List, Optional
from app.database.sqlite_db import DatabaseManager
from app.security.root_manager import RootAccessManager
from app.network.scanner import NetworkScanner
from app.models.device import NetworkDevice, SavedMacAddress, MacHistoryRecord

class NetGuardState:
    """
    Central Controller and State Holder (ViewModel equivalent in Python)
    """
    def __init__(self, db: DatabaseManager, root_mgr: RootAccessManager, scanner: NetworkScanner):
        self.db = db
        self.root_mgr = root_mgr
        self.scanner = scanner
        self.current_interface = "wlan0"
        self.current_mac = "AA:BB:CC:11:22:33"
        self.current_ip = self.scanner.get_local_ip()
        self.scanned_devices: List[NetworkDevice] = []
        self.logs: List[str] = []

    def log(self, message: str):
        self.logs.append(message)
        print(f"[*] {message}")

    def generate_random_mac(self) -> str:
        # Generate locally administered unicast MAC (2nd least significant bit set to 1)
        first_byte = random.choice([0x02, 0x06, 0x0A, 0x0E])
        remaining = [random.randint(0x00, 0xFF) for _ in range(5)]
        mac_bytes = [first_byte] + remaining
        return ":".join(f"{b:02X}" for b in mac_bytes)

    def apply_mac(self, new_mac: str) -> tuple[bool, str]:
        old_mac = self.current_mac
        success, msg = self.root_mgr.set_interface_mac(self.current_interface, new_mac)
        if success:
            self.current_mac = new_mac.upper()
            self.db.add_history(new_mac, old_mac)
            self.log(f"تم تغيير الماك بنجاح من {old_mac} إلى {new_mac}")
        else:
            self.log(f"فشل تغيير الماك: {msg}")
        return success, msg

    def scan_network(self) -> List[NetworkDevice]:
        self.log("بدء مسح الشبكة واستخراج بيانات الأجهزة المكتشفة...")
        self.scanned_devices = self.scanner.scan_network()
        self.log(f"اكتمل الفحص! تم العثور على {len(self.scanned_devices)} جهاز.")
        return self.scanned_devices

    def toggle_block(self, ip: str) -> bool:
        for dev in self.scanned_devices:
            if dev.ip == ip:
                if dev.is_blocked:
                    self.scanner.unblock_device(ip)
                    dev.is_blocked = False
                    self.log(f"تم رفع الحظر عن الجهاز: {ip}")
                else:
                    self.scanner.block_device(ip)
                    dev.is_blocked = True
                    self.log(f"تم حظر الجهاز وقطع النت عبر NetCut: {ip}")
                return dev.is_blocked
        return False
