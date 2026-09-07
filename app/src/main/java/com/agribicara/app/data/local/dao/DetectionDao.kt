package com.agribicara.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.agribicara.app.data.local.entity.DetectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionDao {

    @Insert
    suspend fun insert(entity: DetectionEntity): Long

    @Query("SELECT * FROM disease_detection ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DetectionEntity>>

    /** Membuang riwayat lama agar tabel tidak tumbuh tanpa batas. */
    @Query(
        """
        DELETE FROM disease_detection WHERE id NOT IN (
            SELECT id FROM disease_detection ORDER BY createdAt DESC LIMIT :keep
        )
        """,
    )
    suspend fun trimTo(keep: Int)
}
