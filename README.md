# AgriBicara

Asisten pertanian berbasis suara untuk petani kecil di Indonesia. Petani menekan
tombol mikrofon, bertanya dalam Bahasa Indonesia, dan menerima jawaban lisan yang
dihasilkan AI berdasarkan data cuaca hiperlokal BMKG. Selain itu petani dapat
memfoto tanaman yang sakit untuk mendapat diagnosis penyakit secara offline.

Status saat ini: **Fase 1 — Foundation & Setup** (kerangka arsitektur, belum ada
fitur bisnis). Lihat `.claude/PRPs/prds/agribicara.prd.md` untuk peta fase.

## Syarat Build

| Kebutuhan | Versi |
|---|---|
| JDK | 17 minimum (proyek ini diuji dengan JBR 21 bawaan Android Studio) |
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

3. `local.properties` **tidak pernah di-commit**. File inilah yang nanti menampung
   API key (Gemini, mulai Fase 5). Jangan menaruh kunci apa pun di dalam kode.

## Perintah

```bash
./gradlew assembleDebug            # build debug
./gradlew assembleRelease          # build rilis (R8 + resource shrinking)
./gradlew testDebugUnitTest        # unit test
./gradlew connectedDebugAndroidTest # instrumented test (butuh emulator/device)
./gradlew lintDebug                # analisis statis
```

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
