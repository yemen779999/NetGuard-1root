"""
NetGuard Root - Python Edition
High-Performance Network Security & Identity Management Architecture
"""

import sys
import os

# Ensure app package is in path
sys.path.insert(0, os.path.abspath(os.path.dirname(__file__)))

from app.core.config import AppConfig
from app.core.state import NetGuardState
from app.database.sqlite_db import DatabaseManager
from app.network.scanner import NetworkScanner
from app.security.root_manager import RootAccessManager
from app.ui.cyber_cli import CyberTerminalUI

def main():
    print("=" * 60)
    print("⚡ NETGUARD ROOT - CYBER DEFENSE & NETWORK UTILITY (PYTHON CORE)")
    print("=" * 60)
    
    # Initialize Core Subsystems
    config = AppConfig()
    db = DatabaseManager(config.db_path)
    root_mgr = RootAccessManager()
    scanner = NetworkScanner(db, root_mgr)
    state = NetGuardState(db, root_mgr, scanner)

    # Launch Interactive Terminal Interface
    ui = CyberTerminalUI(state)
    ui.run()

if __name__ == "__main__":
    main()
