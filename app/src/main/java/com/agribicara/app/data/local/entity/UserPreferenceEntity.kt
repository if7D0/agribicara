package com.agribicara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.agribicara.app.core.common.Constants

/**
 * Preferensi pengguna — tabel baris tunggal (id selalu [Constants.USER_PREFERENCE_ID]).
 *
 * regionCode/regionName masih null pada Fase 1; diisi oleh region picker di Fase 2.
 */
@Entity(tableName = "user_preference")
data class UserPreferenceEntity(
    @PrimaryKey val id: Int = Constants.USER_PREFERENCE_ID,
    val isOnboardingCompleted: Boolean = false,
    val regionCode: String? = null,
    val regionName: String? = null,
)
