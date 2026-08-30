package com.agribicara.app.data.remote.wilayah

import com.agribicara.app.data.remote.wilayah.dto.WilayahResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Daftar wilayah administratif Indonesia untuk region picker.
 *
 * Kode yang dikembalikan berformat titik ("32.77.01.1002") dan diterima
 * langsung oleh BMKG sebagai parameter `adm4` — sudah diverifikasi
 * ujung-ke-ujung pada spike 2026-08-29.
 *
 * Keempat tingkat memakai amplop response yang sama, sehingga satu DTO cukup.
 */
interface WilayahApiService {

    @GET("api/provinces.json")
    suspend fun getProvinces(): Response<WilayahResponse>

    @GET("api/regencies/{provinceCode}.json")
    suspend fun getRegencies(@Path("provinceCode") provinceCode: String): Response<WilayahResponse>

    @GET("api/districts/{regencyCode}.json")
    suspend fun getDistricts(@Path("regencyCode") regencyCode: String): Response<WilayahResponse>

    @GET("api/villages/{districtCode}.json")
    suspend fun getVillages(@Path("districtCode") districtCode: String): Response<WilayahResponse>
}
