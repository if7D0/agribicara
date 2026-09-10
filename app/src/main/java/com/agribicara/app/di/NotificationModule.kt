package com.agribicara.app.di

import com.agribicara.app.data.notification.WeatherAlertNotifier
import com.agribicara.app.data.notification.WeatherNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Mengikat kemampuan notifikasi ke implementasi yang menyentuh Android. */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindWeatherAlertNotifier(impl: WeatherNotifier): WeatherAlertNotifier
}
