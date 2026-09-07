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

/**
 * Menandai `BuildConfig.DEBUG` sebagai nilai yang disuntikkan.
 *
 * Disuntikkan, bukan dibaca langsung, dengan alasan yang sama seperti
 * [java.time.Clock] di [AppModule]: `BuildConfig.DEBUG` adalah konstanta
 * kompilasi yang SELALU `true` di unit test (`testDebugUnitTest`), sehingga
 * kode yang membacanya langsung membuat cabang rilis mustahil diuji — dan di
 * sini cabang rilis itulah yang paling penting, karena ia yang menentukan
 * pesan mana yang sampai ke petani.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsDebugBuild
