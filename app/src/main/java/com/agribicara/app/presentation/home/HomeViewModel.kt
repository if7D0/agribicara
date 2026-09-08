package com.agribicara.app.presentation.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agribicara.app.R
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.data.local.NotificationPromptStore
import com.agribicara.app.domain.model.DailyForecast
import com.agribicara.app.domain.model.Region
import com.agribicara.app.domain.model.WeatherSource
import com.agribicara.app.domain.usecase.GetForecastUseCase
import com.agribicara.app.domain.usecase.ObserveSelectedRegionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * State layar Home.
 *
 * Satu data class per layar, di-expose sebagai StateFlow tunggal - pola ini
 * diikuti oleh seluruh ViewModel di fase berikutnya.
 */
data class HomeUiState(
    @param:StringRes val greetingRes: Int = R.string.home_greeting_morning,
    val regionName: String? = null,
    /** Ringkasan hari ini untuk kartu cuaca; null saat belum ada data. */
    val today: DailyForecast? = null,
    val source: WeatherSource? = null,
    val isWeatherLoading: Boolean = false,
    /** True bila pengguna belum memilih wilayah. */
    val needsRegion: Boolean = false,
    /**
     * True bila dialog izin notifikasi sudah pernah ditampilkan.
     *
     * Null selama nilainya masih dibaca dari penyimpanan. Dibedakan dari
     * `false` DENGAN SENGAJA: memperlakukan "belum tahu" sebagai "belum
     * pernah" akan memunculkan dialog izin sekejap sebelum nilai sebenarnya
     * tiba — persis kambuhnya temuan F6 L5 dalam bentuk lain.
     */
    val notificationPromptAsked: Boolean? = null,
) {
    val isOffline: Boolean get() = source == WeatherSource.CACHE
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val clock: Clock,
    private val getForecast: GetForecastUseCase,
    private val observeSelectedRegion: ObserveSelectedRegionUseCase,
    private val notificationPromptStore: NotificationPromptStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(greetingRes = currentGreeting()))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var currentRegion: Region? = null

    init {
        // Wilayah diamati, bukan dibaca sekali: kembali dari region picker
        // langsung memicu pemuatan ulang tanpa bergantung pada resume.
        viewModelScope.launch {
            observeSelectedRegion().distinctUntilChanged().collectLatest { region ->
                currentRegion = region
                if (region == null) {
                    _uiState.update {
                        it.copy(isWeatherLoading = false, needsRegion = true, today = null)
                    }
                } else {
                    fetch(region)
                }
            }
        }

        // Penanda izin notifikasi dibaca dari penyimpanan, bukan dari state
        // Composable. Lihat NotificationPromptStore untuk alasannya (F6 L5).
        viewModelScope.launch {
            notificationPromptStore.hasAsked.collectLatest { asked ->
                _uiState.update { it.copy(notificationPromptAsked = asked) }
            }
        }
    }

    /** Dipanggil tepat setelah dialog izin notifikasi ditampilkan. */
    fun onNotificationPromptShown() {
        viewModelScope.launch {
            runCatching { notificationPromptStore.markAsked() }
                .onFailure {
                    // Pembatalan BUKAN kegagalan. runCatching menangkap
                    // Throwable, CancellationException termasuk, dan menelannya
                    // memutus structured concurrency. Sikap itu sudah dipaku
                    // test di FirebaseAiRepositoryTest; menuliskan pola yang
                    // berlawanan di sini membuatnya berhenti menjadi sikap.
                    if (it is CancellationException) throw it

                    // Gagal menyimpan berarti dialog bisa muncul sekali lagi
                    // nanti — mengganggu, bukan merusak. Tidak pantas
                    // memerahkan apa pun bagi petani.
                    Timber.w(it, "Penanda izin notifikasi gagal disimpan")
                }
        }
    }

    /**
     * Hitung ulang sapaan.
     *
     * Wajib dipanggil saat layar kembali resume: ViewModel bertahan melewati
     * perubahan konfigurasi dan app di-background, jadi sapaan yang hanya
     * dihitung di konstruktor akan basi (buka pukul 10.55 "Selamat pagi",
     * kembali pukul 19.00 masih "Selamat pagi").
     */
    fun refreshGreeting() {
        val greeting = currentGreeting()
        _uiState.update { if (greeting == it.greetingRes) it else it.copy(greetingRes = greeting) }
    }

    /**
     * Memuat ulang cuaca wilayah aktif.
     *
     * Aman dipanggil pada setiap resume: repository mengembalikan cache yang
     * masih segar tanpa menyentuh jaringan, sehingga ini tidak membakar kuota.
     */
    fun loadWeather() {
        val region = currentRegion ?: return
        viewModelScope.launch { fetch(region) }
    }

    private suspend fun fetch(region: Region) {
        _uiState.update { it.copy(isWeatherLoading = true) }

        when (val result = getForecast(region.code, region.name)) {
            is NetworkResult.Success -> _uiState.update {
                it.copy(
                    regionName = result.data.regionName,
                    today = result.data.days.firstOrNull(),
                    source = result.data.source,
                    isWeatherLoading = false,
                    needsRegion = false,
                )
            }

            is NetworkResult.Error -> {
                // Kegagalan cuaca TIDAK memblokir Home: tombol mic tetap
                // harus bisa dipakai. Kartu cuaca dikosongkan — menyisakan
                // data lama akan menampilkan cuaca wilayah sebelumnya di
                // bawah nama wilayah yang baru.
                Timber.w("Cuaca gagal dimuat di Home: %s", result.message)
                _uiState.update {
                    it.copy(
                        regionName = region.name,
                        today = null,
                        source = null,
                        isWeatherLoading = false,
                        needsRegion = false,
                    )
                }
            }

            NetworkResult.Loading -> Unit
        }
    }

    private fun currentGreeting(): Int = greetingFor(LocalTime.now(clock))

    companion object {
        /**
         * java.time aman pada minSdk 24 karena coreLibraryDesugaring aktif.
         * Internal agar batas jamnya bisa diuji langsung.
         */
        @StringRes
        internal fun greetingFor(time: LocalTime): Int = when (time.hour) {
            in 4..10 -> R.string.home_greeting_morning
            in 11..14 -> R.string.home_greeting_afternoon
            in 15..18 -> R.string.home_greeting_evening
            else -> R.string.home_greeting_night
        }
    }
}
