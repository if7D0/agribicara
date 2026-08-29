package com.agribicara.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agribicara.app.core.common.Constants
import com.agribicara.app.data.local.dao.UserPreferenceDao
import com.agribicara.app.data.local.entity.UserPreferenceEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferenceDao: UserPreferenceDao,
) : ViewModel() {

    /**
     * Menandai onboarding selesai lalu memanggil [onSaved].
     *
     * Flag disimpan SEBELUM navigasi supaya app yang ditutup tepat setelah
     * tombol terakhir ditekan tidak menampilkan onboarding lagi.
     */
    fun completeOnboarding(onSaved: () -> Unit) {
        viewModelScope.launch {
            val existing = userPreferenceDao.get(Constants.USER_PREFERENCE_ID)
            val updated = existing?.copy(isOnboardingCompleted = true)
                ?: UserPreferenceEntity(isOnboardingCompleted = true)
            userPreferenceDao.upsert(updated)
            onSaved()
        }
    }
}
