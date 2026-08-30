package com.agribicara.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.agribicara.app.data.local.entity.WeatherCacheEntity

@Dao
interface WeatherCacheDao {

    @Query("SELECT * FROM weather_cache WHERE regionCode = :regionCode ORDER BY date ASC")
    suspend fun getForRegion(regionCode: String): List<WeatherCacheEntity>

    @Upsert
    suspend fun upsertAll(entities: List<WeatherCacheEntity>)

    @Query("DELETE FROM weather_cache WHERE regionCode = :regionCode")
    suspend fun deleteForRegion(regionCode: String)

    /**
     * Ganti seluruh cache satu wilayah secara atomik.
     *
     * Sengaja hapus-lalu-tulis dalam satu transaksi: kalau hanya upsert,
     * hari yang hilang dari response baru (mis. tanggal yang sudah lewat)
     * akan tertinggal selamanya dan bercampur dengan data segar.
     */
    @Transaction
    suspend fun replaceForRegion(regionCode: String, entities: List<WeatherCacheEntity>) {
        deleteForRegion(regionCode)
        upsertAll(entities)
    }
}
