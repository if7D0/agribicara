```markdown
# Plan: AgriBicara — Asisten Pertanian Berbasis Suara & Cuaca Mikro

## Summary
AgriBicara adalah aplikasi Android yang menggabungkan data cuaca hiperlokal dari API BMKG (hingga tingkat kelurahan/desa) dengan kecerdasan buatan (AI) dan antarmuka suara (Voice User Interface). Aplikasi ini dirancang khusus untuk petani dan masyarakat desa di Indonesia agar bisa mendapatkan rekomendasi pertanian praktis hanya dengan berbicara, tanpa perlu membaca grafik atau data teknis. Semua teknologi yang digunakan dalam rencana ini bersifat 100% gratis dengan alternatif yang juga gratis.

## User Story
Sebagai **petani atau warga desa**,
saya ingin **bertanya dengan suara tentang cuaca dan waktu terbaik untuk aktivitas pertanian**,
sehingga saya **mendapat rekomendasi praktis tanpa perlu memahami data cuaca teknis atau membaca teks panjang**.

## Problem → Solution
**Current State:** Petani desa mengandalkan kalender tanam tradisional yang sudah tidak akurat akibat perubahan iklim. Data cuaca BMKG tersedia tetapi terlalu teknis (grafik, tabel, angka) dan sulit dipahami oleh petani dengan literasi digital rendah.
**Desired State:** Petani cukup berbicara ke HP ("Kapan waktu terbaik memupuk padi minggu ini?") dan mendapat jawaban praktis dalam bahasa sederhana berdasarkan data cuaca real-time desanya.

## Metadata
- **Complexity**: Medium
- **Source PRD**: N/A (standalone)
- **PRD Phase**: N/A
- **Estimated Files**: 35–45 file (project baru)
- **Target Platform**: Android (min SDK 24 / Android 7.0)
- **Solo Developer**: Ya — dana minimal, semua teknologi gratis

---

## UX Design

### Before
```
┌─────────────────────────────────────┐
│  Petani melihat ke langit,          │
│  menebak cuaca berdasarkan          │
│  pengalaman / kalender tradisional. │
│                                     │
│  Jika salah → gagal panen,          │
│  pupuk terbuang, rugi waktu.        │
│                                     │
│  Tidak ada akses ke data cuaca      │
│  yang mudah dipahami.               │
└─────────────────────────────────────┘
```

### After
```
┌─────────────────────────────────────┐
│  Petani membuka AgriBicara.         │
│                                     │
│  ┌───────────────────────────────┐  │
│  │  🎤 [TOMBOL MIKROFON BESAR]  │  │
│  │  Tekan & Bicara:              │  │
│  │  "Kapan waktu terbaik         │  │
│  │   memupuk padi minggu ini?"   │  │
│  └───────────────────────────────┘  │
│                                     │
│  AI menjawab dengan SUARA + TEKS:   │
│  "Besok pagi ada hujan lebat di     │
│   desa Anda. Sebaiknya tunda        │
│   pemupukan sampai hari Kamis       │
│   karena cuaca cerah."              │
│                                     │
│  [🔊 Dengar Lagi] [💾 Simpan]      │
└─────────────────────────────────────┘
```

### Interaction Changes
| Touchpoint | Before | After | Notes |
|---|---|---|---|
| Akses info cuaca | Tidak ada / manual tebak | Tekan tombol mic, bicara | VUI-first, teks sebagai pelengkap |
| Pemahaman data cuaca | Grafik/tabel BMKG yang rumit | Kalimat sederhana dari AI | AI menerjemahkan data teknis |
| Rekomendasi pertanian | Berdasarkan kebiasaan turun-temurun | Berbasis data cuaca real-time + AI | Tetap menghormati kearifan lokal |
| Notifikasi cuaca ekstrem | Tidak ada peringatan | Push notification otomatis | Firebase Cloud Messaging |
| Akses tanpa internet | N/A | Cache data cuaca terakhir (offline) | Room Database untuk caching |

---

## Fitur Lengkap

### Fitur Utama (MVP)
| No | Fitur | Deskripsi | Prioritas |
|---|---|---|---|
| F1 | Voice Input (STT) | Pengguna menekan tombol mic dan berbicara dalam bahasa Indonesia. Menggunakan Android native SpeechRecognizer. | P0 |
| F2 | Integrasi BMKG | Mengambil data prakiraan cuaca per kelurahan/kecamatan dari API BMKG. Data: suhu, kelembapan, curah hujan, kecepatan angin, arah angin. | P0 |
| F3 | AI Recommendation Engine | Mengirim data cuaca + pertanyaan pengguna ke LLM (Gemini/Groq) untuk menghasilkan rekomendasi pertanian dalam bahasa sederhana. | P0 |
| F4 | Voice Output (TTS) | AI menjawab dengan suara (Text-to-Speech) agar petani tidak perlu membaca. Menggunakan Android native TTS. | P0 |
| F5 | Deteksi Lokasi Otomatis | Mendeteksi lokasi pengguna via GPS atau pemilihan manual dari daftar provinsi → kabupaten → kecamatan → kelurahan. | P0 |
| F6 | Dashboard Cuaca Harian | Tampilan cuaca hari ini dan 7 hari ke depan dalam format kartu visual sederhana (ikon matahari, hujan, awan). | P1 |
| F7 | Notifikasi Cuaca Ekstrem | Push notification jika BMKG mengeluarkan peringatan dini (hujan lebat, angin kencang, gelombang tinggi). | P1 |
| F8 | Riwayat Pertanyaan | Menyimpan riwayat pertanyaan dan jawaban AI secara lokal agar bisa dilihat ulang. | P1 |
| F9 | Mode Offline | Menampilkan data cuaca terakhir yang di-cache jika tidak ada koneksi internet. AI tidak tersedia offline. | P2 |
| F10 | Bahasa Daerah (Opsional) | Pilihan bahasa Jawa, Sunda, atau Madura untuk output TTS (tergantung ketersediaan engine TTS device). | P2 |

### Fitur yang TIDAK Dibangun (Out of Scope)
- ❌ Deteksi hama via kamera (itu domain Plantix, terlalu kompleks untuk MVP)
- ❌ Marketplace jual-beli hasil tani (itu domain TitipDesa)
- ❌ Integrasi dengan sensor IoT / hardware
- ❌ Multi-user / fitur sosial / komunitas
- ❌ Backend admin panel
- ❌ Pembayaran / fitur berbayar
- ❌ Aplikasi iOS (hanya Android)
- ❌ Chatbot multi-turn conversation kompleks (hanya single Q&A per sesi untuk MVP)

---

## Tech Stack

### Primary Stack (Semua GRATIS)

| Layer | Teknologi | Biaya | Justifikasi |
|---|---|---|---|
| **IDE** | Android Studio | GRATIS | IDE resmi untuk pengembangan Android |
| **Bahasa** | Kotlin | GRATIS | Bahasa resmi Android development |
| **UI Framework** | Jetpack Compose | GRATIS | Modern declarative UI, lebih cepat dikembangkan |
| **Dependency Injection** | Hilt (Dagger) | GRATIS | Standard DI untuk Android, didukung Google |
| **Networking** | Retrofit + OkHttp | GRATIS | HTTP client standar industri untuk Android |
| **Local DB** | Room Database | GRATIS | SQLite abstraction, bagian dari Android Jetpack |
| **Async** | Kotlin Coroutines + Flow | GRATIS | Async programming standar Kotlin |
| **Image Loading** | Coil | GRATIS | Ringan, Kotlin-first image loader |
| **Weather Data** | BMKG Data API | GRATIS | Data resmi pemerintah, tanpa API key untuk data publik |
| **AI / LLM** | Google Gemini API (Free Tier) | GRATIS | 60 RPM free, cukup untuk solo dev |
| **Speech-to-Text** | Android SpeechRecognizer | GRATIS | Built-in Android, tidak perlu API eksternal |
| **Text-to-Speech** | Android TextToSpeech | GRATIS | Built-in Android, support bahasa Indonesia |
| **Backend / Auth** | Firebase Spark Plan | GRATIS | Auth, Firestore, FCM — semua free tier |
| **Push Notification** | Firebase Cloud Messaging | GRATIS | Unlimited push notifications |
| **Maps / Lokasi** | FusedLocationProvider (Google Play Services) | GRATIS | Untuk deteksi GPS device |
| **Peta (jika diperlukan)** | osmdroid + OpenStreetMap | GRATIS | Alternatif gratis untuk Google Maps |
| **Build / CI** | Gradle + GitHub Actions (free tier) | GRATIS | CI/CD gratis untuk public repo |
| **Version Control** | Git + GitHub (private repo free) | GRATIS | Source control |

### Alternative Stack (Jika Primary Tidak Tersedia / Berubah Berbayar)

| Layer | Primary | Alternatif | Catatan |
|---|---|---|---|
| AI / LLM | Gemini API Free Tier | **Groq API Free Tier** | Groq menyediakan free tier dengan model LLaMA 3, kecepatan inference sangat tinggi |
| AI / LLM (alt 2) | Gemini API | **HuggingFace Inference API** | Free tier tersedia, banyak model open-source |
| AI / LLM (alt 3) | Gemini API | **Ollama (local)** | Jika ingin fully offline, jalankan model kecil di device (butuh device high-end) |
| Backend / Auth | Firebase Spark | **Supabase Free Tier** | PostgreSQL, Auth, Realtime — free tier generous (500MB DB, 50K MAU) |
| Backend / Auth (alt 2) | Firebase Spark | **Appwrite Cloud Free** | Self-hostable, free tier tersedia |
| Push Notification | FCM | **Supabase + OneSignal Free** | OneSignal free tier hingga 10K subscribers |
| Weather Data | BMKG API | **Open-Meteo API** | Gratis, open-source, global weather data, tidak perlu API key |
| Weather Data (alt 2) | BMKG API | **OpenWeatherMap Free Tier** | 1000 calls/day gratis, tapi data per kelurahan kurang akurat untuk Indonesia |
| Speech-to-Text | Android SpeechRecognizer | **Vosk (offline STT)** | Open-source, offline capable, tersedia model bahasa Indonesia |
| Text-to-Speech | Android TTS | **eSpeak / Piper TTS** | Open-source TTS engine, bisa di-bundle di APK |
| Peta | osmdroid | **MapLibre GL Native** | Open-source fork dari Mapbox GL, fully free |
| Local DB | Room | **SQLDelight** | Multiplatform, type-safe SQL |
| Networking | Retrofit | **Ktor Client** | Kotlin-native HTTP client |
| UI Framework | Jetpack Compose | **XML Layout + ViewBinding** | Lebih tradisional, lebih ringan untuk device lama |
| CI/CD | GitHub Actions | **Bitbucket Pipelines Free** | 50 menit build gratis/bulan |
| CI/CD (alt 2) | GitHub Actions | **Codemagic Free Tier** | 500 menit build gratis/bulan untuk Flutter/native |

### Detail API yang Digunakan

#### 1. BMKG Data API (Cuaca)
```
Base URL   : https://data.bmkg.go.id/
Format     : XML (untuk feed publik) / JSON (untuk API baru)
Auth       : Tidak perlu API key (data publik pemerintah)
Endpoint   : Prakiraan cuaca per wilayah
Data       : Suhu, kelembapan, curah hujan, kecepatan angin, arah angin
Update     : Data diperbarui oleh BMKG secara berkala
Biaya      : GRATIS
Referensi  : https://data.bmkg.go.id/
Alt        : https://api.bmkg.go.id/ (SATU PETA MKG API)
Alt wrapper: https://github.com/madeaditya02/bmkg-cuaca-api
```

#### 2. Google Gemini API (AI Reasoning)
```
Base URL   : https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent
Auth       : API Key (gratis dari Google AI Studio)
Free Tier  : 60 requests/menit, 1500 requests/hari
Model      : gemini-2.0-flash (tercepat, gratis)
Biaya      : GRATIS untuk tier ini
Referensi  : https://aistudio.google.com/apikey
```

#### 3. Firebase Spark Plan
```
Auth       : Firebase Authentication (email + anonymous)
Database   : Cloud Firestore (50K reads, 20K writes/day)
Storage    : Firebase Storage (5 GB)
Messaging  : FCM (unlimited)
Hosting    : Firebase Hosting (10 GB)
Biaya      : GRATIS (Spark Plan)
Referensi  : https://firebase.google.com/pricing
```

#### 4. Open-Meteo API (Alternatif Cuaca)
```
Base URL   : https://api.open-meteo.com/v1/forecast
Auth       : Tidak perlu API key
Free Tier  : 10,000 calls/day
Data       : Temperature, precipitation, wind, humidity
Biaya      : GRATIS
Referensi  : https://open-meteo.com/
```

---

## Struktur Folder

```
agri-bicara/
│
├── .github/
│   └── workflows/
│       └── ci.yml                          # GitHub Actions CI/CD
│
├── .claude/
│   └── PRPs/
│       └── plans/
│           └── agri-bicara.plan.md         # File plan ini
│
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/agribicara/app/
│   │   │   │   │
│   │   │   │   ├── di/                                    # Dependency Injection
│   │   │   │   │   ├── AppModule.kt                       # Hilt modules utama
│   │   │   │   │   ├── NetworkModule.kt                   # Retrofit, OkHttp setup
│   │   │   │   │   ├── DatabaseModule.kt                  # Room DB setup
│   │   │   │   │   └── RepositoryModule.kt                # Repository bindings
│   │   │   │   │
│   │   │   │   ├── data/                                  # Data Layer
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── dao/
│   │   │   │   │   │   │   ├── WeatherCacheDao.kt         # DAO untuk cache cuaca
│   │   │   │   │   │   │   ├── ChatHistoryDao.kt          # DAO untuk riwayat chat
│   │   │   │   │   │   │   └── UserDao.kt                 # DAO untuk user settings
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   │   ├── WeatherCacheEntity.kt      # Entity cache cuaca
│   │   │   │   │   │   │   ├── ChatHistoryEntity.kt       # Entity riwayat chat
│   │   │   │   │   │   │   └── UserPreferenceEntity.kt    # Entity preferensi user
│   │   │   │   │   │   └── AppDatabase.kt                 # Room Database utama
│   │   │   │   │   │
│   │   │   │   │   ├── remote/
│   │   │   │   │   │   ├── api/
│   │   │   │   │   │   │   ├── BmkgApiService.kt          # Retrofit interface BMKG
│   │   │   │   │   │   │   ├── GeminiApiService.kt        # Retrofit interface Gemini
│   │   │   │   │   │   │   └── OpenMeteoApiService.kt     # Retrofit interface Open-Meteo (fallback)
│   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   │   ├── bmkg/
│   │   │   │   │   │   │   │   ├── BmkgWeatherResponse.kt # Data class response BMKG
│   │   │   │   │   │   │   │   └── BmkgRegionResponse.kt  # Data class wilayah BMKG
│   │   │   │   │   │   │   ├── gemini/
│   │   │   │   │   │   │   │   ├── GeminiRequest.kt       # Request body Gemini
│   │   │   │   │   │   │   │   └── GeminiResponse.kt      # Response body Gemini
│   │   │   │   │   │   │   └── openmeteo/
│   │   │   │   │   │   │       └── OpenMeteoResponse.kt   # Response Open-Meteo
│   │   │   │   │   │   └── interceptor/
│   │   │   │   │   │       └── ApiKeyInterceptor.kt       # OkHttp interceptor untuk API key
│   │   │   │   │   │
│   │   │   │   │   └── repository/
│   │   │   │   │       ├── WeatherRepositoryImpl.kt        # Impl repository cuaca
│   │   │   │   │       ├── AiRepositoryImpl.kt             # Impl repository AI
│   │   │   │   │       └── ChatHistoryRepositoryImpl.kt    # Impl repository riwayat
│   │   │   │   │
│   │   │   │   ├── domain/                                # Domain Layer
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── WeatherData.kt                 # Domain model cuaca
│   │   │   │   │   │   ├── WeatherCondition.kt            # Enum: cerah, hujan, dll
│   │   │   │   │   │   ├── AiRecommendation.kt            # Domain model rekomendasi AI
│   │   │   │   │   │   ├── ChatMessage.kt                 # Domain model chat
│   │   │   │   │   │   ├── Region.kt                      # Domain model wilayah
│   │   │   │   │   │   └── UserPreference.kt              # Domain model preferensi
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── WeatherRepository.kt            # Interface repository cuaca
│   │   │   │   │   │   ├── AiRepository.kt                 # Interface repository AI
│   │   │   │   │   │   └── ChatHistoryRepository.kt        # Interface repository riwayat
│   │   │   │   │   └── usecase/
│   │   │   │   │       ├── GetWeatherUseCase.kt            # Use case ambil cuaca
│   │   │   │   │       ├── GetAiRecommendationUseCase.kt   # Use case rekomendasi AI
│   │   │   │   │       ├── GetRegionListUseCase.kt         # Use case daftar wilayah
│   │   │   │   │       ├── SaveChatHistoryUseCase.kt       # Use case simpan riwayat
│   │   │   │   │       └── CheckExtremeWeatherUseCase.kt   # Use case cek cuaca ekstrem
│   │   │   │   │
│   │   │   │   ├── presentation/                          # Presentation Layer (UI)
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   └── AppNavigation.kt               # NavHost & route definitions
│   │   │   │   │   ├── home/
│   │   │   │   │   │   ├── HomeScreen.kt                  # Layar utama dashboard
│   │   │   │   │   │   └── HomeViewModel.kt               # ViewModel home
│   │   │   │   │   ├── voice/
│   │   │   │   │   │   ├── VoiceInputScreen.kt            # Layar input suara (mic besar)
│   │   │   │   │   │   ├── VoiceInputViewModel.kt         # ViewModel voice
│   │   │   │   │   │   └── components/
│   │   │   │   │   │       ├── MicButton.kt               # Komponen tombol mic animasi
│   │   │   │   │   │       ├── VoiceWaveAnimation.kt      # Animasi gelombang suara
│   │   │   │   │   │       └── VoiceResultCard.kt         # Kartu hasil voice recognition
│   │   │   │   │   ├── chat/
│   │   │   │   │   │   ├── ChatScreen.kt                  # Layar chat dengan AI
│   │   │   │   │   │   ├── ChatViewModel.kt               # ViewModel chat
│   │   │   │   │   │   └── components/
│   │   │   │   │   │       ├── ChatBubble.kt               # Bubble chat
│   │   │   │   │   │       └── ChatInputBar.kt            # Input bar (mic + teks)
│   │   │   │   │   ├── weather/
│   │   │   │   │   │   ├── WeatherDetailScreen.kt         # Detail cuaca 7 hari
│   │   │   │   │   │   ├── WeatherViewModel.kt            # ViewModel cuaca
│   │   │   │   │   │   └── components/
│   │   │   │   │   │       ├── WeatherCard.kt              # Kartu cuaca harian
│   │   │   │   │   │       ├── WeatherIcon.kt             # Ikon cuaca custom
│   │   │   │   │   │       └── WeeklyForecastList.kt      # List prakiraan mingguan
│   │   │   │   │   ├── settings/
│   │   │   │   │   │   ├── SettingsScreen.kt              # Layar pengaturan
│   │   │   │   │   │   ├── SettingsViewModel.kt           # ViewModel settings
│   │   │   │   │   │   └── components/
│   │   │   │   │   │       ├── RegionPicker.kt            # Picker wilayah
│   │   │   │   │   │       └── LanguageSelector.kt        # Pilih bahasa output
│   │   │   │   │   └── onboarding/
│   │   │   │   │       ├── OnboardingScreen.kt            # Layar onboarding pertama kali
│   │   │   │   │       └── OnboardingViewModel.kt         # ViewModel onboarding
│   │   │   │   │
│   │   │   │   ├── service/                               # Background Services
│   │   │   │   │   ├── WeatherCheckWorker.kt              # WorkManager untuk cek cuaca berkala
│   │   │   │   │   └── ExtremeWeatherNotifier.kt          # Notifikasi cuaca ekstrem
│   │   │   │   │
│   │   │   │   ├── util/                                  # Utilities
│   │   │   │   │   ├── Constants.kt                       # Konstanta app-wide
│   │   │   │   │   ├── NetworkResult.kt                   # Sealed class untuk network state
│   │   │   │   │   ├── DateUtils.kt                       # Helper format tanggal
│   │   │   │   │   ├── LocationHelper.kt                  # Helper GPS / fused location
│   │   │   │   │   ├── SpeechHelper.kt                    # Wrapper STT & TTS
│   │   │   │   │   └── WeatherIconMapper.kt               # Mapping kode cuaca → ikon
│   │   │   │   │
│   │   │   │   └── AgriBicaraApp.kt                       # Application class (@HiltAndroidApp)
│   │   │   │
│   │   │   ├── res/
│   │   │   │   ├── drawable/                              # Ikon, vektor, background
│   │   │   │   │   ├── ic_cerah.xml
│   │   │   │   │   ├── ic_hujan.xml
│   │   │   │   │   ├── ic_berawan.xml
│   │   │   │   │   ├── ic_badai.xml
│   │   │   │   │   ├── ic_angin.xml
│   │   │   │   │   └── ic_mic.xml
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml                        # Semua teks UI
│   │   │   │   │   ├── colors.xml                         # Palet warna
│   │   │   │   │   ├── themes.xml                         # Tema Material 3
│   │   │   │   │   └── dimens.xml                         # Dimensi spacing
│   │   │   │   ├── values-in/                             # String bahasa Indonesia
│   │   │   │   │   └── strings.xml
│   │   │   │   ├── raw/                                   # Audio/sound effects (opsional)
│   │   │   │   └── xml/
│   │   │   │       └── network_security_config.xml        # Network security config
│   │   │   │
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   ├── test/                                          # Unit Tests
│   │   │   └── java/com/agribicara/app/
│   │   │       ├── data/
│   │   │       │   └── repository/
│   │   │       │       ├── WeatherRepositoryTest.kt
│   │   │       │       └── AiRepositoryTest.kt
│   │   │       ├── domain/
│   │   │       │   └── usecase/
│   │   │       │       ├── GetWeatherUseCaseTest.kt
│   │   │       │       └── GetAiRecommendationUseCaseTest.kt
│   │   │       └── util/
│   │   │           └── WeatherIconMapperTest.kt
│   │   │
│   │   └── androidTest/                                   # Instrumented Tests
│   │       └── java/com/agribicara/app/
│   │           ├── data/local/
│   │           │   └── WeatherCacheDaoTest.kt
│   │           └── presentation/
│   │               └── HomeScreenTest.kt
│   │
│   ├── build.gradle.kts                                   # App-level build config
│   └── proguard-rules.pro                                 # ProGuard rules
│
├── build.gradle.kts                                       # Project-level build config
├── settings.gradle.kts                                    # Module settings
├── gradle.properties                                      # Gradle properties
├── gradle/
│   └── libs.versions.toml                                 # Version catalog (BOM)
├── local.properties                                       # API keys (GITIGNORE!)
├── .gitignore
├── README.md
├── plan.md                                                # File ini
└── PRD.md                                                 # PRD dokumen
```

---

## Roadmap Pengerjaan

### Overview Timeline: 8 Minggu (Solo Developer)

```
Minggu 1-2  ████████████  Phase 1: Foundation & Setup
Minggu 2-3  ████████████  Phase 2: BMKG Weather Integration
Minggu 3-4  ████████████  Phase 3: Voice Interface (STT + TTS)
Minggu 4-5  ████████████  Phase 4: AI Integration (Gemini)
Minggu 5-6  ████████████  Phase 5: UI Polish & UX Optimization
Minggu 6-7  ████████████  Phase 6: Testing & Bug Fixing
Minggu 7-8  ████████████  Phase 7: Launch Preparation
```

---

### Phase 1: Foundation & Setup (Minggu 1–2)

**Goal:** Project Android berjalan, arsitektur Clean Architecture terpasang, navigasi dasar berfungsi.

| Task | Detail | Estimasi | Deliverable |
|---|---|---|---|
| 1.1 | Buat project Android Studio baru (Empty Compose Activity), min SDK 24, target SDK 35 | 1 jam | Project skeleton |
| 1.2 | Setup `gradle/libs.versions.toml` dengan semua dependency versions | 2 jam | Version catalog |
| 1.3 | Konfigurasi Hilt (DI) — buat `AgriBicaraApp.kt`, `AppModule.kt`, `NetworkModule.kt` | 3 jam | DI berjalan |
| 1.4 | Buat struktur folder Clean Architecture (data, domain, presentation, di, util) | 2 jam | Struktur folder |
| 1.5 | Setup Jetpack Compose Navigation dengan route: Onboarding, Home, Voice, Weather, Settings | 4 jam | Navigasi berfungsi |
| 1.6 | Buat `NetworkResult.kt` sealed class (Success, Error, Loading) | 1 jam | Error handling base |
| 1.7 | Buat `Constants.kt` — semua konstanta app (API keys placeholder, base URLs) | 1 jam | Konstanta terpusat |
| 1.8 | Setup Room Database — `AppDatabase.kt`, entities, DAOs | 4 jam | Local DB siap |
| 1.9 | Buat Onboarding screen sederhana (3 slide: intro, izin lokasi, izin mic) | 4 jam | Onboarding UI |
| 1.10 | Setup GitHub repo, `.gitignore`, GitHub Actions CI (build only) | 2 jam | Repo + CI |

**Dependencies Phase 1:**
```kotlin
// gradle/libs.versions.toml
[versions]
agp = "8.5.0"
kotlin = "2.0.0"
compose-bom = "2024.09.00"
hilt = "2.51.1"
retrofit = "2.11.0"
okhttp = "4.12.0"
room = "2.6.1"
navigation-compose = "2.8.0"
coroutines = "1.9.0"
coil = "2.7.0"

[libraries]
# Compose
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-navigation = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }

# Networking
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }

# Coroutines
coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

# Image
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version = "2.0.0-1.0.24" }
```

**Validation Phase 1:**
- [ ] `./gradlew build` berhasil tanpa error
- [ ] Navigasi antar screen berfungsi (Onboarding → Home)
- [ ] Hilt injection berhasil (tidak crash saat DI)
- [ ] Room Database berhasil dibuat dan DAO bisa insert/read
- [ ] CI pipeline di GitHub Actions hijau (build pass)

---

### Phase 2: BMKG Weather Integration (Minggu 2–3)

**Goal:** Aplikasi bisa mengambil dan menampilkan data cuaca dari BMKG berdasarkan lokasi pengguna.

| Task | Detail | Estimasi | Deliverable |
|---|---|---|---|
| 2.1 | Buat `BmkgApiService.kt` — Retrofit interface untuk BMKG API | 3 jam | API interface |
| 2.2 | Buat DTO classes: `BmkgWeatherResponse.kt`, `BmkgRegionResponse.kt` | 3 jam | Data classes |
| 2.3 | Buat `OpenMeteoApiService.kt` sebagai fallback API | 2 jam | Fallback API |
| 2.4 | Buat `WeatherRepository.kt` (interface) dan `WeatherRepositoryImpl.kt` | 4 jam | Repository layer |
| 2.5 | Buat `GetWeatherUseCase.kt` — ambil cuaca per lokasi, cache ke Room | 3 jam | Use case |
| 2.6 | Buat `GetRegionListUseCase.kt` — daftar provinsi/kab/kecamatan/kelurahan | 3 jam | Use case wilayah |
| 2.7 | Buat `WeatherCacheDao.kt` dan `WeatherCacheEntity.kt` di Room | 2 jam | Cache layer |
| 2.8 | Implementasi `LocationHelper.kt` — FusedLocationProvider untuk GPS | 3 jam | Lokasi GPS |
| 2.9 | Buat `WeatherDetailScreen.kt` + `WeatherViewModel.kt` — tampilkan cuaca 7 hari | 6 jam | UI cuaca |
| 2.10 | Buat komponen: `WeatherCard.kt`, `WeatherIcon.kt`, `WeeklyForecastList.kt` | 4 jam | UI components |
| 2.11 | Buat `WeatherIconMapper.kt` — mapping kode cuaca BMKG ke ikon | 2 jam | Utility |
| 2.12 | Implementasi fallback logic: jika BMKG error → gunakan Open-Meteo | 2 jam | Resilience |
| 2.13 | Buat `WeatherCheckWorker.kt` — WorkManager untuk refresh cuaca tiap 6 jam | 3 jam | Background sync |

**BMKG API Integration Detail:**
```kotlin
// BmkgApiService.kt
interface BmkgApiService {

    // Prakiraan cuaca per wilayah
    // BMKG menggunakan XML feed publik atau JSON API baru
    @GET("datameteo/public/data/prakicu/{region_code}.xml")
    suspend fun getWeatherForecast(
        @Path("region_code") regionCode: String
    ): Response<ResponseBody>

    // Alternatif: Open-Meteo API (fallback)
    @GET("v1/forecast")
    suspend fun getOpenMeteoForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,precipitation_sum,windspeed_10m_max,weathercode",
        @Query("timezone") timezone: String = "Asia/Jakarta",
        @Query("forecast_days") days: Int = 7
    ): Response<OpenMeteoResponse>
}
```

**Fallback Strategy:**
```
BMKG API (primary)
    │
    ├── Success → Parse & display
    │
    └── Error / Timeout
            │
            └── Open-Meteo API (fallback)
                    │
                    ├── Success → Parse & display (label: "Data estimasi")
                    │
                    └── Error → Show cached data from Room + "Offline mode" banner
```

**Validation Phase 2:**
- [ ] Data cuaca 7 hari berhasil ditampilkan di `WeatherDetailScreen`
- [ ] Lokasi GPS terdeteksi dan cuaca sesuai lokasi pengguna
- [ ] Pemilihan manual wilayah (provinsi → kelurahan) berfungsi
- [ ] Fallback ke Open-Meteo berfungsi saat BMKG API down
- [ ] Cache cuaca tersimpan di Room dan bisa ditampilkan offline
- [ ] WorkManager refresh cuaca tiap 6 jam berjalan

---

### Phase 3: Voice Interface — STT + TTS (Minggu 3–4)

**Goal:** Pengguna bisa berbicara ke aplikasi dan mendengar jawaban dengan suara.

| Task | Detail | Estimasi | Deliverable |
|---|---|---|---|
| 3.1 | Buat `SpeechHelper.kt` — wrapper untuk Android SpeechRecognizer | 4 jam | STT wrapper |
| 3.2 | Implementasi STT: request permission mic, start listening, handle results | 4 jam | STT berfungsi |
| 3.3 | Implementasi TTS: Android TextToSpeech engine, bahasa Indonesia | 3 jam | TTS berfungsi |
| 3.4 | Buat `VoiceInputScreen.kt` — layar utama dengan tombol mic besar | 6 jam | UI voice |
| 3.5 | Buat komponen `MicButton.kt` — tombol mic dengan animasi pulse saat listening | 3 jam | Mic component |
| 3.6 | Buat komponen `VoiceWaveAnimation.kt` — animasi gelombang saat merekam | 3 jam | Animasi |
| 3.7 | Buat komponen `VoiceResultCard.kt` — tampilkan teks hasil STT | 2 jam | Result card |
| 3.8 | Handle edge cases STT: no speech, network error, permission denied | 3 jam | Error handling |
| 3.9 | Implementasi auto-play TTS setelah AI menjawab | 2 jam | Auto TTS |
| 3.10 | Buat `VoiceInputViewModel.kt` — state management untuk voice flow | 3 jam | ViewModel |

**SpeechHelper Implementation Pattern:**
```kotlin
// util/SpeechHelper.kt
class SpeechHelper(
    private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    // === SPEECH-TO-TEXT ===
    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onPartial: (String) -> Unit
    ) {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                    matches?.firstOrNull()?.let { onResult(it) }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                    matches?.firstOrNull()?.let { onPartial(it) }
                }
                override fun onError(error: Int) {
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Tidak terdengar suara"
                        SpeechRecognizer.ERROR_NETWORK -> "Periksa koneksi internet"
                        SpeechRecognizer.ERROR_AUDIO -> "Gangguan pada mikrofon"
                        else -> "Terjadi kesalahan"
                    }
                    onError(msg)
                }
                // ... implementasi callback lainnya
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    // === TEXT-TO-SPEECH ===
    fun initTTS(onReady: () -> Unit) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("id", "ID")
                textToSpeech?.setSpeechRate(0.9f) // Sedikit lebih lambat untuk kejelasan
                textToSpeech?.setPitch(1.0f)
                onReady()
            }
        }
    }

    fun speak(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "agri_response")
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
    }

    fun release() {
        textToSpeech?.shutdown()
        speechRecognizer?.destroy()
    }
}
```

**Voice Flow:**
```
User tekan tombol mic
    → Animasi gelombang muncul
    → SpeechRecognizer mulai mendengarkan (bahasa: id-ID)
    → User bicara: "Kapan waktu terbaik memupuk padi?"
    → STT mengembalikan teks
    → Teks dikirim ke AI (Phase 4)
    → AI mengembalikan jawaban teks
    → TTS membacakan jawaban
    → Teks jawaban juga ditampilkan di layar
```

**Validation Phase 3:**
- [ ] Tombol mic berfungsi: tekan → listening → lepas → hasil teks muncul
- [ ] STT mengenali bahasa Indonesia dengan akurat
- [ ] TTS membacakan teks dalam bahasa Indonesia
- [ ] Animasi gelombang muncul saat listening
- [ ] Permission mic diminta saat pertama kali dan ditangani jika ditolak
- [ ] Edge case: tidak ada suara → pesan "Tidak terdengar suara"
- [ ] Edge case: tidak ada internet untuk STT → pesan error yang jelas

---

### Phase 4: AI Integration — Gemini API (Minggu 4–5)

**Goal:** Pertanyaan pengguna + data cuaca diproses oleh AI untuk menghasilkan rekomendasi pertanian.

| Task | Detail | Estimasi | Deliverable |
|---|---|---|---|
| 4.1 | Buat `GeminiApiService.kt` — Retrofit interface untuk Gemini API | 2 jam | API interface |
| 4.2 | Buat DTO: `GeminiRequest.kt`, `GeminiResponse.kt` | 2 jam | Data classes |
| 4.3 | Buat `AiRepository.kt` (interface) dan `AiRepositoryImpl.kt` | 3 jam | Repository |
| 4.4 | Buat `GetAiRecommendationUseCase.kt` — prompt engineering + call Gemini | 5 jam | Use case utama |
| 4.5 | Implementasi prompt template untuk konteks pertanian Indonesia | 4 jam | Prompt template |
| 4.6 | Buat `ChatMessage.kt` domain model dan `ChatHistoryEntity.kt` | 2 jam | Models |
| 4.7 | Buat `ChatHistoryRepositoryImpl.kt` + `ChatHistoryDao.kt` | 3 jam | History storage |
| 4.8 | Buat `ChatScreen.kt` + `ChatViewModel.kt` — UI chat sederhana | 5 jam | Chat UI |
| 4.9 | Implementasi retry logic untuk Gemini API (rate limit handling) | 2 jam | Resilience |
| 4.10 | Buat fallback: jika Gemini error → tampilkan data cuaca mentah + disclaimer | 2 jam | Fallback |

**Prompt Engineering (Inti dari AgriBicara):**
```kotlin
// domain/usecase/GetAiRecommendationUseCase.kt
class GetAiRecommendationUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val weatherRepository: WeatherRepository
) {
    suspend fun execute(
        userQuestion: String,
        regionName: String,
        weatherData: List<WeatherData>
    ): Result<AiRecommendation> {

        // Format data cuaca menjadi teks terstruktur untuk prompt
        val weatherContext = buildWeatherContext(weatherData)

        val systemPrompt = """
            Kamu adalah AgriBicara, asisten pertanian untuk petani Indonesia.

            ATURAN UTAMA:
            1. Jawab dalam bahasa Indonesia yang SEDERHANA dan MUDAH DIPAHAMI.
            2. Gunakan kalimat pendek. Hindari istilah teknis.
            3. Jawaban maksimal 3 kalimat.
            4. Selalu berdasarkan data cuaca yang diberikan.
            5. Jika pertanyaan bukan tentang pertanian/cuaca, jawab sopan bahwa kamu hanya membantu soal pertanian.
            6. Hormati kearifan lokal petani. Jangan merendahkan cara tradisional.
            7. Format jawaban untuk diucapkan (TTS), jangan pakai bullet point atau markdown.

            DATA CUACA UNTUK ${regionName.uppercase()} (7 HARI KE DEPAN):
            ${weatherContext}
        """.trimIndent()

        return aiRepository.getRecommendation(
            systemPrompt = systemPrompt,
            userQuery = userQuestion
        )
    }

    private fun buildWeatherContext(data: List<WeatherData>): String {
        return data.joinToString("\n") { day ->
            "Tanggal: ${day.date}, " +
            "Cuaca: ${day.condition.description}, " +
            "Suhu: ${day.tempMin}°C - ${day.tempMax}°C, " +
            "Curah Hujan: ${day.precipitation}mm, " +
            "Kelembapan: ${day.humidity}%, " +
            "Angin: ${day.windSpeed} km/jam ${day.windDirection}"
        }
    }
}
```

**Gemini API Call:**
```kotlin
// data/remote/api/GeminiApiService.kt
interface GeminiApiService {

    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}

// data/remote/dto/gemini/GeminiRequest.kt
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GenerationConfig? = null
)

data class GeminiContent(
    val role: String, // "user" atau "model"
    val parts: List<GeminiPart>
)

data class GeminiPart(val text: String)

data class GenerationConfig(
    val temperature: Float = 0.7f,
    val maxOutputTokens: Int = 256, // Batasi agar jawaban singkat
    val topP: Float = 0.9f
)
```

**Fallback Chain:**
```
Gemini API (primary)
    │
    ├── Success → Tampilkan jawaban AI
    │
    ├── Rate Limit (429) → Retry 1x setelah 5 detik
    │       └── Masih gagal → Fallback ke Groq API
    │
    └── Error / Timeout
            │
            └── Groq API (fallback, jika sudah di-setup)
                    │
                    └── Masih gagal → Tampilkan data cuaca mentah +
                        pesan: "AI sedang tidak tersedia.
                        Berikut data cuaca untuk daerah Anda."
```

**Validation Phase 4:**
- [ ] Pertanyaan "Kapan waktu terbaik memupuk padi?" menghasilkan jawaban relevan berbasis data cuaca
- [ ] Jawaban AI dalam bahasa Indonesia sederhana, maksimal 3 kalimat
- [ ] Jawaban AI dibacakan oleh TTS secara otomatis
- [ ] Riwayat chat tersimpan di Room dan bisa dilihat kembali
- [ ] Rate limit Gemini ditangani dengan retry
- [ ] Fallback berfungsi saat Gemini API down

---

### Phase 5: UI Polish & UX Optimization (Minggu 5–6)

**Goal:** Aplikasi nyaman digunakan oleh petani dengan literasi digital rendah.

| Task | Detail | Estimasi | Deliverable |
|---|---|---|---|
| 5.1 | Implementasi Material 3 theme dengan warna earthy (hijau, coklat, kuning) | 3 jam | Theme |
| 5.2 | Optimasi ukuran font minimum 16sp untuk keterbacaan | 1 jam | Aksesibilitas |
| 5.3 | Buat semua tombol minimum 48dp x 48dp (touch target) | 2 jam | Aksesibilitas |
| 5.4 | Implementasi loading states dengan shimmer animation | 3 jam | Loading UX |
| 5.5 | Buat `ExtremeWeatherNotifier.kt` — cek cuaca ekstrem, kirim push notification | 4 jam | Notifikasi |
| 5.6 | Setup Firebase Cloud Messaging untuk push notification | 3 jam | FCM setup |
| 5.7 | Implementasi `RegionPicker.kt` — cascading dropdown provinsi → kelurahan | 4 jam | Region picker |
| 5.8 | Buat `SettingsScreen.kt` — ganti wilayah, kecepatan TTS, hapus riwayat | 4 jam | Settings |
| 5.9 | Optimasi untuk layar kecil dan device low-end (min RAM 2GB) | 3 jam | Performance |
| 5.10 | Buat `HomeScreen.kt` final — greeting, cuaca hari ini, tombol mic besar | 5 jam | Home final |
| 5.11 | Implementasi dark mode | 2 jam | Dark mode |
| 5.12 | Buat splash screen sederhana | 1 jam | Splash |

**Design Principles untuk Target User:**
```
1. SATU AKSI UTAMA PER LAYAR
   → Home: tombol mic besar di tengah. Itu saja.

2. TEKS MINIMAL, IKON BESAR
   → Gunakan ikon matahari/hujan/awan, bukan angka teknis.

3. WARNA KONTRAS TINGGI
   → Petani sering di outdoor, layar terkena sinar matahari.

4. FONT BESAR (min 16sp, heading 24sp+)
   → Banyak pengguna lansia dengan penglihatan menurun.

5. FEEDBACK LANGSUNG
   → Setiap sentuhan harus ada respons visual/audio.

6. OFFLINE-FIRST MINDSET
   → Selalu ada konten yang bisa dilihat tanpa internet.
```

**Validation Phase 5:**
- [ ] UI nyaman digunakan di outdoor (kontras tinggi)
- [ ] Semua tombol mudah ditekan (min 48dp)
- [ ] Loading states informatif (bukan blank screen)
- [ ] Notifikasi cuaca ekstrem muncul saat ada peringatan BMKG
- [ ] Region picker berfungsi dari provinsi hingga kelurahan
- [ ] Dark mode berfungsi

---

### Phase 6: Testing & Bug Fixing (Minggu 6–7)

**Goal:** Aplikasi stabil, bebas crash, dan edge cases tertangani.

| Task | Detail | Estimasi | Deliverable |
|---|---|---|---|
| 6.1 | Tulis unit tests untuk semua Use Cases (JUnit + MockK) | 6 jam | Unit tests |
| 6.2 | Tulis unit tests untuk Repository layer | 4 jam | Repo tests |
| 6.3 | Tulis unit tests untuk `WeatherIconMapper` dan `DateUtils` | 2 jam | Util tests |
| 6.4 | Tulis instrumented test untuk Room DAO | 3 jam | DB tests |
| 6.5 | Manual testing di device low-end (Android 7, RAM 2GB) | 4 jam | Compatibility |
| 6.6 | Manual testing STT dengan aksen daerah (Jawa, Sunda) | 3 jam | STT accuracy |
| 6.7 | Testing offline mode — matikan internet, pastikan cache berfungsi | 2 jam | Offline test |
| 6.8 | Testing semua edge cases dari checklist | 3 jam | Edge cases |
| 6.9 | Fix semua bugs yang ditemukan | 6 jam | Bug fixes |
| 6.10 | ProGuard / R8 optimization untuk release build | 2 jam | Optimized APK |

**Unit Test Pattern:**
```kotlin
// test/java/com/agribicara/app/domain/usecase/GetWeatherUseCaseTest.kt
@OptIn(ExperimentalCoroutinesApi::class)
class GetWeatherUseCaseTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val mockWeatherRepository: WeatherRepository = mockk()
    private lateinit var useCase: GetWeatherUseCase

    @Before
    fun setup() {
        useCase = GetWeatherUseCase(mockWeatherRepository)
    }

    @Test
    fun `get weather returns success with valid data`() = runTest {
        // Given
        val region = Region("3201", "Bogor", "Jawa Barat")
        val weatherData = listOf(
            WeatherData(
                date = "2025-01-15",
                condition = WeatherCondition.RAIN,
                tempMin = 22.0,
                tempMax = 31.0,
                precipitation = 15.5,
                humidity = 85,
                windSpeed = 10.0,
                windDirection = "Barat"
            )
        )
        coEvery { mockWeatherRepository.getWeatherByRegion(any()) } returns Result.success(weatherData)

        // When
        val result = useCase.execute(region)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals(WeatherCondition.RAIN, result.getOrNull()?.first()?.condition)
    }

    @Test
    fun `get weather returns error when repository fails`() = runTest {
        // Given
        val region = Region("3201", "Bogor", "Jawa Barat")
        coEvery { mockWeatherRepository.getWeatherByRegion(any()) } returns Result.failure(Exception("API Error"))

        // When
        val result = useCase.execute(region)

        // Then
        assertTrue(result.isFailure)
    }
}
```

**Edge Cases Checklist:**
- [ ] Tidak ada internet saat pertama kali buka app
- [ ] GPS tidak tersedia → fallback ke pemilihan manual wilayah
- [ ] Permission mic ditolak → tampilkan dialog penjelasan, arahkan ke Settings
- [ ] Permission lokasi ditolak → arahkan ke pemilihan manual wilayah
- [ ] STT tidak mengenali suara → pesan "Coba bicara lebih jelas"
- [ ] Gemini API rate limit → retry + fallback
- [ ] Gemini API down total → tampilkan data cuaca mentah
- [ ] BMKG API down → fallback ke Open-Meteo
- [ ] Semua API down → tampilkan cache + banner offline
- [ ] User bertanya di luar konteks pertanian → AI menjawab sopan
- [ ] TTS tidak tersedia di device → fallback ke teks saja
- [ ] Device dengan RAM sangat rendah → tidak crash
- [ ] Rotasi layar saat voice recording → state tidak hilang
- [ ] App di-background saat TTS berbicara → TTS berhenti / lanjut (pilih salah satu)

**Validation Phase 6:**
- [ ] Semua unit tests pass (`./gradlew test`)
- [ ] Semua instrumented tests pass (`./gradlew connectedAndroidTest`)
- [ ] Tidak ada crash di device low-end
- [ ] Semua edge cases tertangani
- [ ] APK release size < 15 MB (tanpa bundle AI model lokal)

---

### Phase 7: Launch Preparation (Minggu 7–8)

**Goal:** Aplikasi siap di-publish ke Google Play Store.

| Task | Detail | Estimasi | Deliverable |
|---|---|---|---|
| 7.1 | Buat signed release build (APK + AAB) | 2 jam | Release build |
| 7.2 | Buat ikon app (512x512, adaptive icon) | 3 jam | App icon |
| 7.3 | Buat screenshot untuk Play Store (min 2) | 2 jam | Screenshots |
| 7.4 | Tulis deskripsi Play Store (judul, deskripsi pendek, deskripsi panjang) | 2 jam | Store listing |
| 7.5 | Buat Privacy Policy sederhana (website / GitHub Pages) | 2 jam | Privacy policy |
| 7.6 | Setup Firebase Crashlytics untuk monitoring crash | 2 jam | Crash monitoring |
| 7.7 | Upload ke Google Play Console (Internal Testing track dulu) | 2 jam | Internal test |
| 7.8 | Beta testing dengan 5–10 petani / warga desa | 5 hari | Feedback |
| 7.9 | Fix bugs dari beta testing | 3 hari | Hotfixes |
| 7.10 | Promote ke Production track | 1 jam | 🚀 LAUNCH |

**Play Store Listing Draft:**
```
Judul: AgriBicara - Asisten Tani Suara
Deskripsi Pendek: Tanya soal cuaca & pertanian pakai suara. Gratis untuk petani Indonesia.

Deskripsi Panjang:
AgriBicara membantu petani Indonesia mendapatkan informasi cuaca dan rekomendasi pertanian hanya dengan BERBICARA.

🎤 CUKUP BICARA
Tekan tombol mikrofon dan bertanya: "Kapan waktu terbaik memupuk padi?" atau "Apakah besok akan hujan?"

🌦️ CUACA DESA ANDA
Data cuaca langsung dari BMKG untuk wilayah kecamatan/desa Anda, bukan data kota.

🤖 JAWABAN SEDERHANA
Kecerdasan buatan menerjemahkan data cuaca menjadi saran praktis yang mudah dipahami.

🔊 DENGAR JAWABAN
Tidak perlu membaca. AgriBicara akan membacakan jawaban untuk Anda.

✅ 100% GRATIS
Tanpa iklan, tanpa biaya langganan. Dibuat untuk petani Indonesia.
```

**Validation Phase 7:**
- [ ] Signed AAB berhasil di-upload ke Play Console
- [ ] Privacy policy terpasang
- [ ] Crashlytics aktif dan tidak ada crash saat beta test
- [ ] Minimal 5 tester memberikan feedback
- [ ] App live di Production track

---

## Testing Strategy

### Unit Tests
| Test | Input | Expected Output | Edge Case? |
|---|---|---|---|
| GetWeatherUseCase success | Region valid, API returns data | List<WeatherData> dengan 7 items | No |
| GetWeatherUseCase API error | Region valid, API returns 500 | Result.failure dengan pesan error | No |
| GetWeatherUseCase empty response | Region valid, API returns empty | Result.failure("Data tidak tersedia") | Yes |
| GetAiRecommendation success | Question + weather data | AiRecommendation dengan teks ≤ 3 kalimat | No |
| GetAiRecommendation off-topic | "Siapa presiden Indonesia?" | AI menjawab hanya bantu pertanian | Yes |
| GetAiRecommendation rate limit | 61 requests dalam 1 menit | Retry → fallback ke Groq | Yes |
| WeatherIconMapper mapping | Kode cuaca BMKG "61" | Icon hujan ringan | No |
| WeatherIconMapper unknown code | Kode cuaca "999" | Icon default (awan) | Yes |
| DateUtils format | "2025-01-15" | "Rabu, 15 Januari 2025" | No |
| DateUtils invalid | "invalid-date" | Fallback: "N/A" | Yes |
| ChatHistory save | 100 chat messages | Semua tersimpan di Room | No |
| ChatHistory max limit | 1001 chat messages | Pesan tertua dihapus | Yes |

### Edge Cases Checklist
- [ ] Empty input (user tidak bicara apa-apa)
- [ ] Maximum size input (user bicara sangat panjang > 30 detik)
- [ ] Invalid types (response API tidak sesuai schema)
- [ ] Concurrent access (user tekan mic berkali-kali cepat)
- [ ] Network failure (wifi mati saat API call)
- [ ] Permission denied (mic dan lokasi ditolak)
- [ ] Device tanpa Google Play Services (STT tidak tersedia)
- [ ] TTS engine tidak terinstall di device
- [ ] BMKG API response format berubah (XML vs JSON)
- [ ] Gemini API response kosong / null
- [ ] Room database corrupt
- [ ] App di-kill OS saat background (low memory)

---

## Validation Commands

### Static Analysis
```bash
# Jalankan Kotlin compiler check
./gradlew compileDebugKotlin

# Jalankan Lint
./gradlew lintDebug
```
EXPECT: Zero errors, warnings minimal

### Unit Tests
```bash
# Jalankan semua unit tests
./gradlew testDebugUnitTest

# Jalankan dengan coverage report
./gradlew testDebugUnitTest jacocoTestReport
```
EXPECT: All tests pass, coverage > 60%

### Instrumented Tests
```bash
# Jalankan di emulator / device
./gradlew connectedDebugAndroidTest
```
EXPECT: All instrumented tests pass

### Full Test Suite
```bash
# Jalankan semua: unit + instrumented
./gradlew test connectedAndroidTest
```
EXPECT: No regressions

### Build Verification
```bash
# Debug build
./gradlew assembleDebug

# Release build (signed)
./gradlew assembleRelease
```
EXPECT: APK/AAB berhasil dibuat tanpa error

### Manual Validation
- [ ] Buka app → Onboarding muncul → bisa skip/next
- [ ] Izin lokasi diminta → grant → cuaca sesuai lokasi
- [ ] Izin mic diminta → grant → bisa bicara
- [ ] Tekan mic → bicara "Apakah besok hujan?" → jawaban muncul + dibacakan
- [ ] Tekan mic → bicara "Kapan waktu terbaik menanam jagung?" → jawaban relevan
- [ ] Matikan internet → buka app → data cache muncul + banner offline
- [ ] Hidupkan internet → data refresh otomatis
- [ ] Buka Settings → ganti wilayah → cuaca berubah sesuai wilayah baru
- [ ] Notifikasi cuaca ekstrem muncul saat ada peringatan (simulasi)
- [ ] Force close app → buka lagi → state terakhir tersimpan
- [ ] Gunakan di device Android 7 (min SDK) → tidak crash
- [ ] Gunakan di device dengan RAM 2GB → tidak lag signifikan

---

## Acceptance Criteria
- [ ] Semua 7 phases selesai
- [ ] Semua validation commands pass
- [ ] Unit tests ditulis dan passing (coverage > 60%)
- [ ] Tidak ada type errors / compile errors
- [ ] Tidak ada lint errors
- [ ] UX sesuai design (voice-first, tombol besar, font besar)
- [ ] Data cuaca berasal dari BMKG atau fallback Open-Meteo
- [ ] AI menjawab dalam bahasa Indonesia sederhana
- [ ] TTS membacakan jawaban
- [ ] App berfungsi offline (cache mode)
- [ ] APK size < 15 MB
- [ ] App live di Google Play Store

## Completion Checklist
- [ ] Code mengikuti Clean Architecture (data → domain → presentation)
- [ ] Error handling konsisten dengan NetworkResult sealed class
- [ ] Logging menggunakan Timber (debug) / Firebase Crashlytics (release)
- [ ] Tests mengikuti pattern JUnit + MockK + coroutines-test
- [ ] Tidak ada hardcoded values (semua di Constants.kt atau BuildConfig)
- [ ] README.md updated dengan setup instructions
- [ ] Tidak ada scope creep (fitur yang tidak ada di plan ini TIDAK dibangun)
- [ ] Self-contained — implementer tidak perlu bertanya lagi

## Risks
| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| BMKG API tidak terdokumentasi dengan baik / format berubah | Tinggi | Tinggi | Siapkan fallback ke Open-Meteo API; parse secara defensif |
| Gemini free tier rate limit terlalu ketat untuk usage nyata | Sedang | Sedang | Cache AI responses untuk pertanyaan serupa; fallback ke Groq |
| Android STT tidak tersedia di device tanpa Google Play Services | Sedang | Tinggi | Fallback ke Vosk (offline STT) atau input teks manual |
| Petani tidak nyaman bicara ke HP (adopsi rendah) | Sedang | Tinggi | Sediakan alternatif input teks; onboarding yang ramah |
| Device low-end crash dengan Compose | Rendah | Sedang | Test di device murah; optimasi recomposition; min SDK 24 |
| Firebase Spark Plan limit tercapai | Rendah | Sedang | Monitor usage; migrate ke Supabase jika perlu |
| API key tereksus di APK (Gemini) | Sedang | Sedang | Gunakan API key restriction (Android package name + SHA-1) |

## Notes
1. **BMKG API**: Data BMKG tersedia sebagai XML feed publik di `data.bmkg.go.id`. Ada juga API baru di `api.bmkg.go.id` (SATU PETA MKG). Karena dokumentasi resmi bisa berubah, selalu siapkan fallback ke Open-Meteo.
2. **Gemini API Key**: Buat di https://aistudio.google.com/apikey. Restrict key ke package name Android + SHA-1 fingerprint untuk keamanan.
3. **Firebase**: Buat project di https://console.firebase.google.com. Aktifkan Authentication (Anonymous), Firestore, dan Cloud Messaging. Semua dalam Spark Plan (gratis).
4. **VUI Design**: Prioritaskan pengalaman suara. Tombol mic harus menjadi elemen paling menonjol di layar. Pertimbangkan juga menambahkan suara feedback (beep saat mulai/berhenti merekam).
5. **Target Device**: Fokus pada device Android murah (Rp 1-2 juta) yang umum di desa: Samsung Galaxy A series, Xiaomi Redmi, Realme C series, Infinix.
6. **Bahasa**: Semua UI dalam bahasa Indonesia. Hindari istilah teknis Inggris di UI user-facing.
7. **Ukuran APK**: Target < 15 MB. Jangan bundle AI model lokal di MVP. Gunakan API cloud untuk AI.
```

---