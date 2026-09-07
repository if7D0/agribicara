package com.agribicara.app.di

import com.agribicara.app.data.ml.ImageClassifier
import com.agribicara.app.data.ml.TfliteImageClassifier
import com.agribicara.app.data.repository.DiseaseRepositoryImpl
import com.agribicara.app.domain.repository.DiseaseRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Mengikat kontrak deteksi penyakit (Fase 4) ke implementasinya.
 *
 * Dipisah dari [RepositoryModule]/[AiModule] supaya penggantian mesin inferensi
 * (mis. dari TFLite ke jalur lain) terlihat sebagai satu perubahan terisolasi —
 * pola yang sama dengan pemisahan AiModule.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MlModule {

    @Binds
    @Singleton
    abstract fun bindDiseaseRepository(impl: DiseaseRepositoryImpl): DiseaseRepository

    /**
     * Batas ke Firebase ML + TFLite, dipisah dari repository-nya supaya
     * kebijakan (ambang, pemetaan) bisa diuji tanpa SDK. Lihat [ImageClassifier].
     */
    @Binds
    @Singleton
    abstract fun bindImageClassifier(impl: TfliteImageClassifier): ImageClassifier
}
