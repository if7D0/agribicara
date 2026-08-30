package com.agribicara.app.di

import com.agribicara.app.data.speech.AndroidSpeechRecognizerRepository
import com.agribicara.app.data.speech.AndroidTextToSpeechRepository
import com.agribicara.app.domain.repository.SpeechRecognizerRepository
import com.agribicara.app.domain.repository.TextToSpeechRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Mengikat kemampuan suara ke implementasi Android-nya.
 *
 * Terpisah dari [RepositoryModule] karena isinya bukan sumber data melainkan
 * kemampuan perangkat — dan keduanya `@Singleton` sebab masing-masing memegang
 * sumber daya sistem (mikrofon, engine TTS) yang tidak boleh digandakan.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SpeechModule {

    @Binds
    @Singleton
    abstract fun bindSpeechRecognizerRepository(
        impl: AndroidSpeechRecognizerRepository,
    ): SpeechRecognizerRepository

    @Binds
    @Singleton
    abstract fun bindTextToSpeechRepository(
        impl: AndroidTextToSpeechRepository,
    ): TextToSpeechRepository
}
