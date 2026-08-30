package com.agribicara.app.di

import com.agribicara.app.BuildConfig
import com.agribicara.app.core.common.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Fondasi jaringan.
 *
 * Sengaja minimal: Fase 1 belum memanggil API apa pun. Service interface
 * (BMKG, Open-Meteo, Gemini) ditambahkan pada fase yang memakainya, bukan di sini.
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
}
