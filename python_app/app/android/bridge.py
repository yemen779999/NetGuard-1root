class AndroidBridge:
    """
    Bridge interface between Python environment and Android Native OS APIs
    (PyJNIus / Android Native IPC / Termux API)
    """
    def __init__(self):
        self.is_android = False
        try:
            from jnius import autoclass
            self.PythonActivity = autoclass('org.kivy.android.PythonActivity')
            self.is_android = True
        except Exception:
            self.PythonActivity = None

    def get_wifi_mac_native(self) -> str:
        if not self.is_android:
            return "02:00:00:00:00:00"
        try:
            activity = self.PythonActivity.mActivity
            wifi_mgr = activity.getSystemService('wifi')
            info = wifi_mgr.getConnectionInfo()
            return str(info.getMacAddress())
        except Exception:
            return "02:00:00:00:00:00"
