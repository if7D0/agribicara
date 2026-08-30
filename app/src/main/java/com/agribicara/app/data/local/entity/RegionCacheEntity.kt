package com.agribicara.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cache daftar wilayah dari wilayah.id.
 *
 * Tanpa ini, region picker mati total begitu wilayah.id tidak bisa dihubungi.
 * Dengan cache, wilayah yang pernah dibuka tetap bisa ditelusuri offline.
 *
 * [parentCode] null hanya untuk provinsi (tingkat teratas).
 */
@Entity(
    tableName = "region_cache",
    indices = [Index(value = ["parentCode"]), Index(value = ["level"])],
)
data class RegionCacheEntity(
    @PrimaryKey val code: String,
    val name: String,
    /** Nama [com.agribicara.app.domain.model.RegionLevel]. */
    val level: String,
    val parentCode: String?,
)
