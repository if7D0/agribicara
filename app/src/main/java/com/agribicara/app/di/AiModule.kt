package com.agribicara.app.di

import com.agribicara.app.data.ai.AiTextGenerator
import com.agribicara.app.data.ai.FirebaseAiRepository
import com.agribicara.app.data.ai.FirebaseTextGenerator
import com.agribicara.app.data.repository.ChatHistoryRepositoryImpl
import com.agribicara.app.domain.repository.AiRepository
import com.agribicara.app.domain.repository.ChatHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Mengikat kontrak AI (Fase 5) ke implementasinya.
 *
 * Dipisah dari [RepositoryModule] supaya penggantian penyedia AI di kemudian
 * hari terlihat sebagai satu perubahan terisolasi.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindAiRepository(impl: FirebaseAiRepository): AiRepository

    /**
     * Batas ke SDK Firebase, dipisah dari repository-nya supaya kebijakan
     * (ulang, pesan) bisa diuji tanpa FirebaseApp. Lihat [AiTextGenerator].
     */
    @Binds
    @Singleton
    abstract fun bindAiTextGenerator(impl: FirebaseTextGenerator): AiTextGenerator

    @Binds
    @Singleton
    abstract fun bindChatHistoryRepository(
        impl: ChatHistoryRepositoryImpl,
    ): ChatHistoryRepository
}
