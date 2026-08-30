package com.agribicara.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class AgriBicaraApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Build rilis: tree yang meneruskan WARN/ERROR ke Crashlytics
        // ditambahkan di Fase 8. Jangan log data pribadi di rilis.

        installAppCheck()
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
