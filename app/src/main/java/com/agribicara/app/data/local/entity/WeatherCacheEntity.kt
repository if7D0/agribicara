package com.agribicara.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Satu hari prakiraan yang disimpan offline.
 *
 * Primary key gabungan (regionCode, date) supaya penulisan ulang untuk hari
 * yang sama menimpa, bukan menumpuk — refresh berulang tidak menggelembungkan
 * tabel.
 *
 * Tanggal disimpan sebagai String ISO ("2026-08-29") bukan epoch: Room tidak
 * punya TypeConverter di project ini, dan bentuk ISO membuat ORDER BY tetap
 * benar secara leksikografis.
 */
@Entity(
    tableName = "weather_cache",
    primaryKeys = ["regionCode", "date"],
    indices = [Index(value = ["regionCode"])],
)
data class WeatherCacheEntity(
    val regionCode: String,
    /** Tanggal lokal ISO "yyyy-MM-dd". */
    val date: String,
    val temperatureMax: Double?,
    val temperatureMin: Double?,
    val precipitationMm: Double?,
    val windSpeed: Double?,
    val weatherCode: Int?,
    val description: String?,
    /** Nama [com.agribicara.app.domain.model.WeatherSource] saat data diambil. */
    val source: String,
    /** Epoch millis saat baris ini ditulis; dasar penentuan cache basi. */
    val fetchedAt: Long,
)
