package com.agribicara.app

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Menjaga agar androidx App Startup tidak ikut mati bersama WorkManager.
 *
 * Ada karena temuan H1 review Fase 6. Manifest sempat memuat
 * `tools:node="remove"` pada seluruh `InitializationProvider` demi mematikan
 * inisialisasi WorkManager bawaan — padahal provider itu adalah titik masuk
 * BERSAMA. emoji2, profileinstaller, lifecycle-process, dan okhttp semuanya
 * ikut mati.
 *
 * Yang membuatnya berbahaya bukan besarnya kerusakan, melainkan sunyinya:
 * build sukses, lint bersih, 213 unit test dan 43 instrumented test hijau,
 * dan worker-nya benar-benar berjalan di perangkat. Tidak ada satu pun sinyal
 * otomatis yang menyentuh emoji2 atau profileinstaller. Test ini adalah
 * sinyal itu.
 *
 * Dibaca dari PackageManager, BUKAN dari berkas manifest: yang ingin
 * dibuktikan adalah apa yang benar-benar dipasang sistem pada APK jadi, bukan
 * apa yang tertulis di sumber.
 */
@RunWith(AndroidJUnit4::class)
class ManifestInitializerTest {

    private companion object {
        const val PROVIDER = "androidx.startup.InitializationProvider"

        /**
         * Initializer yang WAJIB tetap terdaftar, beserta akibat hilangnya.
         *
         * profileinstaller yang paling penting untuk aplikasi ini: tanpa
         * baseline profile, startup rilis melambat di perangkat murah —
         * bertentangan langsung dengan NFR PRD.
         */
        val WAJIB_ADA = listOf(
            "androidx.emoji2.text.EmojiCompatInitializer",
            "androidx.profileinstaller.ProfileInstallerInitializer",
            "androidx.lifecycle.ProcessLifecycleInitializer",
        )

        /**
         * Satu-satunya yang memang sengaja dibuang.
         *
         * AgriBicaraApp mengimplementasikan `Configuration.Provider` supaya
         * WorkManager memakai HiltWorkerFactory; initializer bawaan akan
         * mendahuluinya dengan pabrik bawaan dan membuat WeatherCheckWorker
         * gagal dibuat saat runtime.
         */
        const val WAJIB_TIDAK_ADA = "androidx.work.WorkManagerInitializer"
    }

    private fun metaDataProviderStartup(): Set<String> {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val info = context.packageManager.getProviderInfo(
            ComponentName(context.packageName, PROVIDER),
            PackageManager.GET_META_DATA,
        )
        assertNotNull(
            "InitializationProvider tidak terpasang sama sekali — seluruh App Startup mati",
            info,
        )
        return info.metaData?.keySet().orEmpty()
    }

    @Test
    fun appStartupBawaanAndroidxTetapTerdaftar() {
        val terdaftar = metaDataProviderStartup()

        WAJIB_ADA.forEach { initializer ->
            assertTrue(
                "$initializer hilang dari App Startup. Periksa tools:node pada " +
                    "InitializationProvider di AndroidManifest: node=\"remove\" pada " +
                    "provider-nya mematikan SEMUA initializer, bukan hanya WorkManager. " +
                    "Yang terdaftar sekarang: $terdaftar",
                terdaftar.contains(initializer),
            )
        }
    }

    @Test
    fun initializerWorkManagerTetapDibuang() {
        val terdaftar = metaDataProviderStartup()

        assertFalse(
            "$WAJIB_TIDAK_ADA kembali terdaftar. WorkManager akan diinisialisasi " +
                "dengan pabrik bawaan sebelum Hilt sempat memberi HiltWorkerFactory, " +
                "dan WeatherCheckWorker gagal dibuat saat runtime.",
            terdaftar.contains(WAJIB_TIDAK_ADA),
        )
    }
}
