package com.agribicara.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agribicara.app.core.common.Constants
import com.agribicara.app.data.local.dao.UserPreferenceDao
import com.agribicara.app.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Menentukan layar awal.
 *
 * [StartState.Loading] sengaja ada supaya UI tidak "berkedip" dari Onboarding ke
 * Home saat nilai preferensi masih dibaca dari database.
 */
sealed interface StartState {
    data object Loading : StartState
    data class Ready(val route: String) : StartState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferenceDao: UserPreferenceDao,
) : ViewModel() {

    private val _startState = MutableStateFlow<StartState>(StartState.Loading)
    val startState: StateFlow<StartState> = _startState.asStateFlow()

    init {
        resolveStartDestination()
    }

    private fun resolveStartDestination() {
        viewModelScope.launch {
            // Kegagalan baca database TIDAK boleh menggantung app di layar kosong.
            // Room bisa gagal dibuka (DB korup, disk penuh, migrasi bermasalah di
            // Fase 2/4/5). Dalam kasus itu, tampilkan onboarding: paling buruk
            // pengguna melihatnya sekali lagi, jauh lebih baik daripada layar
            // putih tanpa jalan keluar.
            val route = try {
                val preference = userPreferenceDao.get(Constants.USER_PREFERENCE_ID)
                if (preference?.isOnboardingCompleted == true) Route.HOME else Route.ONBOARDING
            } catch (e: Exception) {
                Timber.e(e, "Gagal membaca preferensi; jatuh ke onboarding")
                Route.ONBOARDING
            }
            _startState.value = StartState.Ready(route)
        }
    }
}
