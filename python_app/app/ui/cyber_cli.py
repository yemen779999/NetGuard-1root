import sys
from app.core.state import NetGuardState

class CyberTerminalUI:
    def __init__(self, state: NetGuardState):
        self.state = state

    def display_menu(self):
        print("\n" + "=" * 55)
        print("⚡ NETGUARD ROOT - لوحة التحكم الرئيسية")
        print("=" * 55)
        print("1. 📡 فحص الأجهزة المتصلة بالشبكة (Network Radar)")
        print("2. 🎲 توليد وتطبيق عنوان MAC عشوائي (MAC Spoofer)")
        print("3. 📑 عرض عناوين MAC المحفوظة (Saved MACs)")
        print("4. 💾 حفظ عنوان MAC جديد")
        print("5. 🛑 قطع النت عن جهاز (NetCut Toggle)")
        print("6. 📜 سجل تغييرات MAC (History)")
        print("7. 📦 تصدير نسخة احتياطية (Full JSON Backup)")
        print("0. 🚪 خروج")
        print("-" * 55)

    def run(self):
        while True:
            self.display_menu()
            choice = input("اختر عملية [0-7]: ").strip()

            if choice == "1":
                devices = self.state.scan_network()
                print("\n🎯 الأجهزة المكتشفة:")
                print(f"{'IP Address':<18} {'MAC Address':<20} {'الشركة / النوع':<20} {'الحالة'}")
                print("-" * 65)
                for d in devices:
                    status = "🛑 مقطوع" if d.is_blocked else "🟢 متصل"
                    print(f"{d.ip:<18} {d.mac:<20} {d.vendor:<20} {status}")

            elif choice == "2":
                rand_mac = self.state.generate_random_mac()
                print(f"\n[+] الماك المولد: {rand_mac}")
                confirm = input("تطبيق على واجهة الشبكة؟ (y/n): ").lower()
                if confirm in ['y', 'yes', 'نعم']:
                    success, msg = self.state.apply_mac(rand_mac)
                    print(f"النتيجة: {msg}")

            elif choice == "3":
                saved = self.state.db.get_saved_macs()
                print(f"\n📑 العناوين المحفوظة ({len(saved)}):")
                for s in saved:
                    print(f"#{s.id} - {s.title}: {s.mac_address}")

            elif choice == "4":
                title = input("العنوان/الاسم: ")
                mac = input("عنوان MAC: ")
                if self.state.root_mgr.is_valid_mac(mac):
                    self.state.db.save_mac(title, mac)
                    print("✅ تم الحفظ بنجاح!")
                else:
                    print("❌ صيغة MAC غير صحيحة!")

            elif choice == "5":
                ip = input("أدخل IP الجهاز لحظر/فك حظر الاتصال: ")
                state = self.state.toggle_block(ip)
                print(f"حالة الجهاز الآن: {'مقطوع' if state else 'متصل'}")

            elif choice == "6":
                history = self.state.db.get_history()
                print(f"\n📜 سجل التغييرات ({len(history)}):")
                for h in history:
                    print(f"من: {h.old_mac}  -->  إلى: {h.new_mac}")

            elif choice == "7":
                json_data = self.state.db.export_backup_json()
                print("\n📦 بيانات النسخة الاحتياطية:")
                print(json_data)

            elif choice == "0":
                print("وداعاً!")
                break
