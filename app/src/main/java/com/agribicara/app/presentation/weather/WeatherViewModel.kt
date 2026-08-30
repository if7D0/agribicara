package com.agribicara.app.presentation.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.domain.model.DailyForecast
import com.agribicara.app.domain.model.Region
import com.agribicara.app.domain.model.WeatherSource
import com.agribicara.app.domain.usecase.GetForecastUseCase
import com.agribicara.app.domain.usecase.ObserveSelectedRegionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeatherUiState(
    val regionName: String? = null,
    val days: List<DailyForecast> = emptyList(),
    val source: WeatherSource? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** True bila pengguna belum memilih wilayah sama sekali. */
    val needsRegion: Boolean = false,
) {
    /** Banner offline hanya relevan saat data benar-benar berasal dari cache. */
    val isOffline: Boolean get() = source == WeatherSource.CACHE
}

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val getForecast: GetForecastUseCase,
    private val observeSelectedRegion: ObserveSelectedRegionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var currentRegion: Region? = null

    init {
        /**
         * Wilayah DIAMATI sebagai flow, bukan dibaca sekali dengan .first().
         *
         * Layar ini bertahan di back stack: pengguna bisa membuka picker dari
         * sini, mengganti wilayah, lalu kembali. Dengan pembacaan sekali-jalan
         * layar akan tetap menampilkan state wilayah lama tanpa cara memuat
         * ulang. collectLatest juga membatalkan pengambilan yang sedang
         * berjalan begitu wilayah berubah, sehingga respons wilayah lama tidak
         * bisa mendarat belakangan.
         */
        viewModelScope.launch {
            observeSelectedRegion().distinctUntilChanged().collectLatest { region ->
                currentRegion = region
                if (region == null) {
                    _uiState.update {
                        it.copy(isLoading = false, needsRegion = true, days = emptyList())
                    }
                } else {
                    fetch(region)
                }
            }
        }
    }

    /** Muat ulang wilayah yang sedang aktif; dipakai tombol coba lagi. */
    fun refresh() {
        val region = currentRegion ?: return
        viewModelScope.launch { fetch(region) }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private suspend fun fetch(region: Region) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        when (val result = getForecast(region.code, region.name)) {
            is NetworkResult.Success -> _uiState.update {
                it.copy(
                    regionName = result.data.regionName,
                    days = result.data.days,
                    source = result.data.source,
                    isLoading = false,
                    needsRegion = false,
                )
            }

            is NetworkResult.Error -> _uiState.update {
                it.copy(
                    regionName = region.name,
                    // Data wilayah SEBELUMNYA wajib dibuang: menyisakannya
                    // akan menampilkan cuaca wilayah lama di bawah nama
                    // wilayah baru.
                    days = emptyList(),
                    source = null,
                    isLoading = false,
                    needsRegion = false,
                    errorMessage = result.message,
                )
            }

            NetworkResult.Loading -> Unit
        }
    }
}
