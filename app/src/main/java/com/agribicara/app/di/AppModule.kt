package com.agribicara.app.di

import com.agribicara.app.core.common.DefaultDispatcherProvider
import com.agribicara.app.core.common.DispatcherProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(
        impl: DefaultDispatcherProvider,
    ): DispatcherProvider

    companion object {

        /**
         * Waktu disuntikkan, bukan dibaca langsung lewat LocalTime.now(),
         * supaya batas jam sapaan bisa diuji secara deterministik — pola yang
         * sama dengan [DispatcherProvider].
         */
        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.systemDefaultZone()
    }
}
