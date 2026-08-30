package com.agribicara.app.data.remote.bmkg

import com.agribicara.app.data.remote.bmkg.dto.BmkgForecastResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Prakiraan cuaca publik BMKG. Tanpa API key.
 *
 * Hanya menerima kode `adm4` (kelurahan/desa). Query `adm3` dibalas 301 dan
 * tanpa parameter dibalas 404 — keduanya sudah diuji pada spike 2026-08-29.
 *
 * Mengembalikan [Response] agar 404 "Data not found" bisa dibedakan dari
 * kegagalan jaringan: Retrofit TIDAK melempar exception untuk 4xx.
 */
interface BmkgApiService {

    @GET("publik/prakiraan-cuaca")
    suspend fun getForecast(
        @Query("adm4") adm4: String,
    ): Response<BmkgForecastResponse>
}
