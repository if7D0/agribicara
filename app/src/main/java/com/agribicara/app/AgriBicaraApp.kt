package com.agribicara.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.agribicara.app.data.worker.WeatherCheckScheduler
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
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
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Build rilis: tree yang meneruskan WARN/ERROR ke Crashlytics
        // ditambahkan di Fase 8. Jangan log data pribadi di rilis.

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
     * BUILD RILIS SENGAJA TIDAK PUNYA PROVIDER APA PUN, jadi fitur AI **tidak akan
     * bekerja di APK rilis** sampai Play Integrity dipasang di Fase 8. Ini dicatat
     * terang-terangan, bukan dilewat diam-diam: memasang debug provider di rilis
     * akan membuat App Check bisa dipalsukan siapa saja yang membongkar APK.
     */
    private fun installAppCheck() {
        FirebaseApp.initializeApp(this)

        if (BuildConfig.DEBUG) {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance(),
            )
            Timber.i("App Check debug provider terpasang; cari debug token di logcat")
        } else {
            // Fase 8: PlayIntegrityAppCheckProviderFactory.getInstance()
            Timber.w("App Check tidak dipasang di build rilis — panggilan AI akan ditolak")
        }
    }
}
