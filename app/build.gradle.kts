import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.agribicara.app"
    // compileSdk 37 diwajibkan oleh Compose BOM 2026.08.00
    compileSdk = 37

    defaultConfig {
        applicationId = "com.agribicara.app"
        // Android 7.0 — target device murah di desa (NFR PRD)
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        // java.time dipakai mulai Fase 2; butuh desugaring karena minSdk 24
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // MigrationTestHelper membaca schema JSON dari assets androidTest.
    // Tanpa ini test migrasi gagal dengan "Cannot find the schema file".
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
}

kotlin {
    compilerOptions {
        // Samakan dengan compileOptions di atas agar tidak ada
        // "Inconsistent JVM-target compatibility" antara Java dan Kotlin.
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    // Schema Room di-commit ke git — dibutuhkan untuk migrasi di Fase 2/4/5
    arg("room.schemaLocation", "$projectDir/schemas")
}

/**
 * Coverage host test (unit test JVM).
 *
 * Kover pada Android TIDAK menghitung instrumented test. Karena itu kelas yang
 * memang diuji lewat `androidTest` WAJIB dikecualikan — kalau tidak, angkanya
 * rendah palsu dan mendorong penulisan test demi mengejar persentase, bukan
 * demi menemukan cacat. Setiap pengecualian di bawah diberi alasannya.
 */
kover {
    reports {
        filters {
            excludes {
                /*
                 * Glob harus cocok dengan SELURUH nama berkualifikasi penuh.
                 *
                 * Ini sebab sebenarnya AppDatabase_Impl (102 baris bangkitan)
                 * sempat tetap terhitung padahal sudah didaftarkan: pola
                 * "Hilt_*" menuntut nama DIMULAI dengan "Hilt_", sehingga ia
                 * tidak pernah cocok dengan com.agribicara.app.Hilt_MainActivity.
                 * Yang benar "*Hilt_*". Bandingkan "*_Impl" yang berhasil justru
                 * karena sudah diawali "*".
                 *
                 * Aturan kedua: setiap FQN butuh pasangan "$*". Tanpa itu
                 * kelasnya terkecualikan tetapi kelas lambda di dalamnya
                 * (misalnya ...$listener$1) tetap terhitung.
                 *
                 * Daftar disatukan dalam satu panggilan classes() semata demi
                 * keterbacaan, BUKAN karena panggilan ganda bermasalah. Itu
                 * sempat saya kira penyebabnya dan itu KELIRU: Kover
                 * menggabungkan panggilan classes() yang berulang. Dibuktikan
                 * dengan menambahkan panggilan kedua berisi satu kelas lalu
                 * membandingkan XML-nya -- kelas dari panggilan kedua ikut
                 * terkecualikan SEMENTARA kelas dari panggilan pertama tetap
                 * terkecualikan. Yang dulu membuat angkanya membaik adalah
                 * perbaikan glob di atas, bukan penggabungannya.
                 */
                classes(
                    // --- kode bangkitan, bukan tulisan manusia -------------
                    "*_Impl",
                    "*_Impl\$*",
                    "*_Factory",
                    "*_Factory\$*",
                    "*_MembersInjector",
                    "*_HiltModules*",
                    "*_GeneratedInjector",
                    "*Hilt_*",
                    // Berkas Compose tingkat-atas menjadi kelas FooKt.
                    // annotatedBy(Composable) menangkap fungsinya, tetapi
                    // bukan kelas pembungkus berkasnya.
                    "*ScreenKt",
                    "*ScreenKt$*",
                    "*ComponentsKt",
                    "*ComponentsKt$*",
                    "*MicIndicatorKt",
                    "*ComposableSingletons*",
                    "*_ComponentTreeDeps*",
                    "dagger.hilt.*",
                    "hilt_aggregated_deps.*",
                    "com.agribicara.app.BuildConfig",

                    // --- titik masuk Android, tanpa kebijakan di dalamnya --
                    "com.agribicara.app.MainActivity",
                    "com.agribicara.app.MainActivity$*",
                    "com.agribicara.app.AgriBicaraApp",
                    "com.agribicara.app.AgriBicaraApp$*",
                    "com.agribicara.app.data.local.AppDatabase",
                    "com.agribicara.app.data.local.AppDatabase$*",

                    // Satu-satunya kode yang menyentuh SDK Firebase AI.
                    // Sengaja dijaga sedangkal mungkin dan tidak bisa dibangun
                    // di JVM; seluruh keputusannya ada di FirebaseAiRepository
                    // yang justru punya test.
                    "com.agribicara.app.data.ai.FirebaseTextGenerator",
                    "com.agribicara.app.data.ai.FirebaseTextGenerator$*",

                    // Membungkus NotificationCompat dan PendingIntent; jalur
                    // keputusannya diuji lewat WeatherCheckWorkerTest.
                    "com.agribicara.app.data.notification.WeatherNotifier",
                    "com.agribicara.app.data.notification.WeatherNotifier$*",

                    // Wrapper SpeechRecognizer dan TextToSpeech milik Android.
                    // Kebijakannya sudah diangkat keluar ke SpeechErrorMapper
                    // dan TtsLanguageStatus, dan KEDUANYA punya unit test.
                    "com.agribicara.app.data.speech.AndroidSpeechRecognizerRepository",
                    "com.agribicara.app.data.speech.AndroidSpeechRecognizerRepository$*",
                    "com.agribicara.app.data.speech.AndroidTextToSpeechRepository",
                    "com.agribicara.app.data.speech.AndroidTextToSpeechRepository$*",
                )

                // UI Compose diuji lewat instrumented test (HomeContentTest,
                // VoiceContentTest, TouchTargetInvariantTest). Composable tidak
                // bisa dieksekusi di JVM tanpa Robolectric, yang sengaja tidak
                // dipakai project ini.
                annotatedBy("androidx.compose.runtime.Composable")

                packages(
                    // Modul DI: deklarasi penyedia, bukan kebijakan.
                    "com.agribicara.app.di",
                    // Token warna dan tipografi; kebenarannya adalah rasio
                    // kontras yang dihitung manual, bukan sesuatu yang
                    // dieksekusi test.
                    "com.agribicara.app.presentation.theme",
                    // Entity Room: data class murni tanpa perilaku.
                    "com.agribicara.app.data.local.entity",
                    // DAO dan migrasi: diuji lewat MigrationTest,
                    // SentAlertDaoTest, UserPreferenceDaoTest,
                    // WeatherCacheDaoTest, ChatMessageDaoTest, dan
                    // RegionCacheDaoTest — semuanya instrumented.
                    "com.agribicara.app.data.local.dao",
                    "com.agribicara.app.data.local.migration",
                    // Worker: butuh Context dan WorkerParameters sungguhan,
                    // diuji lewat WeatherCheckWorkerTest yang instrumented.
                    "com.agribicara.app.data.worker",
                )
            }
        }

        /*
         * Ambang 85% BARIS, bukan 80% dan bukan 91%.
         *
         * 80% adalah aturan ECC; angka terukur saat ambang ini dipasang 91,1%,
         * jadi memasang 80 berarti membiarkan coverage turun sebelas poin tanpa
         * satu pun peringatan. Memasangnya tepat di 91 juga salah arah — setiap
         * refactor sah yang menghapus baris teruji akan memerah tanpa ada yang
         * rusak. 85 memberi ruang gerak sambil tetap di atas aturan.
         *
         * SENGAJA tidak ada ambang untuk CABANG. Angkanya 57,6%, dan sebagian
         * besar cabang yang belum tersentuh adalah pemeriksaan null bangkitan
         * Kotlin di DTO. Memasang ambang cabang sekarang hanya akan memaksa
         * penulisan test terhadap cabang yang tidak pernah bisa terjadi.
         * Dicatat sebagai utang terbuka, bukan diam-diam dianggap tidak ada.
         */
        verify {
            rule {
                minBound(85, CoverageUnit.LINE)
            }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // WorkManager (Fase 6): pemeriksaan cuaca ekstrem berkala.
    // androidx.hilt:hilt-compiler DIBUTUHKAN di samping hilt-compiler Dagger di
    // atas — yang ini yang menghasilkan kode untuk @HiltWorker.
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Firebase AI Logic (Fase 5). Versi datang dari BoM di atas — sengaja tanpa versi.
    implementation(libs.firebase.ai)
    // implementation, BUKAN debugImplementation: AgriBicaraApp merujuk kelas ini di
    // balik penjagaan BuildConfig.DEBUG. Konstanta itu compile-time, jadi seluruh
    // cabangnya lenyap saat build release dan kelasnya tidak pernah ikut ter-APK.
    implementation(libs.firebase.appcheck.debug)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.timber)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.mockk.android)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
