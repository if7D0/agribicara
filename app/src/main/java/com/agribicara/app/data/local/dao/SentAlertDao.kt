package com.agribicara.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.agribicara.app.core.common.Constants
import com.agribicara.app.data.local.entity.SentAlertEntity

@Dao
interface SentAlertDao {

    @Query("SELECT * FROM sent_alert WHERE id = :id LIMIT 1")
    suspend fun get(id: Int = Constants.SENT_ALERT_ID): SentAlertEntity?

    /**
     * Upsert, bukan insert: barisnya selalu satu dan selalu ber-id sama, jadi
     * peringatan terbaru menimpa yang lama tanpa perlu menghapus lebih dulu.
     */
    @Upsert
    suspend fun upsert(entity: SentAlertEntity)
}
