package com.agribicara.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.agribicara.app.core.common.Constants
import com.agribicara.app.data.local.entity.UserPreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferenceDao {

    @Query("SELECT * FROM user_preference WHERE id = :id LIMIT 1")
    fun observe(id: Int): Flow<UserPreferenceEntity?>

    @Query("SELECT * FROM user_preference WHERE id = :id LIMIT 1")
    suspend fun get(id: Int): UserPreferenceEntity?

    @Upsert
    suspend fun upsert(entity: UserPreferenceEntity)

    @Query("UPDATE user_preference SET isOnboardingCompleted = :completed WHERE id = :id")
    suspend fun updateOnboardingCompleted(id: Int, completed: Boolean): Int

    /**
     * Menandai onboarding selesai secara atomik.
     *
     * Sengaja BUKAN read-modify-write di ViewModel: pola itu bisa menimpa field
     * lain (mis. regionCode yang ditulis region picker Fase 2) dengan salinan
     * basi bila ada penulis lain di sela baca dan tulis. UPDATE bertarget hanya
     * menyentuh satu kolom; INSERT dipakai hanya bila barisnya memang belum ada.
     */
    @Transaction
    suspend fun markOnboardingCompleted(id: Int = Constants.USER_PREFERENCE_ID) {
        val updatedRows = updateOnboardingCompleted(id, true)
        if (updatedRows == 0) {
            upsert(UserPreferenceEntity(id = id, isOnboardingCompleted = true))
        }
    }
}
