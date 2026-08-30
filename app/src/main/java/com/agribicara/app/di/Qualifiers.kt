package com.agribicara.app.di

import javax.inject.Qualifier

/**
 * Pembeda instance Retrofit.
 *
 * Fase 2 memakai tiga host berbeda (BMKG, Open-Meteo, wilayah.id) sehingga
 * butuh tiga Retrofit dengan base URL berbeda. OkHttpClient-nya tetap SATU
 * dan dipakai bersama — jangan membuat client kedua.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BmkgRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OpenMeteoRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WilayahRetrofit
