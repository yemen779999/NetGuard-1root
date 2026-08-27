import os

class AppConfig:
    def __init__(self):
        self.app_name = "NetGuard Root"
        self.version = "1.0.0"
        self.data_dir = os.path.expanduser("~/.netguard")
        os.makedirs(self.data_dir, exist_ok=True)
        self.db_path = os.path.join(self.data_dir, "netguard.db")
        self.default_interface = "wlan0"
        self.enable_3d_effects = True
        self.animation_level = "Full" # Full, Reduced, Off
        self.language = "ar" # ar, en
