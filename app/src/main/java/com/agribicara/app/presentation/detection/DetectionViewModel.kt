package com.agribicara.app.presentation.detection

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agribicara.app.core.common.Constants
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.domain.model.DetectionOutcome
import com.agribicara.app.domain.model.DiseaseDetection
import com.agribicara.app.domain.repository.DiseaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetectionUiState(
    val isAnalyzing: Boolean = false,
    /** Hasil terakhir; null saat idle/sebelum foto pertama. */
    val outcome: DetectionOutcome? = null,
    val errorMessage: String? = null,
    val history: List<DiseaseDetection> = emptyList(),
)

/**
 * State layar deteksi. Mengikuti pola [com.agribicara.app.presentation.weather.WeatherViewModel]:
 * `HiltViewModel`, satu `MutableStateFlow<UiState>`, coroutine di `viewModelScope`.
 *
 * Bergantung langsung pada [DiseaseRepository] (bukan lewat use case): tidak ada
 * orkestrasi domain tambahan di atas repository untuk fitur ini, dan menyisipkan
 * use case hanya akan menyebarkan tipe Android [Uri] lebih jauh tanpa manfaat.
 */
@HiltViewModel
class DetectionViewModel @Inject constructor(
    private val diseaseRepository: DiseaseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetectionUiState())
    val uiState: StateFlow<DetectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            diseaseRepository.observeHistory(Constants.DETECTION_HISTORY_LIMIT).collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }

    fun analyze(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, outcome = null, errorMessage = null) }
            when (val result = diseaseRepository.classify(imageUri)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isAnalyzing = false, outcome = result.data)
                }
                is NetworkResult.Error -> _uiState.update {
                    it.copy(isAnalyzing = false, errorMessage = result.message)
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    /** Kosongkan hasil untuk memulai foto baru ("Foto lagi"). */
    fun reset() {
        _uiState.update { it.copy(outcome = null, errorMessage = null) }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
