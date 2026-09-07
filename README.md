# AgriBicara

Asisten pertanian berbasis suara untuk petani kecil di Indonesia. Petani menekan
tombol mikrofon, bertanya dalam Bahasa Indonesia, dan menerima jawaban lisan yang
dihasilkan AI berdasarkan data cuaca hiperlokal BMKG.

Status saat ini: **Fase 8 — Launch Preparation** (berjalan). Fase 1, 2, 3, 5, 6,
dan 7 selesai dan ada di `master`. Lihat `.claude/PRPs/prds/agribicara.prd.md`
untuk peta fase.

**Deteksi penyakit tanaman TIDAK ada di aplikasi ini.** Fase 4 berstatus
`blocked`: spike 2026-08-30 membuktikan PlantVillage tidak memuat padi sama
sekali, dan model berbasis dataset itu turun ke ~31% saat diuji di luar dataset
latihnya. Diagnosis keliru yang terlihat yakin berisiko membuat petani membeli
pestisida yang salah. Bagian ini dihapus dari deskripsi di atas karena README
sebelumnya menjanjikannya seolah sudah ada.

Yang BELUM terbukti dan sengaja dicatat terbuka: notifikasi cuaca ekstrem belum
pernah muncul di layar, TalkBack belum pernah dinyalakan, dan aplikasi belum
pernah diuji di perangkat low-end sungguhan (Android 7 / RAM 2GB).

## Syarat Build

| Kebutuhan | Versi |
|---|---|
| JDK | 17 minimum. Dikembangkan dengan JBR bawaan Android Studio — per 2026-09-02 versinya **25.0.2**, bukan 21 seperti tertulis sebelumnya. CI memakai **JDK 21**, jadi keduanya memang berbeda dan itu tidak masalah selama keduanya ≥17 |
| Android SDK Platform | API 37 (`platforms;android-37.0`) |
| SDK Build Tools | 36.0.0+ |
| Gradle | 9.7.1 (via wrapper — tidak perlu instalasi terpisah) |

Android Gradle Plugin 9.x mewajibkan JDK 17 ke atas. JDK 11 atau lebih rendah
akan gagal total.

## Setup

1. Pasang Android Studio beserta Android SDK.
2. Buat `local.properties` di root proyek yang menunjuk ke SDK Anda:

   ```properties
   sdk.dir=C\:/Users/<nama-anda>/AppData/Local/Android/Sdk
   ```

   Perhatikan tanda `\` sebelum titik dua — di file `.properties`, titik dua
   setelah huruf drive harus di-escape (Android Lint akan menandainya jika tidak).

3. `local.properties` **tidak pernah di-commit**.

   Catatan koreksi: baris ini sebelumnya menyatakan `local.properties` akan
   menampung API key Gemini mulai Fase 5. **Itu tidak pernah terjadi.** Fase 5
   justru memakai Firebase AI Logic supaya tidak ada kunci Gemini di mana pun —
   kunci yang ditanam di APK bisa dibaca siapa saja yang membongkarnya. Satu-satunya
   rahasia lokal proyek ini sekarang adalah material penandatanganan rilis, dan
   itu tinggal di `keystore.properties`, bukan di sini.

4. **Daftarkan debug token App Check — wajib, sekali per perangkat uji.**

   Tanpa langkah ini fitur tanya-jawab AI **mati total** di build debug: App
   Check menolak setiap panggilan Gemini, dan layar hanya menampilkan pesan
   kegagalan. Build tetap sukses, seluruh test tetap hijau, dan tidak ada satu
   pun pemeriksaan otomatis yang bisa menangkapnya.

   ```bash
   adb logcat -d | grep DebugAppCheckProvider
   ```

   Salin UUID yang tercetak, lalu daftarkan di **Firebase Console → App Check →
   aplikasi Android → ⋮ → Manage debug tokens**.

   Token disimpan di `shared_prefs` aplikasi, jadi ia bertahan melewati
   `adb install -r` tetapi **hilang bila data aplikasi dibersihkan atau
   aplikasi di-uninstall** — saat itu terbitlah token baru yang harus
   didaftarkan lagi. Gejalanya menipu: fitur yang kemarin bekerja tiba-tiba
   mati tanpa ada kode yang berubah.

   Sejak perbaikan 2026-09-08, build debug menyebut penyebab ini terang-terangan
   di layar. Build rilis tidak, dan memang tidak boleh: petani tidak punya
   Firebase Console. Rilis memakai Play Integrity yang bekerja otomatis tanpa
   token apa pun — **petani tidak pernah mendaftarkan apa-apa.**

## Perintah

```bash
./gradlew assembleDebug            # build debug
./gradlew assembleRelease          # build rilis (R8 + resource shrinking)
./gradlew bundleRelease            # AAB untuk Play Store
./gradlew testDebugUnitTest        # unit test
./gradlew koverVerifyDebug         # ambang coverage 85% baris
./gradlew lintDebug                # analisis statis
```

`connectedDebugAndroidTest` sengaja **tidak** dicantumkan: ia gagal enam kali
berturut-turut di mesin pengembangan proyek ini selama Fase 6, tidak satu pun
karena kode. Jalankan instrumented test lewat `adb install` kedua APK lalu
`am instrument` per kelas — perintah lengkapnya ada di plan Fase 7 dan 8.

## Build Rilis

1. Buat keystore dan `keystore.properties` mengikuti `keystore.properties.example`.
2. Jalankan `./gradlew bundleRelease`.
3. Verifikasi tanda tangannya:

   ```bash
   export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
   "$JAVA_HOME/bin/keytool" -printcert -jarfile app/build/outputs/bundle/release/app-release.aab
   ```

Tanpa `keystore.properties`, `assembleRelease` dan `bundleRelease` **tetap
berjalan** dan menghasilkan artefak unsigned. Itu disengaja: CI membangun rilis
tanpa punya keystore sama sekali. Untuk memverifikasi APK (bukan AAB) pakai
`apksigner`, bukan `keytool` — APK ditandatangani skema v2 dan bukan JAR
signature, sehingga `keytool -printcert` tidak menampilkan apa pun.

Aset toko dan ikon legacy dibangkitkan dengan `java tools/StoreAssets.java`.

Langkah Play Console, beta test, dan hal-hal yang harus dikerjakan manusia ada di
`docs/play/release-checklist.md` — **belum dikerjakan**.

## Catatan Versi Penting

- **Kotlin dan KSP sengaja tidak dipin ke versi terbaru.** AGP 9.3.0 meng-embed
  KGP 2.2.10 dan KSP 2.2.10-2.0.2; proyek mengikuti pasangan itu karena keduanya
  adalah kombinasi yang diuji AGP. Lint akan memberi peringatan bahwa Kotlin
  2.4.10 dan KSP 2.3.11 tersedia — itu disengaja, jangan dinaikkan tanpa
  memverifikasi ulang seluruh rantai AGP/KGP/KSP.
- **AGP 9 memiliki built-in Kotlin.** Plugin `org.jetbrains.kotlin.android`
  TIDAK dipakai (dan akan menggagalkan build jika ditambahkan). Plugin
  `org.jetbrains.kotlin.plugin.compose` tetap diperlukan.
- `android.disallowKotlinSourceSets=false` di `gradle.properties` diperlukan
  karena KSP masih mendaftarkan source hasil generate lewat `kotlin.sourceSets`.
  Tinjau ulang ketika KSP merilis versi yang sadar built-in Kotlin.

## Arsitektur

Clean Architecture, satu module (`:app`):

```
com.agribicara.app
├── core/common/       NetworkResult, Constants, DispatcherProvider
├── di/                Modul Hilt (App, Database, Network)
├── data/local/        Room: AppDatabase, entity, DAO
└── presentation/      theme, navigation, onboarding, home
```

Konvensi yang berlaku untuk semua fase berikutnya:

- Repository mengembalikan `NetworkResult`, tidak melempar exception lintas layer.
- Pesan error selalu Bahasa Indonesia sederhana (langsung tampil ke petani).
- Logging lewat Timber; tanpa `Log.d`/`println`, tanpa data pribadi di rilis.
- Satu `<Screen>UiState` per layar, di-expose sebagai `StateFlow` tunggal.
- Tanpa nilai hardcoded: teks di `strings.xml`, ukuran di `Dimens`, sisanya di
  `Constants`.

## Aksesibilitas

Target pengguna adalah petani dengan literasi digital rendah yang memakai
aplikasi di luar ruangan. Aturan yang ditegakkan sejak Fase 1:

- Teks tidak pernah di bawah 16sp (body default 18sp).
- Semua elemen interaktif minimal 48dp.
- Kontras tinggi; dynamic color (Material You) sengaja dimatikan karena warna
  dari wallpaper dapat merusak rasio kontras.
