package com.agribicara.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.agribicara.app.data.local.entity.RegionCacheEntity

@Dao
interface RegionCacheDao {

    @Query("SELECT * FROM region_cache WHERE level = :level AND parentCode IS :parentCode ORDER BY name ASC")
    suspend fun getByLevel(level: String, parentCode: String?): List<RegionCacheEntity>

    @Upsert
    suspend fun upsertAll(entities: List<RegionCacheEntity>)
}
