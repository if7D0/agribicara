package com.agribicara.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
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
}
