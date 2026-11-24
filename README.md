# CONCORD & A TO Z GAS — Android Inventory Management App

Offline-first Android inventory management application built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Room Database** for **CONCORD & A TO Z GAS (Industrial & LPG)**.

---

## 1. Key Features

### Role-Based PIN Authentication
- **Admin Login**: `Ragavan` (Initial PIN: `1725`) — Full access to Dashboard, Store Management, Vehicle Management, Reports, Product Master, Staff PINs, and Backup/Restore.
- **Staff Login**: Individual PIN-protected accounts for all 7 staff/vehicle drivers:
  1. `Nadeem` (`NADEEM`, initial PIN `1001`)
  2. `Fazalamin` (`FAZALAMIN`, initial PIN `1002`)
  3. `Fawad` (`FAWAD`, initial PIN `1003`)
  4. `Ragavan` (`RAGAVAN`, initial PIN `1004`)
  5. `Sivan` (`SIVAN`, initial PIN `1005`)
  6. `Jinu` (`JINU`, initial PIN `1006`)
  7. `SAJAN` (`SAJAN`, initial PIN `1007`)
- Staff can view their assigned vehicle stock, receive full cylinders from store, record customer sales, record empty cylinders from customers, return empty cylinders to store, and view daily closing balance. Staff cannot modify opening stock, delete transactions, view other staff records, or access Admin settings.

### Store & Vehicle Inventory Tracking
- **Store Management**: Opening stock, full/empty purchases, full/empty inward, full dispatch (to vehicle or direct), empty dispatch (to plant), empty received from vehicles, stock adjustments, and live Store Full/Empty balances.
- **Vehicle Management**: Separate daily tracking per vehicle (`Old Full`, `Full Received`, `Total Full`, `Customer Sales`, `Full Balance`, `Old Empty`, `Customer Empty Received`, `Empty Returned to Store`, `Empty Balance`) with automatic day-to-day carry-forward.
- **Negative Stock Guard**: Atomic Room transactions prevent negative balances in Store or Vehicles and display a clear warning dialog when stock is insufficient.

### Product Master (Pre-Seeded)
- **INDUSTRIAL GAS**: Oxygen, Acetylene, Argon, Argon Mix, CO2 20kg, CO2 30kg, Nitrogen
- **48 CFT SMALL**: Oxygen, Acetylene, Argon, Argon Mix, CO2 5kg, Nitrogen
- **COOKING GAS**: 11kg, 22kg, 44kg
- **MCG CUTTING GAS**: 22kg, 44kg

### Reports & Offline Backup
- **6 Reports**: Daily Store Report, Daily Vehicle Report, Monthly Sales Report, Product-Wise Sales, Staff-Wise Sales, and Full & Empty Stock Report.
- **PDF & Excel (.xlsx) Export**: Save directly via Android Storage Access Framework or share via FileProvider.
- **Offline Backup & Restore**: Export and restore the complete Room database as a JSON file.

---

## 2. Project Architecture

- `app/src/main/java/com/example/data/local/` — Room Entities, Converters, DAOs, and `ConcordDatabase`
- `app/src/main/java/com/example/data/model/` — Domain enums, UI summary models, and pure `StockCalculator`
- `app/src/main/java/com/example/data/repository/` — `InventoryRepository` with atomic Room `@Transaction` checks
- `app/src/main/java/com/example/export/` — Offline `ReportExporter` (PDF & OpenXML `.xlsx`) and `BackupManager` (JSON)
- `app/src/main/java/com/example/ui/viewmodel/` — `AppViewModel` (`StateFlow` reactive state)
- `app/src/main/java/com/example/ui/screens/` — Compose screens for Login, Admin Dashboard, Store, Vehicles, Staff Dashboard, Reports, and Admin Settings
- `app/src/main/java/com/example/ui/theme/` — Corporate Green & Red Material 3 theme and bundled fonts

---

## 3. Build & Installable APK Instructions

### Prerequisites
- **Android Studio** Ladybug / Meerkat or newer (JDK 11+, Android SDK Platform 36).
- `ANDROID_SDK_ROOT` configured (automatically configured in AI Studio; set automatically by Android Studio on local machines).

### Build via Android Studio
1. Open the project root folder in **Android Studio** and allow Gradle sync to complete.
2. To run on a connected Android phone (USB Debugging enabled): click **Run ▶**.
3. To generate an installable APK:
   - Go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
   - Locate the generated APK at `app/build/outputs/apk/debug/app-debug.apk`.
   - Copy `app-debug.apk` to the target Android phone and open it to install.

### Build via Command Line (Gradle)
```bash
# Run unit & Robolectric tests
gradle :app:testDebugUnitTest

# Build debug installable APK
gradle :app:assembleDebug

# Build release APK (with signing environment variables configured)
gradle :app:assembleRelease
```
The debug APK output is written to:
`app/build/outputs/apk/debug/app-debug.apk`
