import unittest
import os
import tempfile
from app.database.sqlite_db import DatabaseManager
from app.security.root_manager import RootAccessManager
from app.network.scanner import NetworkScanner
from app.core.state import NetGuardState

class TestNetGuardPython(unittest.TestCase):
    def setUp(self):
        self.temp_db = tempfile.NamedTemporaryFile(delete=False)
        self.db = DatabaseManager(self.temp_db.name)
        self.root_mgr = RootAccessManager()
        self.scanner = NetworkScanner(self.db, self.root_mgr)
        self.state = NetGuardState(self.db, self.root_mgr, self.scanner)

    def tearDown(self):
        try:
            os.remove(self.temp_db.name)
        except Exception:
            pass

    def test_mac_validation(self):
        self.assertTrue(self.root_mgr.is_valid_mac("00:1A:2B:3C:4D:5E"))
        self.assertTrue(self.root_mgr.is_valid_mac("aa-bb-cc-dd-ee-ff"))
        self.assertFalse(self.root_mgr.is_valid_mac("00:1A:2B:3C:4D"))
        self.assertFalse(self.root_mgr.is_valid_mac("INVALID_MAC"))

    def test_random_mac_generation(self):
        mac = self.state.generate_random_mac()
        self.assertTrue(self.root_mgr.is_valid_mac(mac))

    def test_database_crud(self):
        mac_id = self.db.save_mac("Test Device", "00:11:22:33:44:55")
        saved = self.db.get_saved_macs()
        self.assertEqual(len(saved), 1)
        self.assertEqual(saved[0].title, "Test Device")

        self.db.add_history("00:11:22:33:44:55", "AA:BB:CC:DD:EE:FF")
        history = self.db.get_history()
        self.assertEqual(len(history), 1)

    def test_network_scan_fallback(self):
        devices = self.scanner.scan_network()
        self.assertTrue(len(devices) > 0)
        self.assertEqual(devices[0].device_type, "راوتر")

if __name__ == "__main__":
    unittest.main()
