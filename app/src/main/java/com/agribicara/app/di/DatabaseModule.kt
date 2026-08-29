package com.agribicara.app.di

import android.content.Context
import androidx.room.Room
import com.agribicara.app.core.common.Constants
import com.agribicara.app.data.local.AppDatabase
import com.agribicara.app.data.local.dao.UserPreferenceDao
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
    ).build()

    @Provides
    fun provideUserPreferenceDao(database: AppDatabase): UserPreferenceDao =
        database.userPreferenceDao()
}
