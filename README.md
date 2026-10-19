# ZScreenControl

Aplikasi Android gratis tanpa iklan untuk mengontrol layar:
**timeout layar**, **sleep timer**, dan (menyusul) **jadwal otomatis**.

- Stack: Kotlin + Jetpack Compose (Material 3)
- minSdk 28 (Android 9) — termasuk **OPPO A5 2020 / ColorOS**
- targetSdk 36
- Versi saat ini: **0.1.0**

## Fitur v0.1.0

| Tab | Isi |
|---|---|
| Timeout Layar | Lihat & ubah timeout layar sistem, tombol "Matikan layar sekarang", status izin lengkap + panduan ColorOS |
| Sleep Timer | Preset durasi, hitung mundur, +10 menit, berhenti; otomatis mematikan layar saat habis |
| Jadwal | Placeholder — direncanakan di v0.2 |

Metode mematikan layar (fallback bertingkat, otomatis memilih yang tersedia):

1. **Device Admin** → `lockNow()`
2. **Layanan Aksesibilitas** → `GLOBAL_ACTION_LOCK_SCREEN` (API 28+)
3. **Overlay hitam** → "screen-off palsu" (layar fisik tetap menyala, ketuk untuk keluar)

Ada juga **Quick Settings Tile "Matikan Layar"**.

## Build APK lewat GitHub Actions (disarankan)

Workflow `Build APK` akan otomatis berjalan setiap push.

1. Buat repo baru di GitHub (public/private keduanya bisa).
2. Push isi folder ini ke repo tersebut. Cara termudah lewat Git:

   ```bash
   cd D:\dokumentasi\Project\ZScreenControl\android
   git init
   git add .
   git commit -m "ZScreenControl v0.1.0"
   git branch -M main
   git remote add origin https://github.com/<USERNAME>/<NAMA-REPO>.git
   git push -u origin main
   ```

   > Bisa juga push seluruh folder `ZScreenControl` — workflow otomatis
   > mendeteksi project Gradle di sub-folder `android/`.

3. Buka repo → tab **Actions** → tunggu job **Build APK** selesai (hijau ✅).
4. Klik run tersebut → bagian **Artifacts** → unduh **ZScreenControl-debug-apk**.
5. Extract zip-nya, di dalamnya ada `app-debug.apk`.

## Instal di OPPO A5 2020

1. Copy `app-debug.apk` ke HP.
2. Buka file-nya → izinkan **"Install unknown apps"** untuk aplikasi File Manager-nya.
3. Setelah terpasang, buka ZScreenControl → beri izin satu per satu di tab **Timeout Layar**.
4. Khusus ColorOS, lakukan dua hal agar service tidak dibunuh:
   - **Optimasi baterai**: tab Timeout Layar → "Optimasi baterai" → pilih ZScreenControl → *Don't optimize*.
   - **Auto-start**: tombol "Auto-start (ColorOS)" → aktifkan ZScreenControl.
5. Tambahkan tile: geser Quick Settings → Edit → tambahkan **"Matikan Layar"**.

## Build lokal (opsional)

Butuh **Android Studio** terbaru (JDK 17+ sudah dibawa Android Studio):

1. Android Studio → *Open* → pilih folder ini.
2. Tunggu Gradle sync (unduh dependency sekali saja).
3. Run ▶ ke HP yang terhubung USB (aktifkan USB debugging di OPPO).

Atau lewat terminal: `gradlew.bat assembleDebug` (pastikan JAVA_HOME menunjuk JDK 17+).

## Rilis bertanda tangan (opsional)

1. Buat keystore (sekali saja):

   ```bash
   keytool -genkeypair -v -keystore zscreencontrol-release.keystore -alias zscreencontrol -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Base64-kan file keystore itu, lalu simpan sebagai **GitHub Secrets** di repo:
   - `RELEASE_KEYSTORE_BASE64` — isi base64 keystore
   - `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`
3. Push lagi → artifact **ZScreenControl-release-apk** akan muncul di samping debug.

Tanpa secrets tersebut, workflow tetap jalan normal dan hanya membuat APK debug.

## Catatan penting

- APK **debug** ditandatangani dengan debug key — cukup untuk pemakaian pribadi,
  tapi **jangan** dipakai untuk update dari APK release (beda tanda tangan).
- Simpan file keystore rilis sebaiknya **di luar repo**.
- Perubahan versi dilakukan di `app/build.gradle.kts` (`versionCode` / `versionName`).

## Roadmap

- **v0.2** — Jadwal otomatis (Room + AlarmManager), timeout per aplikasi (aksesibilitas),
  pilihan metode matikan layar, widget Glance, tile sleep timer.
- **v0.3** — Per-app timeout polish, PIN lock (opsional), pengaturan tema.

## Struktur singkat

```
app/src/main/java/com/zscreencontrol/
├─ MainActivity.kt            # entry, 3 tab Compose
├─ core/                      # ScreenOffController, Permissions, Prefs, BatteryCompat
├─ service/                   # SleepTimerService, Accessibility, BlackOverlay
├─ deviceadmin/               # DeviceAdminReceiver (force-lock)
├─ tile/                      # QS Tile
└─ ui/                        # theme + 3 layar
```
