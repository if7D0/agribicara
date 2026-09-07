package com.agribicara.app.data.repository

import android.content.Context
import android.net.Uri
import com.agribicara.app.R
import com.agribicara.app.core.common.Constants
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.data.local.dao.DetectionDao
import com.agribicara.app.data.local.entity.DetectionEntity
import com.agribicara.app.data.ml.ImageClassifier
import com.agribicara.app.data.ml.ModelUnavailableException
import com.agribicara.app.domain.ml.DiseaseClassificationPolicy
import com.agribicara.app.domain.model.DetectionOutcome
import com.agribicara.app.domain.model.DetectionOutcomeType
import com.agribicara.app.domain.model.DiseaseDetection
import com.agribicara.app.domain.repository.DiseaseRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Orkestrasi deteksi: classifier (SDK) → kebijakan (teruji) → riwayat.
 *
 * Mengikuti kontrak [DiseaseRepository]/[NetworkResult]: tidak pernah melempar
 * melewati batas kecuali pembatalan coroutine; setiap kegagalan menjadi pesan
 * Bahasa Indonesia dari `strings.xml`. Keputusan keyakinan sepenuhnya di
 * [DiseaseClassificationPolicy] agar kelas ini tetap dangkal dan teruji.
 */
@Singleton
class DiseaseRepositoryImpl @Inject constructor(
    private val classifier: ImageClassifier,
    private val detectionDao: DetectionDao,
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) : DiseaseRepository {

    override suspend fun classify(imageUri: Uri): NetworkResult<DetectionOutcome> = try {
        val predictions = classifier.classify(imageUri)
        val outcome = DiseaseClassificationPolicy.decide(
            predictions = predictions,
            threshold = Constants.DISEASE_CONFIDENCE_THRESHOLD,
        )
        persist(outcome)
        NetworkResult.Success(outcome)
    } catch (e: CancellationException) {
        // Menelan pembatalan coroutine akan merusak structured concurrency.
        throw e
    } catch (e: ModelUnavailableException) {
        // Tindakannya berbeda: butuh internet sekali untuk mengunduh model,
        // bukan sekadar "coba lagi".
        Timber.w(e, "Model deteksi belum tersedia")
        NetworkResult.Error(context.getString(R.string.error_detection_model_unavailable), e)
    } catch (e: Exception) {
        Timber.e(e, "Deteksi penyakit gagal")
        NetworkResult.Error(context.getString(R.string.error_detection_failed), e)
    }

    override fun observeHistory(limit: Int): Flow<List<DiseaseDetection>> =
        detectionDao.observeRecent(limit).map { entities ->
            entities.mapNotNull { it.toDomain() }
        }

    private suspend fun persist(outcome: DetectionOutcome) {
        detectionDao.insert(outcome.toEntity(clock.millis()))
        detectionDao.trimTo(Constants.DETECTION_HISTORY_LIMIT)
    }
}

private fun DetectionOutcome.toEntity(now: Long): DetectionEntity = when (this) {
    is DetectionOutcome.Diagnosed -> DetectionEntity(
        outcomeType = DetectionOutcomeType.DIAGNOSED.name,
        label = label,
        confidence = confidence,
        createdAt = now,
    )
    is DetectionOutcome.Healthy -> DetectionEntity(
        outcomeType = DetectionOutcomeType.HEALTHY.name,
        label = null,
        confidence = confidence,
        createdAt = now,
    )
    is DetectionOutcome.Unsure -> DetectionEntity(
        outcomeType = DetectionOutcomeType.UNSURE.name,
        label = null,
        confidence = topConfidence,
        createdAt = now,
    )
}

/** Baris dengan outcomeType tak dikenal (dari versi app lain) dibuang, bukan crash. */
private fun DetectionEntity.toDomain(): DiseaseDetection? {
    val type = DetectionOutcomeType.entries.firstOrNull { it.name == outcomeType } ?: return null
    return DiseaseDetection(
        id = id,
        outcomeType = type,
        label = label,
        confidence = confidence,
        createdAt = createdAt,
    )
}
