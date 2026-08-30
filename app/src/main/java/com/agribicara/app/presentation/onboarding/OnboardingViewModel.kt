package com.agribicara.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agribicara.app.core.common.Constants
import com.agribicara.app.data.local.dao.UserPreferenceDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class OnboardingUiState(
    val isSaving: Boolean = false,
    /** Menjadi true tepat sekali, saat flag berhasil ditulis. */
    val isCompleted: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferenceDao: UserPreferenceDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /**
     * Menandai onboarding selesai.
     *
     * Navigasi TIDAK dilakukan lewat callback dari sini: memasukkan lambda UI
     * (yang menahan NavHostController) ke ViewModel akan membocorkan controller
     * milik Activity yang sudah hancur bila terjadi rotasi saat penulisan
     * berlangsung. Sebagai gantinya layar mengamati [OnboardingUiState.isCompleted].
     *
     * [isSaving] juga menjadi penjaga tap ganda — tanpa itu dua tap cepat
     * menghasilkan dua kali navigasi ke Home.
     */
    fun completeOnboarding() {
        if (_uiState.value.isSaving || _uiState.value.isCompleted) return

        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            try {
                userPreferenceDao.markOnboardingCompleted(Constants.USER_PREFERENCE_ID)
                _uiState.value = _uiState.value.copy(isSaving = false, isCompleted = true)
            } catch (e: Exception) {
                Timber.e(e, "Gagal menyimpan status onboarding")
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Gagal menyimpan. Coba lagi.",
                )
            }
        }
    }

    fun onErrorShown() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
