package com.agribicara.app.di

import com.agribicara.app.data.repository.RegionRepositoryImpl
import com.agribicara.app.data.repository.WeatherRepositoryImpl
import com.agribicara.app.domain.repository.RegionRepository
import com.agribicara.app.domain.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Mengikat interface domain ke implementasi data.
 *
 * @Binds dipakai (bukan @Provides) karena Hilt cukup tahu bahwa implementasi
 * sudah punya @Inject constructor — tidak perlu factory yang ditulis tangan.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(impl: WeatherRepositoryImpl): WeatherRepository

    @Binds
    @Singleton
    abstract fun bindRegionRepository(impl: RegionRepositoryImpl): RegionRepository
}
