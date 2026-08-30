package com.agribicara.app.di

import com.agribicara.app.BuildConfig
import com.agribicara.app.core.common.Constants
import com.agribicara.app.data.remote.bmkg.BmkgApiService
import com.agribicara.app.data.remote.openmeteo.OpenMeteoApiService
import com.agribicara.app.data.remote.wilayah.WilayahApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Fondasi jaringan.
 *
 * Tiga host dipakai di Fase 2 (BMKG, Open-Meteo, wilayah.id), masing-masing
 * butuh Retrofit sendiri karena base URL berbeda — tapi semuanya berbagi satu
 * [OkHttpClient] agar connection pool dan timeout konsisten.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                },
            )
        }
        return builder.build()
    }

    /**
     * Konfigurasi parsing yang sengaja permisif.
     *
     * `ignoreUnknownKeys` adalah mitigasi konkret untuk risiko "BMKG API tidak
     * stabil/format berubah" di PRD: penambahan field baru oleh BMKG tidak akan
     * meruntuhkan parsing. `coerceInputValues` menjaga null pada field non-null
     * agar jatuh ke nilai default, bukan melempar exception.
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    @BmkgRetrofit
    fun provideBmkgRetrofit(client: OkHttpClient, json: Json): Retrofit =
        buildRetrofit(Constants.BMKG_BASE_URL, client, json)

    @Provides
    @Singleton
    @OpenMeteoRetrofit
    fun provideOpenMeteoRetrofit(client: OkHttpClient, json: Json): Retrofit =
        buildRetrofit(Constants.OPEN_METEO_BASE_URL, client, json)

    @Provides
    @Singleton
    @WilayahRetrofit
    fun provideWilayahRetrofit(client: OkHttpClient, json: Json): Retrofit =
        buildRetrofit(Constants.WILAYAH_BASE_URL, client, json)

    @Provides
    @Singleton
    fun provideBmkgApiService(@BmkgRetrofit retrofit: Retrofit): BmkgApiService =
        retrofit.create(BmkgApiService::class.java)

    @Provides
    @Singleton
    fun provideOpenMeteoApiService(@OpenMeteoRetrofit retrofit: Retrofit): OpenMeteoApiService =
        retrofit.create(OpenMeteoApiService::class.java)

    @Provides
    @Singleton
    fun provideWilayahApiService(@WilayahRetrofit retrofit: Retrofit): WilayahApiService =
        retrofit.create(WilayahApiService::class.java)

    private fun buildRetrofit(baseUrl: String, client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(APPLICATION_JSON.toMediaType()))
            .build()

    private const val APPLICATION_JSON = "application/json"
}
