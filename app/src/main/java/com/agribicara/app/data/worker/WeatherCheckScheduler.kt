package com.agribicara.app.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.agribicara.app.core.common.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/** Mendaftarkan pemeriksaan cuaca berkala. */
@Singleton
class WeatherCheckScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Aman dipanggil setiap kali app dijalankan.
     *
     * [ExistingPeriodicWorkPolicy.KEEP], BUKAN UPDATE atau REPLACE: keduanya
     * mengatur ulang jadwal setiap app dibuka, sehingga pada pengguna yang
     * sering membuka aplikasi hitungan intervalnya selalu dimulai ulang dan
     * pekerjaannya praktis tidak pernah berjalan.
     */
    fun schedule() {
        val request = PeriodicWorkRequestBuilder<WeatherCheckWorker>(
            Constants.WEATHER_CHECK_INTERVAL_HOURS,
            TimeUnit.HOURS,
        ).setConstraints(
            // Tanpa jaringan, satu-satunya hasil adalah retry yang membakar
            // baterai. Biarkan sistem yang menunggu koneksi.
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build(),
        ).build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                Constants.WEATHER_CHECK_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }.onFailure {
            // Gagal menjadwalkan tidak boleh membuat aplikasi gagal dibuka.
            // Notifikasi adalah tambahan; layar utamanya yang utama.
            Timber.e(it, "Gagal menjadwalkan pemeriksaan cuaca")
        }
    }
}
