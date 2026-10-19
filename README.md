# ZScreenControl

<p align="center">
  <img src="art/logo.svg" width="112" alt="Logo ZScreenControl">
</p>

Aplikasi Android gratis tanpa iklan untuk mengontrol layar:
**timeout layar**, **sleep timer**, **jadwal otomatis**, dan **timeout per aplikasi**.

![ZScreenControl banner](art/banner.svg)

- Stack: Kotlin + Jetpack Compose (Material 3), Room, DataStore, AlarmManager
- minSdk 28 (Android 9) — termasuk **OPPO A5 2020 / ColorOS**
- targetSdk 36
- Versi saat ini: **0.2.0**

## Fitur v0.2.0

| Tab | Isi |
|---|---|
| Timeout Layar | Lihat & ubah timeout layar sistem, tombol "Matikan layar sekarang", pilihan metode (Otomatis/Admin/Aksesibilitas/Overlay), status izin lengkap + panduan ColorOS |
| Sleep Timer | Preset durasi, hitung mundur, +10 menit, berhenti; otomatis mematikan layar saat habis (bertahan walau proses dibekukan, ada alarm cadangan) |
| Jadwal | Daftar jadwal berulang (hari + jam) via Room + AlarmManager; bertahan setelah reboot; pengingat izin "Alarm & pengingat" |
| Aplikasi | **Timeout per aplikasi**: daftarkan app tertentu + durasi pemakaian; setelah batas habis layar dimatikan otomatis (via layanan aksesibilitas) |

Metode mematikan layar (fallback bertingkat saat pilihan "Otomatis"):

1. **Device Admin** → `lockNow()`
2. **Layanan Aksesibilitas** → `GLOBAL_ACTION_LOCK_SCREEN` (API 28+)
3. **Overlay hitam** → "screen-off palsu" (layar fisik tetap menyala, ketuk untuk keluar)

Ada juga:

- **Quick Settings Tile "Matikan Layar"** dan **tile "Sleep Timer"** (mulai/berhenti + sisa waktu)
- **Widget**: "Matikan Layar" (1 ketuk) dan "Sleep Timer" (sisa waktu + Mulai/Stop)

## Build APK lewat GitHub Actions (disarankan)

Workflow `Build APK` akan otomatis berjalan setiap push.

1. Buat repo baru di GitHub (public/private keduanya bisa).
2. Push isi folder ini ke repo tersebut. Cara termudah lewat Git:

   ```bash
   cd D:\dokumentasi\Project\ZScreenControl\android
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
5. Tambahkan tile & widget: geser Quick Settings → Edit → tambahkan **"Matikan Layar"** dan **"Sleep Timer"**; widget ditambahkan dari launcher (tekan lama layar beranda).
6. Untuk tab **Jadwal**: izinkan **"Alarm & pengingat"** (khusus API 31+) lewat kartu pengingat yang tampil di tab tersebut.
7. Untuk tab **Aplikasi**: aktifkan **Layanan Aksesibilitas** ZScreenControl (juga dipakai fitur kunci layar).

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

- **v0.3** — PIN lock (opsional), tema ringan, migrasi state timer ke DataStore (saat ini tetap SharedPreferences untuk akses sinkron servis), pengaturan lanjutan per app (jam malam, kuota harian).

## Struktur singkat

```
app/src/main/java/com/zscreencontrol/
├─ MainActivity.kt            # entry, 4 tab Compose
├─ ZScreenApp.kt              # channel notifikasi + init AppGraph
├─ core/                      # ScreenOffController, Permissions, Prefs, BatteryCompat, AppGraph
├─ data/                      # SettingsRepository (DataStore) + db/ Room (Schedule, AppTimeout)
├─ scheduler/                 # SleepScheduler (AlarmManager), fire/reboot receiver
├─ service/                   # SleepTimerService, AccessibilityService, BlackOverlayService
├─ deviceadmin/               # DeviceAdminReceiver (force-lock)
├─ tile/                      # QS Tile "Matikan Layar" + "Sleep Timer"
├─ widget/                    # Widget provider + updater (RemoteViews)
└─ ui/                        # theme + 4 layar
```
