package com.agribicara.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.data.notification.WeatherNotifier
import com.agribicara.app.domain.usecase.GetForecastUseCase
import com.agribicara.app.domain.usecase.ObserveSelectedRegionUseCase
import com.agribicara.app.domain.weather.ExtremeWeatherRule
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Memeriksa cuaca secara berkala dan memperingatkan bila ekstrem (Fase 6).
 *
 * Tanpa ini, petani baru tahu ada badai ketika ia sendiri membuka aplikasi —
 * padahal justru pada hari sibuk di sawah ia paling jarang membukanya.
 *
 * Memakai ulang [GetForecastUseCase] yang sama dengan layar Home dan layar
 * cuaca, jadi cache, fallback Open-Meteo, dan penanganan kegagalannya persis
 * sama. Tidak ada jalur data kedua yang bisa menyimpang diam-diam.
 */
@HiltWorker
class WeatherCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val getForecast: GetForecastUseCase,
    private val observeSelectedRegion: ObserveSelectedRegionUseCase,
    private val notifier: WeatherNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // `.first()` WAJIB: sumbernya Flow Room yang tidak pernah berakhir.
        // Mengumpulkannya tanpa batas akan menahan doWork() sampai sistem
        // mematikannya, dan pekerjaannya tidak pernah dianggap selesai.
        val region = observeSelectedRegion().first()

        if (region == null) {
            // Belum memilih wilayah berarti tidak ada yang bisa diperiksa.
            // Ini keadaan yang sah, BUKAN kegagalan — mengembalikan retry akan
            // membuat WorkManager mencoba terus tanpa akhir.
            Timber.d("Pemeriksaan cuaca dilewati: wilayah belum dipilih")
            return Result.success()
        }

        return when (val result = getForecast(region.code, region.name)) {
            is NetworkResult.Success -> {
                ExtremeWeatherRule.evaluate(result.data)?.let { alert ->
                    Timber.i("Cuaca ekstrem terdeteksi: %s", alert.reason)
                    notifier.notify(alert)
                }
                Result.success()
            }

            is NetworkResult.Error -> {
                // retry(), BUKAN failure(): kegagalan di sini hampir selalu
                // jaringan desa yang sedang putus. failure() membuat pekerjaan
                // ini tidak pernah dicoba lagi sampai app dibuka.
                Timber.w("Pemeriksaan cuaca gagal: %s", result.message)
                Result.retry()
            }

            NetworkResult.Loading -> Result.success()
        }
    }
}
