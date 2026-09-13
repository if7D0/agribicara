# Panduan Pengembang

Detail teknis untuk membangun, menguji, dan merilis AgriBicara. Untuk gambaran
umum proyek, baca [README](../README.md) lebih dulu.

## Daftar isi

- [Syarat build](#syarat-build)
- [Setup](#setup)
- [Menghidupkan fitur AI di clone Anda](#menghidupkan-fitur-ai-di-clone-anda)
- [Perintah](#perintah)
- [Instrumented test](#instrumented-test)
- [Build rilis](#build-rilis)
- [Catatan versi penting](#catatan-versi-penting)
- [Konvensi kode](#konvensi-kode)
- [Aksesibilitas](#aksesibilitas)

## Syarat build

| Kebutuhan | Versi |
|---|---|
| JDK | 17 minimum. Dikembangkan dengan JBR bawaan Android Studio (25.0.2 per 2026-09-02); CI memakai JDK 21. Keduanya ≥17, jadi perbedaan itu tidak masalah |
| Android SDK Platform | API 37 (`platforms;android-37.0`) |
| SDK Build Tools | 36.0.0+ |
| Gradle | 9.7.1 (via wrapper — tidak perlu instalasi terpisah) |
| Perangkat | Android 7.0 (API 24) ke atas |

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

3. `local.properties` **tidak pernah di-commit**, dan tidak menampung kunci API
   apa pun. Proyek ini sengaja tidak memiliki API key Gemini di mana pun — kunci
   yang ditanam di APK bisa dibaca siapa saja yang membongkarnya. Satu-satunya
   rahasia lokal adalah material penandatanganan rilis di `keystore.properties`.

## Menghidupkan fitur AI di clone Anda

Panggilan AI lewat **Firebase AI Logic** yang dijaga **Firebase App Check**:
hanya build yang sidik jari penandatanganannya terdaftar di projek Firebase yang
boleh memanggil. Build hasil clone ditandatangani kunci Anda sendiri, jadi akan
ditolak sampai Anda memakai projek Firebase sendiri:

1. Buat projek di [Firebase Console](https://console.firebase.google.com), tambah
   aplikasi Android dengan package `com.agribicara.app`, lalu **timpa**
   `app/google-services.json` dengan milik Anda.
2. Aktifkan **Firebase AI Logic** (Gemini) di projek itu.
3. **Build debug — daftarkan debug token App Check, sekali per perangkat uji.**

   ```bash
   adb logcat -d | grep DebugAppCheckProvider
   ```

   Salin UUID yang tercetak, lalu daftarkan di **Firebase Console → App Check →
   aplikasi Android → ⋮ → Manage debug tokens**.

   Tanpa langkah ini fitur tanya-jawab AI **mati total** di build debug —
   sementara build tetap sukses, seluruh test tetap hijau, dan tidak ada
   pemeriksaan otomatis yang bisa menangkapnya. Token disimpan di `shared_prefs`
   aplikasi: ia bertahan melewati `adb install -r`, tetapi **hilang bila data
   aplikasi dibersihkan atau aplikasi di-uninstall**. Build debug menyebut
   penyebab ini terang-terangan di layar; build rilis tidak, karena petani tidak
   punya Firebase Console.

4. **Build rilis** — daftarkan **Play Integrity** dan SHA-256 kunci Anda. Bila
   APK dipasang di luar Play (sideload), longgarkan juga syarat
   `PLAY_RECOGNIZED`. Rinciannya ada di
   [`play/release-checklist.md`](play/release-checklist.md) bagian B, lengkap
   dengan arti error `400 App not registered` dan `403 App attestation failed`.

`app/google-services.json` sengaja ikut di-commit: berkas itu konfigurasi klien,
bukan rahasia — isinya ikut terbungkus di setiap APK. Yang menjaga akses adalah
App Check, bukan kerahasiaan berkas itu.

## Perintah

```bash
./gradlew assembleDebug            # build debug
./gradlew assembleRelease          # build rilis (R8 + resource shrinking)
./gradlew bundleRelease            # AAB untuk Play Store
./gradlew testDebugUnitTest        # unit test
./gradlew koverVerifyDebug         # ambang coverage 85% baris
./gradlew lintDebug                # analisis statis
```

CI ([`.github/workflows/ci.yml`](../.github/workflows/ci.yml)) menjalankan
`assembleDebug`, `testDebugUnitTest`, `lintDebug`, dan `assembleRelease` di
setiap push dan pull request.

## Instrumented test

`connectedDebugAndroidTest` sengaja **tidak** dipakai: ia gagal enam kali
berturut-turut di mesin pengembangan proyek ini, tidak satu pun karena kode.
Jalankan instrumented test lewat skrip:

```bash
./gradlew assembleDebug assembleDebugAndroidTest
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
scripts/instrumented.sh              # 92 test, 15 kelas, satu proses per kelas
```

Skrip itu menjalankan **satu kelas per proses**. Bukan kerapian: satu proses
yang menjalankan seluruh 92 test tertahan tanpa sebab yang pernah berhasil
ditemukan, sementara setiap subset sampai 89 test lulus. Alasan lengkap dan
daftar hipotesis yang sudah gugur ada di kepala
[`scripts/instrumented.sh`](../scripts/instrumented.sh) — baca dulu sebelum
mencoba menyatukannya kembali.

Hal yang mudah menjebak:

- **Rebuild APK sebelum menjalankan test.** Test melawan APK basi hanya
  menghasilkan hijau yang tidak berarti.
- Runner-nya **`com.agribicara.app.AgriBicaraTestRunner`**, bukan
  `androidx.test.runner.AndroidJUnitRunner`. Memakai nama lama menghasilkan
  `Unable to find instrumentation info`.
- `am force-stop com.agribicara.app` **tidak cukup**. `com.agribicara.app.test`
  adalah paket TERPISAH dan tetap hidup; sisa prosesnya menabrak run berikutnya
  dan gejalanya menyerupai flaky. Skrip sudah mematikan keduanya.

## Build rilis

1. Buat keystore dan `keystore.properties` mengikuti
   [`keystore.properties.example`](../keystore.properties.example).
2. Jalankan `./gradlew bundleRelease` (AAB) atau `./gradlew assembleRelease` (APK).
3. Verifikasi tanda tangannya:

   ```bash
   # AAB
   keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab
   # APK — pakai apksigner, bukan keytool (APK ditandatangani skema v2)
   apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
   ```

Tanpa `keystore.properties`, `assembleRelease` dan `bundleRelease` **tetap
berjalan** dan menghasilkan artefak unsigned. Itu disengaja: CI membangun rilis
tanpa punya keystore sama sekali.

Aset toko dan ikon legacy dibangkitkan dengan `java tools/StoreAssets.java`.
Langkah Play Console yang **tidak ditempuh** (proyek ditutup sebagai portfolio)
tetap terdokumentasi utuh di
[`play/release-checklist.md`](play/release-checklist.md).

## Catatan versi penting

- **Kotlin dan KSP sengaja tidak dinaikkan.** AGP 9.3.0 meng-embed KGP 2.2.10
  dan KSP 2.2.10-2.0.2; proyek mengikuti pasangan yang diuji AGP. Lint akan
  memberi peringatan bahwa versi lebih baru tersedia — itu disengaja, jangan
  dinaikkan tanpa memverifikasi ulang seluruh rantai AGP/KGP/KSP.
- **AGP 9 memiliki built-in Kotlin.** Plugin `org.jetbrains.kotlin.android`
  TIDAK dipakai (dan akan menggagalkan build jika ditambahkan). Plugin
  `org.jetbrains.kotlin.plugin.compose` tetap diperlukan.
- `android.disallowKotlinSourceSets=false` di `gradle.properties` diperlukan
  karena KSP masih mendaftarkan source hasil generate lewat `kotlin.sourceSets`.
  Tinjau ulang ketika KSP merilis versi yang sadar built-in Kotlin.

## Konvensi kode

- Repository mengembalikan `NetworkResult`, tidak melempar exception lintas layer.
- Satu `<Screen>UiState` per layar, di-expose sebagai `StateFlow` tunggal.
- Pesan error selalu Bahasa Indonesia sederhana (langsung tampil ke petani).
- Logging lewat Timber; tanpa `Log.d`/`println`, tanpa data pribadi di rilis.
- Tanpa nilai hardcoded: teks di `strings.xml`, ukuran di `Dimens`, sisanya di
  `Constants`.

## Aksesibilitas

Target pengguna adalah petani dengan literasi digital rendah yang memakai
aplikasi di luar ruangan. Aturan yang ditegakkan sejak awal:

- Teks tidak pernah di bawah 16sp (body default 18sp).
- Semua elemen interaktif minimal 48dp.
- Kontras tinggi; dynamic color (Material You) sengaja dimatikan karena warna
  dari wallpaper dapat merusak rasio kontras.
