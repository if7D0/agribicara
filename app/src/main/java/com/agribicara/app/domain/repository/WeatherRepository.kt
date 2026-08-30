package com.agribicara.app.domain.repository

import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.domain.model.Forecast

/**
 * Akses data cuaca.
 *
 * Kontrak (lihat [NetworkResult]): implementasi TIDAK PERNAH melempar
 * exception melewati batas ini. Setiap kegagalan menjadi
 * [NetworkResult.Error] dengan pesan Bahasa Indonesia sederhana.
 */
interface WeatherRepository {

    /**
     * Prakiraan untuk satu kelurahan.
     *
     * Menjalankan rantai: BMKG -> Open-Meteo -> cache Room. Selalu
     * mengembalikan Success bila salah satu tingkat berhasil; [Forecast.source]
     * memberi tahu UI dari mana data akhirnya datang.
     */
    suspend fun getForecast(regionCode: String, regionName: String): NetworkResult<Forecast>
}
