package com.agribicara.app.data.remote.openmeteo

import com.agribicara.app.core.common.Constants
import com.agribicara.app.data.remote.openmeteo.dto.OpenMeteoResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo — fallback saat BMKG gagal, sekaligus penambal hari 4-7 yang
 * tidak disediakan BMKG. Gratis, tanpa API key.
 */
interface OpenMeteoApiService {

    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("daily") daily: String = Constants.OPEN_METEO_DAILY_FIELDS,
        @Query("timezone") timezone: String = Constants.DEFAULT_TIMEZONE,
        @Query("forecast_days") forecastDays: Int = Constants.FORECAST_DAYS,
    ): Response<OpenMeteoResponse>
}
