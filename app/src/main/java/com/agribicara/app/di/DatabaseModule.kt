package com.agribicara.app.di

import android.content.Context
import androidx.room.Room
import com.agribicara.app.core.common.Constants
import com.agribicara.app.data.local.AppDatabase
import com.agribicara.app.data.local.dao.ChatMessageDao
import com.agribicara.app.data.local.dao.RegionCacheDao
import com.agribicara.app.data.local.dao.UserPreferenceDao
import com.agribicara.app.data.local.dao.WeatherCacheDao
import com.agribicara.app.data.local.migration.MIGRATION_1_2
import com.agribicara.app.data.local.migration.MIGRATION_2_3
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        Constants.DATABASE_NAME,
    )
        // Migrasi eksplisit. JANGAN mengganti dengan
        // fallbackToDestructiveMigration() — itu menghapus preferensi dan
        // seluruh cache pengguna setiap kali versi naik.
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()

    @Provides
    fun provideUserPreferenceDao(database: AppDatabase): UserPreferenceDao =
        database.userPreferenceDao()

    @Provides
    fun provideWeatherCacheDao(database: AppDatabase): WeatherCacheDao =
        database.weatherCacheDao()

    @Provides
    fun provideRegionCacheDao(database: AppDatabase): RegionCacheDao =
        database.regionCacheDao()

    @Provides
    fun provideChatMessageDao(database: AppDatabase): ChatMessageDao =
        database.chatMessageDao()
}
