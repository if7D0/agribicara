# Plan: Asisten Pertanian Cerdas Desa (TaniCerdas)

## Summary
TaniCerdas adalah aplikasi Android one-stop untuk petani kecil di Indonesia yang mengintegrasikan prakiraan cuaca desa-spesifik, informasi harga komoditas real-time, deteksi penyakit tanaman berbasis AI, dan kalendar tanam pintar. Aplikasi ini dirancang untuk solo developer dengan budget minimal, memanfaatkan API gratis dan open-source technologies.

## Problem → Solution
**Problem**: 33 juta+ petani kecil di Indonesia kesulitan mengakses informasi cuaca akurat level desa, harga pasar real-time, dan bantuan diagnosis penyakit tanaman — yang menyebabkan kerugian finansial dan hasil panen suboptimal.

**Solution**: Aplikasi mobile yang menggabungkan semua kebutuhan informasi pertanian dalam satu platform dengan interface yang user-friendly untuk petani dengan literasi digital rendah.

---

# BAGIAN 1: PRODUCT REQUIREMENTS DOCUMENT (PRD)

## 1.1 Informasi Produk

| Field | Detail |
|---|---|
| **Nama Produk** | TaniCerdas - Asisten Pertanian Cerdas |
| **Platform** | Android (Native Kotlin) |
| **Target User** | Petani kecil (lahan < 2 hektar), usia 25-55 tahun |
| **Bahasa UI** | Bahasa Indonesia (sederhana, tanpa istilah teknis) |
| **Offline Support** | Ya (fitur inti harus bekerja offline) |
| **Minimum Android** | Android 7.0 (API Level 24) — untuk coverage maksimal device di desa |

## 1.2 User Personas

### Persona 1: Pak Budi (Petani Padi, 45 tahun)
- **Latar**: Petani padi di Jawa Tengah, lahan 0.5 hektar
- **Pain Points**: Tidak tahu kapan waktu tanen optimal, sering tertipu tengkulak soal harga, sulit identifikasi hama
- **Tech Comfort**: Bisa WhatsApp, kadang lihat YouTube
- **Goals**: Meningkatkan hasil panen, mendapat harga jual yang adil

### Persona 2: Bu Sari (Petani Sayur, 32 tahun)
- **Latar**: Petani sayuran di dataran tinggi, lahan sewa 0.3 hektar
- **Pain Points**: Cuaca ekstrem merusak tanaman, butuh cepat identifikasi penyakit
- **Tech Comfort**: Aktif di Facebook, bisa menggunakan aplikasi mobile
- **Goals**: Mengurangi kerugian karena cuaca dan penyakit

### Persona 3: Kang Dedi (Petani Muda, 28 tahun)
- **Latar**: Lulusan SMK Pertanian, mulai usaha pertanian organik
- **Pain Points**: Butuh edukasi teknik modern, akses pasar lebih luas
- **Tech Comfort**: Cukup tinggi, aktif di media sosial
- **Goals**: Membangun usaha pertanian modern yang profitable

## 1.3 Fitur Requirements

### 1.3.1 MVP Features (Must Have - Phase 1)

#### F01: Prakiraan Cuaca Desa
| Aspect | Detail |
|---|---|
| **Priority** | P0 (Critical) |
| **Description** | Menampilkan prakiraan cuaca 3 hari ke depan untuk desa spesifik user |
| **Data Source** | BMKG API (data.bmkg.go.id) |
| **Offline Behavior** | Cache data terakhir untuk viewing offline |
| **Acceptance Criteria** | - Menampilkan suhu, kelembaban, peluang hujan, kecepatan angin<br>- Update otomatis setiap 6 jam<br>- Notifikasi jika ada peringatan dini cuaca ekstrem |

#### F02: Dashboard Harga Komoditas
| Aspect | Detail |
|---|---|
| **Priority** | P0 (Critical) |
| **Description** | Menampilkan harga real-time komoditas pertanian di pasar lokal dan tingkat petani |
| **Data Source** | Scraping/retreiving dari infoharga.bappebti.go.id dan sp2kp.kemendag.go.id |
| **Offline Behavior** | Cache data terakhir (max 24 jam) |
| **Acceptance Criteria** | - Menampilkan harga komoditas utama (padi, jagung, cabai, tomat, dll)<br>- Grafik trend harga 7 hari<br>- Perbandingan harga antar pasar terdekat |

#### F03: Deteksi Penyakit Tanaman
| Aspect | Detail |
|---|---|
| **Priority** | P0 (Critical) |
| **Description** | User upload foto tanaman, aplikasi memberikan diagnosis dan rekomendasi pengobatan |
| **Technology** | Google ML Kit Image Labeling + custom TensorFlow Lite model |
| **Offline Behavior** | Fully offline (on-device ML) |
| **Acceptance Criteria** | - Akurasi minimal 80% untuk 10 penyakit umum<br>- Memberikan rekomendasi pengobatan organik dan kimiawi<br>- Interface sederhana: foto → hasil → solusi |

#### F04: Kalendar Tanam
| Aspect | Detail |
|---|---|
| **Priority** | P1 (Important) |
| **Description** | Rekomendasi waktu tanam berdasarkan data cuaca dan jenis komoditas |
| **Data Source** | Kombinasi data cuaca BMKG + database pengetahuan lokal |
| **Offline Behavior** | Bisa diakses offline dengan data yang di-cache |
| **Acceptance Criteria** | - Menampilkan kapan waktu tanen optimal untuk komoditas tertentu<br>- Reminder notifikasi untuk jadwal tanam<br>- Berdasarkan data musim lokal |

### 1.3.2 Phase 2 Features (Should Have)

#### F05: Edukasi Pertanian
- Video dan artikel panduan budidaya per komoditas
- Konten lokal yang sesuai dengan kondisi wilayah user
- YouTube Data API untuk embed video edukasi gratis

#### F06: Pasar Terdekat
- Menampilkan lokasi pasar/kios pertanian terdekat menggunakan OpenStreetMap
- Informasi kontak dan jam operasional

#### F07: Kalkulator Kebutuhan Lahan
- Input luas lahan → output kebutuhan benih, pupuk, pestisida
- Estimasi biaya produksi dan potensi keuntungan

### 1.3.3 Phase 3 Features (Could Have)

#### F08: Forum Komunitas Petani
- Diskusi antar petani dalam aplikasi
- Sharing pengalaman dan tips

#### F09: Integrasi Marketplace
- Link ke TaniHub dan platform B2B lainnya
- Bukan fitur jual-beli langsung (terlalu kompleks untuk MVP)

## 1.4 Non-Functional Requirements

| Requirement | Target |
|---|---|
| **App Size** | < 30 MB (untuk hemat storage dan download di jaringan lambat) |
| **Performance** | Cold start < 3 detik di device low-end (2GB RAM) |
| **Battery Usage** | Minimal (no background GPS tracking by default) |
| **Network** | Optimized for 3G/4G dengan data usage minimal |
| **Offline** | Fitur inti harus berfungsi dengan koneksi intermittent |
| **Accessibility** | Font size besar, high contrast mode, ikon intuitif |
| **Language** | Bahasa Indonesia sederhana, tanpa istilah teknis |

## 1.5 Success Metrics

| Metric | Target MVP (3 bulan) | Target 12 bulan |
|---|---|---|
| Downloads | 1.000 | 50.000 |
| DAU (Daily Active Users) | 100 | 5.000 |
| Retention (7-day) | 20% | 35% |
| Feature Usage (Cuaca) | 60% dari DAU | 70% |
| Feature Usage (Harga) | 50% dari DAU | 65% |
| Feature Usage (Deteksi) | 30% dari DAU | 50% |
| Rating Play Store | 4.0+ | 4.5+ |

---

# BAGIAN 2: TECHNICAL ARCHITECTURE

## 2.1 System Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    TaniCerdas Android App                    │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌────────────┐ │
│  │  Presentation    │  │    Domain       │  │   Data     │ │
│  │    Layer         │  │    Layer        │  │   Layer    │ │
│  │                  │  │                 │  │            │ │
│  │  - Activities    │  │  - Use Cases    │  │  - Repos   │ │
│  │  - Fragments     │  │  - Entities     │  │  - APIs    │ │
│  │  - ViewModels    │  │  - Interfaces   │  │  - Cache   │ │
│  │  - Adapters      │  │                 │  │  - Local   │ │
│  └─────────────────┘  └─────────────────┘  └────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ (Hanya untuk sync & auth)
                              ▼
                    ┌──────────────────┐
                    │   Firebase       │
                    │   - Auth         │
                    │   - Firestore    │
                    │   - Analytics    │
                    └──────────────────┘
```

**Architecture Pattern**: Clean Architecture (MVVM + Use Cases)
- **Presentation Layer**: UI components (Activities/Fragments) + ViewModels
- **Domain Layer**: Business logic + Use Cases + Entities
- **Data Layer**: Repositories + Data sources (Remote/Local)

## 2.2 Tech Stack Utama

### 2.2.1 Frontend (Android Native)

| Technology | Purpose | License | Free Tier |
|---|---|---|---|
| **Kotlin** | Primary language | Open source (Apache 2.0) | Gratis |
| **Jetpack Compose** | UI toolkit | Open source | Gratis |
| **Coroutines** | Async operations | Open source | Gratis |
| **Flow** | Reactive data streams | Open source | Gratis |
| **ViewModel** | Lifecycle-aware UI state | Open source | Gratis |
| **Navigation Compose** | In-app navigation | Open source | Gratis |
| **Hilt** | Dependency injection | Open source | Gratis |
| **Room** | Local database | Open source | Gratis |
| **DataStore** | Key-value storage | Open source | Gratis |
| **WorkManager** | Background tasks | Open source | Gratis |
| **Coil** | Image loading | Open source | Gratis |

### 2.2.2 External APIs (Gratis)

| API | Purpose | Free Tier | Documentation |
|---|---|---|---|
| **BMKG API** | Prakiraan cuaca, gempa, peringatan dini | Unlimited (government open data) | data.bmkg.go.id |
| **Bappebti Data** | Harga komoditas tingkat petani | Unlimited (public data) | infoharga.bappebti.go.id |
| **SP2KP Kemendag** | Harga pangan strategis | Unlimited (public data) | sp2kp.kemendag.go.id |
| **OpenStreetMap** | Peta dan lokasi | Unlimited | openstreetmap.org |
| **Google ML Kit** | Image labeling, text recognition | Gratis (on-device) | developers.google.com/ml-kit |

### 2.2.3 Backend Services (Firebase - Gratis)

| Service | Purpose | Free Tier Limit |
|---|---|---|
| **Firebase Auth** | User authentication | Unlimited users |
| **Cloud Firestore** | Cloud database | 1GB storage, 50K reads/day, 20K writes/day |
| **Firebase Analytics** | App analytics | Unlimited events |
| **Firebase Crashlytics** | Crash reporting | Unlimited |
| **Cloud Messaging (FCM)** | Push notifications | Unlimited messages |

## 2.3 Alternatif Teknologi (Jika Teknologi Utama Berbayar/Masalah)

### 2.3.1 Backend Alternatives (Jika Firebase Tidak Cukup)

| Primary | Alternative 1 | Alternative 2 | Notes |
|---|---|---|---|
| Firebase Auth | Supabase Auth | Appwrite Auth | Keduanya punya free tier generous |
| Firestore | Supabase PostgreSQL | MongoDB Atlas (free 512MB) | SQL vs NoSQL |
| FCM | OneSignal (free tier) | - | OneSignal free unlimited push |

### 2.3.2 Weather API Alternatives

| Primary | Alternative | Free Tier | Notes |
|---|---|---|---|
| BMKG API | OpenWeatherMap | 1,000 calls/day free | OpenWeatherMap lebih akurat global |
| BMKG API | WeatherAPI.com | 1M calls/month free | Feature lengkap |
| BMKG API | Meteosource | 100 calls/day free | Untuk fallback |

### 2.3.3 Maps Alternatives

| Primary | Alternative | Free Tier | Notes |
|---|---|---|---|
| OpenStreetMap | Mapbox | 50K MAU free | UI lebih bagus |
| OpenStreetMap | Leaflet | Unlimited | Leaflet + OSM tiles |

### 2.3.4 ML Alternatives

| Primary | Alternative | Free Tier | Notes |
|---|---|---|---|
| Google ML Kit | TensorFlow Lite | Open source | Butuh custom model training |
| Google ML Kit | ONNX Runtime | Open source | Cross-platform |

## 2.4 Database Schema

### 2.4.1 Local Database (Room)

```kotlin
// Weather Cache Table
@Entity(tableName = "weather_cache")
data class WeatherCache(
    @PrimaryKey val desaId: String,
    val temperature: Double,
    val humidity: Double,
    val rainfallChance: Double,
    val windSpeed: Double,
    val lastUpdated: Long,
    val jsonPayload: String // Full JSON response untuk offline
)

// Commodity Price Table
@Entity(tableName = "commodity_prices")
data class CommodityPrice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val commodityName: String,
    val price: Double,
    val unit: String,
    val marketName: String,
    val province: String,
    val date: String, // ISO format
    val source: String // "bappebti" or "kemendag"
)

// User Profile Table
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val userId: String,
    val name: String,
    val desaId: String,
    val desaName: String,
    val province: String,
    val mainCommodities: List<String>,
    val landSize: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// Plant Disease History Table
@Entity(tableName = "detection_history")
data class DetectionHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imageUri: String, // Local storage path
    val detectedDisease: String,
    val confidence: Double,
    val recommendation: String,
    val timestamp: Long = System.currentTimeMillis()
)
```

### 2.4.2 Firestore Collections (Cloud)

```
users/{userId}
├── profile: { name, desaId, desaName, province, commodities[], landSize }
├── settings: { notifications, language, theme }
└── subscription: { tier: "free" | "premium", expiresAt }

admin/commodities
├── {commodityId}: { name, unit, activeMarkets[] }

admin/disease_catalog
├── {diseaseId}: { name, symptoms, treatment_organic, treatment_chemical, imageRef }
```

## 2.5 Folder Structure

```
TaniCerdas/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/tanicerdas/app/
│   │   │   │   ├── TaniCerdasApplication.kt
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── di/  (Dependency Injection)
│   │   │   │   │   ├── AppModule.kt
│   │   │   │   │   ├── NetworkModule.kt
│   │   │   │   │   └── DatabaseModule.kt
│   │   │   │   ├── presentation/  (UI Layer)
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── weather/
│   │   │   │   │   │   │   ├── WeatherScreen.kt
│   │   │   │   │   │   │   ├── WeatherViewModel.kt
│   │   │   │   │   │   │   └── components/
│   │   │   │   │   │   │       ├── WeatherCard.kt
│   │   │   │   │   │   │   └── HourlyForecast.kt
│   │   │   │   │   │   ├── prices/
│   │   │   │   │   │   │   ├── PricesScreen.kt
│   │   │   │   │   │   │   ├── PricesViewModel.kt
│   │   │   │   │   │   │   └── components/
│   │   │   │   │   │   │       ├── CommodityCard.kt
│   │   │   │   │   │   │       └── PriceTrendChart.kt
│   │   │   │   │   │   ├── detection/
│   │   │   │   │   │   │   ├── DetectionScreen.kt
│   │   │   │   │   │   │   ├── DetectionViewModel.kt
│   │   │   │   │   │   │   └── components/
│   │   │   │   │   │   │       ├── CameraCapture.kt
│   │   │   │   │   │   │       └── ResultDialog.kt
│   │   │   │   │   │   ├── calendar/
│   │   │   │   │   │   │   ├── CalendarScreen.kt
│   │   │   │   │   │   │   └── CalendarViewModel.kt
│   │   │   │   │   │   ├── onboarding/
│   │   │   │   │   │   │   ├── OnboardingScreen.kt
│   │   │   │   │   │   │   └── LocationPicker.kt
│   │   │   │   │   │   └── settings/
│   │   │   │   │   │       ├── SettingsScreen.kt
│   │   │   │   │   │       └── ProfileScreen.kt
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   └── NavGraph.kt
│   │   │   │   │   └── components/  (Shared UI)
│   │   │   │   │       ├── LoadingIndicator.kt
│   │   │   │   │       ├── ErrorState.kt
│   │   │   │   │       └── OfflineBanner.kt
│   │   │   │   ├── domain/  (Business Logic)
│   │   │   │   │   ├── models/
│   │   │   │   │   │   ├── Weather.kt
│   │   │   │   │   │   ├── CommodityPrice.kt
│   │   │   │   │   │   ├── PlantDisease.kt
│   │   │   │   │   │   └── User.kt
│   │   │   │   │   ├── repositories/
│   │   │   │   │   │   ├── WeatherRepository.kt
│   │   │   │   │   │   ├── PriceRepository.kt
│   │   │   │   │   │   ├── DetectionRepository.kt
│   │   │   │   │   │   └── UserRepository.kt
│   │   │   │   │   └── usecases/
│   │   │   │   │       ├── GetWeatherForecast.kt
│   │   │   │   │       ├── GetCommodityPrices.kt
│   │   │   │   │       ├── DetectPlantDisease.kt
│   │   │   │   │       └── SaveUserProfile.kt
│   │   │   │   └── data/  (Data Layer)
│   │   │   │       ├── remote/
│   │   │   │       │   ├── api/
│   │   │   │       │   │   ├── BMKGApiService.kt
│   │   │   │       │   │   ├── BappebtiApiService.kt
│   │   │   │       │   │   └── MLKitService.kt
│   │   │   │       │   ├── dto/
│   │   │   │       │   │   ├── WeatherResponse.kt
│   │   │   │       │   │   └── PriceResponse.kt
│   │   │   │       │   └── interceptor/
│   │   │   │       │       └── CacheInterceptor.kt
│   │   │   │       ├── local/
│   │   │   │       │   ├── database/
│   │   │   │       │   │   ├── TaniCerdasDatabase.kt
│   │   │   │       │   │   ├── dao/
│   │   │   │       │   │   │   ├── WeatherDao.kt
│   │   │   │       │   │   │   ├── PriceDao.kt
│   │   │   │       │   │   │   └── UserDao.kt
│   │   │   │       │   │   └── converter/
│   │   │   │       │   │       └── Converters.kt
│   │   │   │       │   ├── preferences/
│   │   │   │       │   │   └── UserPreferences.kt
│   │   │   │       │   └── ml/
│   │   │   │       │       └── DiseaseClassifier.kt
│   │   │   │       └── repository/
│   │   │   │           ├── WeatherRepositoryImpl.kt
│   │   │   │           ├── PriceRepositoryImpl.kt
│   │   │   │           ├── DetectionRepositoryImpl.kt
│   │   │   │           └── UserRepositoryImpl.kt
│   │   │   ├── res/
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   ├── drawable/
│   │   │   │   ├── layout/  (jika masih pakai XML untuk beberapa komponen)
│   │   │   │   └── values-night/  (Dark mode)
│   │   │   └── AndroidManifest.xml
│   │   ├── test/  (Unit Tests)
│   │   │   └── java/com/tanicerdas/app/
│   │   │       ├── domain/
│   │   │       │   ├── usecases/
│   │   │       │   │   ├── GetWeatherForecastTest.kt
│   │   │       │   │   └── GetCommodityPricesTest.kt
│   │   │       │   └── repositories/
│   │   │       └── data/
│   │   │           ├── repository/
│   │   │           │   └── WeatherRepositoryImplTest.kt
│   │   │           └── local/
│   │   │               └── dao/
│   │   │                   └── WeatherDaoTest.kt
│   │   └── androidTest/  (UI Tests)
│   │       └── java/com/tanicerdas/app/
│   │           ├── presentation/
│   │           │   └── screens/
│   │           │       ├── weather/
│   │           │       │   └── WeatherScreenTest.kt
│   │           │       └── prices/
│   │           │           └── PricesScreenTest.kt
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml  (Version catalog)
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## 2.6 Key Dependencies (build.gradle.kts)

```kotlin
// gradle/libs.versions.toml
[versions]
kotlin = "2.0.0"
compose = "2024.09.00"
retrofit = "2.11.0"
room = "2.6.1"
hilt = "2.51.1"
coroutines = "1.8.1"
lifecycle = "2.8.4"
mlkit = "17.0.1"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-navigation = { group = "androidx.navigation", name = "navigation-compose", version = "2.8.0" }

# Lifecycle
lifecycle-viewmodel = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }

# Dependency Injection
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }

# Network
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name: "okhttp", version = "4.12.0" }
okhttp-logging = { group = "com.squareup.okhttp3", name: "logging-interceptor", version = "4.12.0" }

# Database
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Coroutines
coroutines-android = { group = "org.jetbrains.kotlinx", name: "kotlinx-coroutines-android", version.ref = "coroutines" }

# Firebase
firebase-bom = { group = "com.google.firebase", name: "firebase-bom", version = "33.1.2" }
firebase-auth = { group = "com.google.firebase", name: "firebase-auth-ktx" }
firebase-firestore = { group = "com.google.firebase", name: "firebase-firestore-ktx" }
firebase-analytics = { group = "com.google.firebase", name: "firebase-analytics-ktx" }
firebase-crashlytics = { group = "com.google.firebase", name: "firebase-crashlytics-ktx" }
firebase-messaging = { group = "com.google.firebase", name: "firebase-messaging-ktx" }

# ML Kit
mlkit-image-labeling = { group = "com.google.mlkit", name: "image-labeling", version.ref = "mlkit" }
mlkit-text-recognition = { group = "com.google.mlkit", name: "text-recognition", version.ref = "mlkit" }

# Image Loading
coil-compose = { group = "io.coil-kt", name: "coil-compose", version = "2.7.0" }

# DataStore
datastore-preferences = { group = "androidx.datastore", name: "datastore-preferences", version = "1.1.1" }

# WorkManager
work-runtime = { group = "androidx.work", name: "work-runtime-ktx", version = "2.9.1" }

[plugins]
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
```

---

# BAGIAN 3: DEVELOPMENT ROADMAP

## 3.1 Timeline Overview

```
Bulan 1-2: Foundation & MVP Core
├── Week 1-2: Project setup, architecture, authentication
├── Week 3-4: Weather feature (BMKG API integration)
├── Week 5-6: Commodity prices (data scraping/API)
└── Week 7-8: Plant disease detection (ML Kit)

Bulan 3: Polish & Testing
├── Week 9-10: Calendar feature, offline support
├── Week 11-12: Testing, bug fixes, performance optimization
└── Week 13: Play Store submission preparation

Bulan 4+: Launch & Iteration
├── Soft launch (1 provinsi)
├── User feedback collection
└── Phase 2 features development
```

## 3.2 Detailed Task Breakdown

### Phase 1: Foundation (Week 1-2)

#### Task 1: Project Setup
- **ACTION**: Initialize Android project with Kotlin + Jetpack Compose
- **IMPLEMENT**: 
  - Create project dengan template "Empty Activity (Compose)"
  - Setup version catalog (libs.versions.toml)
  - Configure build.gradle.kts dengan semua dependencies
  - Setup ProGuard rules untuk release build
- **VALIDATE**: Project builds successfully dengan `./gradlew assembleDebug`

#### Task 2: Architecture Setup
- **ACTION**: Implement Clean Architecture structure
- **IMPLEMENT**:
  - Create package structure (presentation, domain, data)
  - Setup Hilt dependency injection modules
  - Configure Room database dengan initial entities
  - Setup Retrofit untuk API calls
- **VALIDATE**: DI graph valid, app runs tanpa crash

#### Task 3: Authentication & Onboarding
- **ACTION**: Implement user registration/login dengan Firebase Auth
- **IMPLEMENT**:
  - Anonymous authentication (untuk MVP, tanpa email/password)
  - Onboarding screen: pilih desa (dropdown dari API BMKG)
  - Simpan user profile ke Firestore & Room
- **VALIDATE**: User bisa login, profil tersimpan, bisa logout

### Phase 2: Core Features (Week 3-8)

#### Task 4: Weather Feature
- **ACTION**: Implement weather display dengan BMKG API
- **IMPLEMENT**:
  ```kotlin
  // BMKG API endpoint: https://api.bmkg.go.id/publik/prakiraan-cuaca?adm4={desaId}
  // Response: JSON dengan temperature, humidity, rainfall, dll
  
  interface BMKGApiService {
      @GET("publik/prakiraan-cuaca")
      suspend fun getWeatherForecast(
          @Query("adm4") desaId: String
      ): Response<WeatherResponse>
  }
  ```
  - Create WeatherRepository dengan offline caching
  - Build WeatherScreen dengan Compose (current + 3-day forecast)
  - Implement WorkManager untuk periodic refresh (setiap 6 jam)
  - FCM notification untuk peringatan cuaca ekstrem
- **GOTCHA**: BMKG API kadang lambat, implement timeout dan retry logic
- **VALIDATE**: 
  - Cuaca tampil dengan data real dari BMKG
  - Bekerja offline (cache dari request terakhir)
  - Auto-refresh bekerja

#### Task 5: Commodity Prices
- **ACTION**: Implement commodity price dashboard
- **IMPLEMENT**:
  - Create scraper/client untuk Bappebti & Kemendag data
  - Build PriceRepository dengan caching strategy (24 hours)
  - PriceScreen dengan list komoditas dan detail view
  - Price trend chart (gunakan Vico library - free)
- **GOTCHA**: 
  - Data Bappebti mungkin tidak punya API public, perlu web scraping
  - Implement rate limiting dan respect robots.txt
- **VALIDATE**:
  - Harga komoditas utama tampil dan update
  - Chart trend 7 hari bekerja
  - Offline mode menampilkan data terakhir

#### Task 6: Plant Disease Detection
- **ACTION**: Implement ML-based disease detection
- **IMPLEMENT**:
  - Setup ML Kit Image Labeling dengan custom labels
  - Create DiseaseClassifier yang process image dan return result
  - Camera capture atau gallery picker
  - Result screen dengan diagnosis dan rekomendasi
  - Save detection history ke Room
- **GOTCHA**:
  - ML Kit butuh model yang trained, untuk MVP bisa pakai general labels
  - Konsiderasi lighting conditions - implement guidance untuk user
- **VALIDATE**:
  - Foto bisa diambil/dipilih
  - ML memberikan hasil (minimal akurasi 80% untuk testing)
  - Hasil ditampilkan dengan rekomendasi pengobatan

### Phase 3: Polish & Launch (Week 9-13)

#### Task 7: Planting Calendar
- **ACTION**: Implement planting calendar recommendation
- **IMPLEMENT**:
  - Create database komoditas dengan planting seasons
  - Combine dengan weather data untuk recommendation
  - Calendar UI dengan Compose
  - Reminder notification via FCM
- **VALIDATE**: Rekomendasi tanam sesuai dengan data cuaca dan musim

#### Task 8: Offline Optimization
- **ACTION**: Ensure all core features work offline
- **IMPLEMENT**:
  - Implement comprehensive caching strategy
  - Offline banner ketika tidak ada koneksi
  - Sync mechanism ketika koneksi kembali
- **VALIDATE**: App masih berguna tanpa internet (fitur inti)

#### Task 9: Testing & QA
- **ACTION**: Comprehensive testing
- **IMPLEMENT**:
  - Unit tests untuk business logic
  - UI tests dengan Compose Testing
  - Performance testing di low-end device
  - Battery usage optimization
- **VALIDATE**: 
  - Crash-free rate > 99%
  - ANR rate < 0.47%
  - Cold start < 3 detik

#### Task 10: Play Store Preparation
- **ACTION**: Prepare for Play Store submission
- **IMPLEMENT**:
  - App icon dan screenshots
  - Privacy policy (wajib untuk Play Store)
  - Content rating questionnaire
  - AAB (Android App Bundle) build
- **VALIDATE**: AAB lolos pre-launch report

---

# BAGIAN 4: TESTING STRATEGY

## 4.1 Unit Tests

| Test | Input | Expected Output | Edge Case |
|---|---|---|---|
| GetWeatherForecast | Valid desaId "31.71.01.1001" | Weather object dengan valid data | Invalid desaId |
| GetWeatherForecast | Network timeout | Cached data or error message | No cache available |
| GetCommodityPrices | Valid market "Cianjur" | List of prices | Market not found |
| DetectPlantDisease | Image with clear disease | Disease name + confidence | Blurry image |
| SaveUserProfile | Valid user data | User saved to both Room & Firestore | Network error |

## 4.2 UI Tests (Compose Testing)

```kotlin
class WeatherScreenTest {
    @Test
    fun `weather data displayed when loaded`() {
        composeTestRule.setContent {
            WeatherScreen(viewModel = fakeViewModel)
        }
        
        composeTestRule
            .onNodeWithText("Suhu")
            .assertExists()
        
        composeTestRule
            .onNodeWithText("28°C")
            .assertExists()
    }
    
    @Test
    fun `error message shown when network fails`() {
        // Test implementation
    }
    
    @Test
    fun `offline banner visible when no connection`() {
        // Test implementation
    }
}
```

## 4.3 Performance Testing Checklist

- [ ] Cold start < 3 detik di device 2GB RAM
- [ ] Memory usage < 150MB di normal operation
- [ ] Battery drain minimal (no unnecessary background work)
- [ ] Network usage optimized (compressed images, minimal API calls)
- [ ] APK/AAB size < 30MB

---

# BAGIAN 5: RISK ANALYSIS

## 5.1 Technical Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| BMKG API tidak stabil/downtime | Medium | High | Implement fallback ke OpenWeatherMap free tier |
| Data Bappebti tidak accessible via API | High | Medium | Implement web scraping dengan proper rate limiting |
| ML Kit akurasi rendah untuk penyakit lokal | Medium | High | Training custom model dengan dataset lokal (PlantVillage dataset sebagai starting point) |
| Firebase free tier limits tercapai | Low | Medium | Monitor usage, siapkan migration plan ke Supabase |
| Device fragmentation di desa (old Android) | High | Medium | Test di berbagai device, support minimum API 24 |

## 5.2 Market Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Petani tidak adopt teknologi | Medium | High | Design dengan UX yang sangat sederhana, target petani muda dulu |
| Kompetitor besar masuk (TaniHub expand ke B2C) | Medium | Medium | Focus pada niche: desa-desa yang tidak dilayani platform besar |
| Monetisasi sulit | High | Medium | Freemium model, target 5% conversion rate ke premium |

---

# BAGIAN 6: VALIDATION COMMANDS

## 6.1 Build & Deploy

```bash
# Debug build
./gradlew assembleDebug

# Release build (AAB)
./gradlew bundleRelease

# Install di device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run UI tests
./gradlew connectedAndroidTest

# Check lint
./gradlew lint
```

## 6.2 Manual Testing Checklist

- [ ] Onboarding: User bisa pilih desa dan disimpan
- [ ] Weather: Data cuaca tampil dan akurat
- [ ] Weather: Auto-refresh setelah 6 jam
- [ ] Prices: Harga komoditas tampil dan update
- [ ] Prices: Chart trend bekerja
- [ ] Detection: Foto bisa diambil dan diproses
- [ ] Detection: Hasil ditampilkan dengan rekomendasi
- [ ] Offline: Semua fitur inti bekerja tanpa internet
- [ ] Offline: Banner "Tidak ada koneksi" muncul
- [ ] Notifications: Reminder cuaca ekstrem bekerja
- [ ] Performance: App lancar di device low-end

---

# BAGIAN 7: MONETIZATION ROADMAP

## 7.1 Phase 1: Growth (Bulan 1-6)
- **Model**: Free dengan ads (Google AdMob)
- **Goal**: User acquisition dan retention
- **Metrics**: DAU, retention rate, crash-free rate

## 7.2 Phase 2: Premium Features (Bulan 7-12)
- **Model**: Freemium dengan premium subscription
- **Features**: 
  - Analisis lahan mendalam
  - Rekomendasi pupuk custom
  - Priority access ke data
  - No ads
- **Pricing**: Rp 15.000-25.000/bulan

## 7.3 Phase 3: B2B2C (Tahun 2+)
- **Model**: Partnership dengan koperasi, pemerintah desa, atau agri-business
- **Features**: Dashboard untuk organization, bulk pricing, white-label options

---

# BAGIAN 8: ESTIMATED COSTS

## 8.1 Development Costs (Solo Developer)

| Item | Cost | Notes |
|---|---|---|
| Android Studio | Rp 0 | Free |
| Google Play Developer Account | Rp 350.000 (one-time) | Wajib untuk publish |
| Firebase | Rp 0 | Free tier untuk MVP |
| BMKG API | Rp 0 | Government open data |
| ML Kit | Rp 0 | Free |
| Testing Devices | Rp 2.000.000 (optional) | 1 device low-end untuk testing |

**Total MVP Investment**: ~Rp 350.000 - Rp 2.350.000

## 8.2 Operational Costs (Monthly)

| Service | Free Tier Limit | If Exceed |
|---|---|---|
| Firebase | 1GB storage, 50K reads/day | $0.10/100K reads |
| AdMob | Free | Revenue share (30% Google) |
| Play Store | $25 one-time | - |

**Estimated Monthly Cost**: Rp 0 - Rp 500.000 (tergantung user growth)

---

# APPENDIX: EXTERNAL RESOURCES

## API Documentation Links
1. **BMKG API**: https://data.bmkg.go.id/prakiraan-cuaca
2. **Bappebti Harga Komoditas**: https://infoharga.bappebti.go.id/harga_komoditi_petani
3. **Kemendag SP2KP**: https://sp2kp.kemendag.go.id
4. **Google ML Kit**: https://developers.google.com/ml-kit
5. **Firebase**: https://firebase.google.com/docs

## Learning Resources
1. **Jetpack Compose**: https://developer.android.com/jetpack/compose
2. **Clean Architecture**: https://developer.android.com/topic/architecture
3. **Room Database**: https://developer.android.com/training/data-storage/room
4. **Hilt DI**: https://developer.android.com/training/dependency-injection/hilt-android

## Open Source Projects for Reference
1. **Weather App Example**: https://github.com/android/weather-samples
2. **Plant Disease Detection**: https://github.com/spMohanty/PlantVillage-Dataset
3. **Clean Architecture Android**: https://github.com/android10/Android-CleanArchitecture

---

*Document Version: 1.0*
*Last Updated: 2025-01-XX*
*Created by: AI Project Manager (Claude)*
*Status: Ready for Implementation*