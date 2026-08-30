package com.agribicara.app.presentation.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.agribicara.app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
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
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(greetingRes = currentGreeting()))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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
        if (greeting != _uiState.value.greetingRes) {
            _uiState.value = _uiState.value.copy(greetingRes = greeting)
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
