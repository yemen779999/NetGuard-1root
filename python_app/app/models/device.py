from dataclasses import dataclass, field
import time

@dataclass
class NetworkDevice:
    ip: str
    mac: str
    name: str = "جهاز غير معروف"
    vendor: str = "Unknown"
    device_type: str = "جهاز"
    hostname: str = "Unknown"
    is_blocked: bool = False
    timestamp: float = field(default_factory=time.time)

@dataclass
class SavedMacAddress:
    id: int
    title: str
    mac_address: str
    timestamp: float = field(default_factory=time.time)

@dataclass
class MacHistoryRecord:
    id: int
    new_mac: str
    old_mac: str
    timestamp: float = field(default_factory=time.time)
