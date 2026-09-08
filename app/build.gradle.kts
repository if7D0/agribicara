import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
// WAJIB di-import, tidak bisa ditulis lengkap sebagai java.util.Properties():
// di dalam skrip Kotlin DSL, `java` lebih dulu resolve ke accessor extension
// milik Gradle (JavaPluginExtension), sehingga `java.util` gagal dengan
// "Unresolved reference 'util'" — pesan yang sama sekali tidak menyinggung
// bahwa penyebabnya adalah tabrakan nama.
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.kover)
}

/**
 * Material penanda tangan build rilis (Fase 8).
 *
 * Berkasnya SENGAJA tidak ada di repo — `keystore.properties` dan `*.jks` sudah
 * masuk .gitignore sejak Fase 1. Templatnya ada di `keystore.properties.example`.
 *
 * Dibaca dengan penjagaan `exists()` karena CI menjalankan `assembleRelease`
 * tanpa punya keystore sama sekali (lihat .github/workflows/ci.yml). Tanpa
 * penjagaan itu CI merah dengan pesan yang tidak menyinggung signing sedikit pun.
 */
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
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
        /*
         * versionCode TIDAK BOLEH turun atau diulang, selamanya, untuk satu
         * applicationId. Sekali sebuah angka terunggah ke track mana pun —
         * internal testing sekalipun — angka itu hangus permanen. Naikkan +1
         * setiap unggahan, bukan setiap rilis.
         *
         * Masih 1 karena belum pernah ada unggahan ke Play sama sekali.
         */
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Dibuat HANYA bila keystore-nya benar-benar ada. Mendaftarkan
        // signingConfig yang menunjuk berkas tidak ada membuat build gagal saat
        // eksekusi, bukan saat konfigurasi — jadi gejalanya muncul jauh dari
        // sebabnya.
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            // findByName, BUKAN getByName: yang kedua MELEMPAR saat config-nya
            // tidak dibuat. null berarti build rilis tetap unsigned — persis
            // perilaku yang dibutuhkan CI, dan persis perilaku sebelum Fase 8.
            signingConfig = signingConfigs.findByName("release")

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
 * Berkas yang dibaca LicenseNoticeInvariantTest lewat jalur filesystem, bukan
 * classpath, sehingga Gradle tidak bisa menemukannya sendiri.
 *
 * Tanpa deklarasi ini task test dianggap up-to-date ketika `ml/NOTICE` atau
 * aset atribusinya berubah, dan penjaga lisensi itu dilewati DIAM-DIAM persis
 * pada saat ia dibutuhkan. Diverifikasi: mengubah aset tanpa baris ini tidak
 * memerahkan apa pun sampai `--rerun-tasks` dipaksakan.
 */
tasks.withType<Test>().configureEach {
    inputs.file(rootProject.file("ml/NOTICE"))
        .withPropertyName("mlNotice")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(layout.projectDirectory.file("src/main/assets/paddy_doctor_notice.txt"))
        .withPropertyName("paddyDoctorNoticeAsset")
        .withPathSensitivity(PathSensitivity.RELATIVE)
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
                 * Aturan ketiga, kosmetik tetapi menular: tulis "$*" TANPA
                 * backslash. Kotlin memperlakukan "$" yang tidak diikuti
                 * identifier atau "{" sebagai karakter biasa, sehingga "\$*"
                 * dan "$*" menghasilkan literal yang IDENTIK. Dulu dua gaya
                 * hidup berdampingan di daftar ini dan pembacanya wajar
                 * menduga ada bedanya. Diverifikasi Fase 9 dengan menjalankan
                 * koverXmlReportDebug pada kedua bentuk: LINE missed=128
                 * covered=1138, sama persis.
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
                    "*_Impl$*",
                    "*_Factory",
                    "*_Factory$*",
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

                    // Fase 8. Alasan yang persis sama dengan FirebaseTextGenerator
                    // di atas: menyentuh SDK Firebase, tidak bisa dibangun di JVM,
                    // dan sengaja dangkal. Seluruh keputusannya ada di
                    // CrashLogPolicy, yang justru punya test.
                    //
                    // AppCheckProviderChoice dan CrashLogPolicy TIDAK ada di daftar
                    // ini, dan itu disengaja — keduanya objek murni yang punya unit
                    // test sendiri. Mengecualikan mereka akan menyembunyikan justru
                    // kode yang paling perlu diukur.
                    "com.agribicara.app.data.logging.CrashReportingTree",
                    "com.agribicara.app.data.logging.CrashReportingTree$*",

                    // Fase 4. Menyentuh SDK Firebase ML + TFLite Interpreter dan
                    // meng-decode Uri gambar — tidak bisa dibangun di JVM. Sengaja
                    // dangkal: seluruh kebijakan (ambang keyakinan, pemetaan label)
                    // ada di DiseaseClassificationPolicy dan DiseaseCatalog yang
                    // JUSTRU punya unit test dan TIDAK dikecualikan.
                    "com.agribicara.app.data.ml.TfliteImageClassifier",
                    "com.agribicara.app.data.ml.TfliteImageClassifier$*",

                    // Membungkus NotificationCompat dan PendingIntent; jalur
                    // keputusannya diuji lewat WeatherCheckWorkerTest.
                    "com.agribicara.app.data.notification.WeatherNotifier",
                    "com.agribicara.app.data.notification.WeatherNotifier$*",

                    // Fase 9. KELASNYA saja: delegate DataStore terikat pada
                    // Context dan menulis berkas sungguhan, tidak bisa
                    // dijalankan di JVM.
                    //
                    // `NotificationPromptStoreKt` SENGAJA TIDAK dikecualikan
                    // lagi (temuan H1/M1 tinjauan Fase 9). Kebijakan kegagalan
                    // baca tinggal di sana sebagai operator `tanpaGagalBaca()`
                    // dan diuji NotificationPromptStoreTest — mengecualikannya
                    // akan menyembunyikan justru bagian yang berperilaku.
                    //
                    // Pembenaran versi pertama berbunyi "yang berperilaku
                    // adalah HomeViewModel, dan itu diuji". Itu TIDAK benar
                    // saat ditulis: jalur ini tidak punya satu pun test sampai
                    // tinjauan menemukannya. Sekarang punya empat di
                    // HomeViewModelTest.
                    "com.agribicara.app.data.local.NotificationPromptStore",
                    "com.agribicara.app.data.local.NotificationPromptStore$*",

                    // Fase 9. Hanya membangun NotificationChannel lalu
                    // menyerahkannya ke NotificationManager — tidak ada
                    // keputusan di dalamnya, dan keduanya tidak bisa dijalankan
                    // di JVM. Sebaris alasannya dengan WeatherNotifier di atas.
                    "com.agribicara.app.data.notification.WeatherAlertChannel",
                    "com.agribicara.app.data.notification.WeatherAlertChannel$*",

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
    // Fase 8. Pasangan rilis dari baris di atas: tanpa ini build rilis tidak punya
    // provider App Check sama sekali dan SETIAP panggilan Gemini ditolak runtime.
    implementation(libs.firebase.appcheck.playintegrity)
    // Fase 8. Crash dan log WARN/ERROR dari build rilis.
    implementation(libs.firebase.crashlytics)
    // Fase 4. Runtime TFLite (Interpreter). Model di-bundel di assets aplikasi
    // (Firebase ML Model Hosting deprecated). Bukan diatur BoM — versi eksplisit.
    // Pra-proses gambar dilakukan manual (Bitmap -> ByteBuffer) tanpa
    // tensorflow-lite-support: support 0.4.4 bentrok namespace di AGP 9.
    implementation(libs.tensorflow.lite)

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
