package com.agribicara.app.presentation.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.agribicara.app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State layar Home.
 *
 * Satu data class per layar, di-expose sebagai StateFlow tunggal - pola ini
 * diikuti oleh seluruh ViewModel di fase berikutnya.
 */
data class HomeUiState(
    @param:StringRes val greetingRes: Int = R.string.home_greeting_morning,
    val isVoiceReady: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(greetingRes = greetingFor(LocalTime.now())))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private companion object {
        /**
         * java.time dipakai di sini - aman pada minSdk 24 karena
         * coreLibraryDesugaring aktif di app/build.gradle.kts.
         */
        @StringRes
        fun greetingFor(time: LocalTime): Int = when (time.hour) {
            in 4..10 -> R.string.home_greeting_morning
            in 11..14 -> R.string.home_greeting_afternoon
            in 15..18 -> R.string.home_greeting_evening
            else -> R.string.home_greeting_night
        }
    }
}
