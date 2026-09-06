package com.agribicara.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.agribicara.app.data.appcheck.AppCheckProvider
import com.agribicara.app.data.appcheck.AppCheckProviderChoice
import com.agribicara.app.data.logging.CrashReportingTree
import com.agribicara.app.data.worker.WeatherCheckScheduler
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class AgriBicaraApp : Application(), Configuration.Provider {

    /**
     * Pabrik Worker milik Hilt.
     *
     * Tanpa ini, [com.agribicara.app.data.worker.WeatherCheckWorker] tidak bisa
     * dibuat karena konstruktornya menerima use case hasil injeksi.
     */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var weatherCheckScheduler: WeatherCheckScheduler

    /**
     * WorkManager memakai konfigurasi ini, bukan bawaannya.
     *
     * Ini SETENGAH dari syaratnya. Setengah lagi ada di AndroidManifest, yang
     * harus menghapus `androidx.startup.InitializationProvider`. Kalau provider
     * itu dibiarkan, WorkManager sudah terlanjur terinisialisasi dengan pabrik
     * bawaan sebelum Hilt sempat menyuntik apa pun — dan gejalanya menyesatkan:
     * BUILD-NYA TETAP SUKSES, kegagalannya hanya muncul saat worker benar-benar
     * dijalankan.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Debug mencetak ke logcat; rilis meneruskan WARN/ERROR/ASSERT ke
        // Crashlytics. Keduanya eksklusif: menanam DebugTree di rilis akan
        // membocorkan isi log ke logcat perangkat siapa pun yang menyambungkan
        // kabel. Jangan log data pribadi di rilis — pertanyaan petani adalah
        // kalimat bebas yang bisa memuat apa saja.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }

        installAppCheck()

        // Aman dipanggil setiap peluncuran; kebijakan KEEP menjaga jadwal yang
        // sudah berjalan agar tidak dimulai ulang.
        weatherCheckScheduler.schedule()
    }

    /**
     * App Check ditegakkan otomatis untuk Firebase AI Logic sejak awal Juli 2026:
     * tanpa provider yang sah, SETIAP panggilan Gemini ditolak saat runtime
     * meskipun build sukses.
     *
     * Fase 5 hanya memasang debug provider. Saat pertama dijalankan, provider ini
     * mencetak sebuah debug token ke logcat (tag `DebugAppCheckProvider`) yang
     * harus didaftarkan manual di Firebase console → App Check → Manage debug
     * tokens. Token itu terikat pada instalasi, jadi tiap perangkat uji baru
     * butuh pendaftarannya sendiri.
     *
     * Build rilis memakai Play Integrity sejak Fase 8. Debug provider TIDAK
     * PERNAH boleh dipasang di rilis: ia membuat App Check bisa dipalsukan siapa
     * saja yang membongkar APK. Keputusan itu tinggal di [AppCheckProviderChoice]
     * supaya bisa diuji — kelas ini sendiri tidak bisa dijalankan di JVM.
     *
     * **Yang masih bisa gagal senyap di sini, dan tidak tertangkap test mana pun:**
     * Play Integrity menuntut SHA-256 sertifikat penanda tangan terdaftar di
     * Firebase. Dengan Play App Signing, sertifikat pada APK yang benar-benar
     * dipasang petani adalah milik Google, bukan upload key. Bila yang terdaftar
     * keliru, setiap panggilan Gemini ditolak dan gejalanya menyerupai gangguan
     * jaringan biasa. Lihat `docs/play/release-checklist.md` butir B6-B8.
     */
    private fun installAppCheck() {
        FirebaseApp.initializeApp(this)

        val factory = when (AppCheckProviderChoice.forBuild(BuildConfig.DEBUG)) {
            AppCheckProvider.DEBUG -> DebugAppCheckProviderFactory.getInstance()
            AppCheckProvider.PLAY_INTEGRITY -> PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(factory)

        if (BuildConfig.DEBUG) {
            Timber.i("App Check debug provider terpasang; cari debug token di logcat")
        }
    }
}
