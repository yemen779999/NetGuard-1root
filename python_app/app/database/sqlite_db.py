import sqlite3
import json
import time
from typing import List, Optional
from app.models.device import SavedMacAddress, MacHistoryRecord, NetworkDevice

class DatabaseManager:
    """
    Room Database Equivalent for Python (SQLite Engine)
    Supports:
    - saved_macs
    - mac_history
    - config_change_logs
    - network_profiles
    - scanned_devices
    - full JSON backup and restore
    """
    def __init__(self, db_path: str):
        self.db_path = db_path
        self._init_db()

    def _get_connection(self):
        return sqlite3.connect(self.db_path)

    def _init_db(self):
        with self._get_connection() as conn:
            cursor = conn.cursor()
            
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS saved_macs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                macAddress TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """)

            cursor.execute("""
            CREATE TABLE IF NOT EXISTS mac_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                macAddress TEXT NOT NULL,
                oldMacAddress TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """)

            cursor.execute("""
            CREATE TABLE IF NOT EXISTS scanned_devices (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ip TEXT NOT NULL,
                mac TEXT NOT NULL,
                deviceName TEXT,
                deviceType TEXT,
                vendor TEXT,
                hostname TEXT,
                isBlocked INTEGER DEFAULT 0,
                timestamp INTEGER NOT NULL
            )
            """)

            cursor.execute("""
            CREATE TABLE IF NOT EXISTS config_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                action TEXT NOT NULL,
                details TEXT,
                timestamp INTEGER NOT NULL
            )
            """)
            conn.commit()

    def save_mac(self, title: str, mac_address: str) -> int:
        with self._get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(
                "INSERT INTO saved_macs (title, macAddress, timestamp) VALUES (?, ?, ?)",
                (title, mac_address.upper(), int(time.time() * 1000))
            )
            conn.commit()
            return cursor.lastrowid

    def get_saved_macs(self) -> List[SavedMacAddress]:
        with self._get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT id, title, macAddress, timestamp FROM saved_macs ORDER BY id DESC")
            rows = cursor.fetchall()
            return [SavedMacAddress(id=r[0], title=r[1], mac_address=r[2], timestamp=r[3]) for r in rows]

    def delete_saved_mac(self, mac_id: int):
        with self._get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("DELETE FROM saved_macs WHERE id = ?", (mac_id,))
            conn.commit()

    def add_history(self, new_mac: str, old_mac: str):
        with self._get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(
                "INSERT INTO mac_history (macAddress, oldMacAddress, timestamp) VALUES (?, ?, ?)",
                (new_mac.upper(), old_mac.upper(), int(time.time() * 1000))
            )
            conn.commit()

    def get_history(self) -> List[MacHistoryRecord]:
        with self._get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT id, macAddress, oldMacAddress, timestamp FROM mac_history ORDER BY id DESC")
            rows = cursor.fetchall()
            return [MacHistoryRecord(id=r[0], new_mac=r[1], old_mac=r[2], timestamp=r[3]) for r in rows]

    def clear_history(self):
        with self._get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("DELETE FROM mac_history")
            conn.commit()

    def export_backup_json(self) -> str:
        saved_macs = self.get_saved_macs()
        history = self.get_history()
        data = {
            "type": "NETGUARD_FULL_BACKUP",
            "version": 1,
            "timestamp": int(time.time() * 1000),
            "saved_macs": [{"title": m.title, "macAddress": m.mac_address, "timestamp": m.timestamp} for m in saved_macs],
            "mac_history": [{"macAddress": h.new_mac, "oldMacAddress": h.old_mac, "timestamp": h.timestamp} for h in history]
        }
        return json.dumps(data, indent=4, ensure_ascii=False)

    def import_backup_json(self, json_content: str) -> bool:
        try:
            data = json.loads(json_content)
            if "saved_macs" in data:
                for item in data["saved_macs"]:
                    self.save_mac(item.get("title", "مستورد"), item.get("macAddress", ""))
            return True
        except Exception:
            return False
