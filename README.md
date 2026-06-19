# ZeroCache - 16

Cache & Size Analyzer untuk Android. Membersihkan cache aplikasi satu ketukan dengan dukungan No-Root (AccessibilityService) dan Root (pm clear).

## Stack

- **Bahasa:** Kotlin 2.1.0
- **UI:** Jetpack Compose (BOM 2024.12.01) + Material 3
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 35 (Android 15)
- **Build:** Gradle 8.11.1 + AGP 8.7.3

## Fitur

1. **Dashboard tunggal** dengan ringkasan total cache dan jumlah aplikasi.
2. **List aplikasi** disortir dari cache terbesar ke terkecil.
3. **Tombol Hapus Semua Cache** sebagai primary action.
4. **Mode No-Root:** membuka halaman App Info lalu AccessibilityService men-tap tombol Hapus Cache.
5. **Mode Root:** shell `su -c rm -rf /data/data/<pkg>/cache` + `su -c pm clear` (saat di-root).
6. **Toggle bahasa Indonesia / Inggris** di kanan atas berupa icon bendera.
7. **Locale persistence** lewat SharedPreferences (tidak bergantung pada system locale).

## Arsitektur

```
com.zerocache/
├── MainActivity.kt                  # Entry point + in-app locale switcher
├── data/
│   ├── AppCacheInfo.kt              # Domain model
│   ├── AppCacheScanner.kt           # PackageManager + filesystem scan
│   └── ClearEngine.kt               # Strategy executor (Root / NoRoot)
├── service/
│   └── ZeroCacheAccessibilityService.kt  # No-Root auto-tap
├── ui/
│   ├── DashboardViewModel.kt        # StateFlow state holder
│   ├── theme/                       # Color / Type / Theme (Notion-inspired)
│   └── screen/
│       └── DashboardScreen.kt       # Single-screen Compose UI
└── util/
    ├── LocaleManager.kt             # In-app language switcher
    ├── SizeFormatter.kt             # Bytes → KB/MB/GB
    └── SystemUtils.kt               # RootChecker + SettingsOpener
```

## Bahasa

- **Default:** Bahasa Indonesia (`values/strings.xml`)
- **English:** `values-en/strings.xml`
- **In-app toggle:** `LocaleManager` membungkus `Context` dengan `Configuration.setLocale`.

## Permissions

| Permission | Alasan |
|---|---|
| `QUERY_ALL_PACKAGES` | Enumerasi aplikasi terinstal |
| `DELETE_CACHE_FILES` | (signature) Untuk penghapusan cache sistem |
| `PACKAGE_USAGE_STATS` | Baca ukuran cache via StorageStatsManager |
| `BIND_ACCESSIBILITY_SERVICE` | Mode No-Root auto-tap |

## Design Reference

Palette dan typography diadaptasi dari studi design di `~/Documents/DATA +/design/ALL/notion.md` (Notion-clean editorial palette: indigo primary, cream canvas, dark navy untuk emphasis).

## Build

```bash
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

## Status

- [x] Gradle setup + manifest + permissions
- [x] Custom logo (vector drawable adaptive icon)
- [x] Theme (Color, Type, Theme.kt) - Notion palette
- [x] AppCacheScanner (PackageManager + filesystem)
- [x] ClearEngine (Root + NoRoot strategies)
- [x] ZeroCacheAccessibilityService (auto-tap)
- [x] DashboardViewModel + DashboardScreen Compose UI
- [x] i18n (ID/EN) + flag toggle + LocaleManager
- [x] Build APK debug - BUILD SUCCESSFUL
- [x] Lint - PASSED

## Catatan

- Tidak di-deploy (sesuai instruksi).
- Untuk testing di device, install `app-debug.apk` lewat ADB atau sideload.
- Mode Root butuh device yang sudah di-root + `su` binary di PATH.
- Mode No-Root butuh user mengaktifkan ZeroCache Auto-Clear di Setelan → Aksesibilitas.
