# NetGuard Root - Project Guidelines & Rules

This project is **NetGuard Root**, a professional, engineering/security-styled offline-first network utility for Android 12+ that manages MAC addresses, monitors networks (NetCut), and applies system-level hardening via root permissions or demo simulation fallback.

## 🎨 Visual Identity & Styling
- **Theme**: Obsidian Minimalist Cyber Security Theme
- **Background**: `CyberDarkBg` (`#0A0C10`)
- **Surfaces**: `CyberSurface` (`#12161F`), `CyberSurfaceVariant` (`#1C2230`)
- **Key Accents**:
  - `CyberGreen` (`#C4FF60`) - Neon lime highlights, used for active, unblocked, primary statuses
  - `CyberRed` (`#FFFF5757`) - Deep security alert red, used for blocking network and device alerts
  - `CyberOrange` (`#FFFFB03A`) - Warning / standby simulation badges
- **Font**: Sans-Serif Display paired with strict Monospace text elements for telemetry indices.

## 🛠️ Tech Stack & Key Components
- **Framework**: modern Kotlin, Jetpack Compose with strict Material Design 3 (M3).
- **Persistence**: Room Database configured via KSP.
  - `saved_macs` table -> `SavedMac(id, title, macAddress, timestamp)`
  - `mac_history` table -> `MacHistory(id, macAddress, oldMacAddress, timestamp)`
- **Shell Commands**: Powered by `com.github.topjohnwu.libsu:core`.
- **Sensors & Real network**: Retrieves connection info from `WifiManager`/`NetworkInterface` dynamically.

## 🔒 Security Modes (Hybrid Action Engine)
- **Root Available Mode**: Directly runs active binary calls `ip link set`, `settings put global`, and ARP poisoning in real-time.
- **Simulation Mode**: Fallback automatically if root is absent. Runs full terminal emulation output on black dialog logs to guarantee 100% testability on standard development environments.

## 📁 Repository Map
- `/app/src/main/java/com/example/MainActivity.kt` - Main interactive dashboard and UI views
- `/app/src/main/java/com/example/data/Database.kt` - Room Database, entities, DAOs, and NetworkRepository
- `/app/src/main/java/com/example/network/RootServices.kt` - Raw system command integration, network scan, and blocking
- `/app/src/main/java/com/example/network/NetGuardViewModel.kt` - Central state machine, terminal log pipes, and navigation
