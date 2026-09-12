package com.agribicara.app.data.repository

import android.content.Context
import android.net.Uri
import com.agribicara.app.R
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.data.local.dao.DetectionDao
import com.agribicara.app.data.local.entity.DetectionEntity
import com.agribicara.app.data.ml.ImageClassifier
import com.agribicara.app.data.ml.ImageDecodeException
import com.agribicara.app.data.ml.ModelUnavailableException
import com.agribicara.app.domain.model.ConfidenceBand
import com.agribicara.app.domain.model.DetectionOutcome
import com.agribicara.app.domain.model.DetectionOutcomeType
import com.agribicara.app.domain.model.DiseasePrediction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Orkestrasi deteksi: classifier -> kebijakan -> riwayat, dengan pemetaan
 * kegagalan ke pesan Bahasa Indonesia. Classifier di-fake supaya SDK Firebase
 * ML/TFLite tidak dibutuhkan.
 */
class DiseaseRepositoryImplTest {

    private val classifier = mockk<ImageClassifier>()
    private val dao = mockk<DetectionDao>(relaxed = true)
    private val context = mockk<Context>()
    private val clock = Clock.fixed(Instant.parse("2026-09-06T10:00:00Z"), ZoneId.of("UTC"))
    private val uri = mockk<Uri>()

    private lateinit var repository: DiseaseRepositoryImpl

    @Before
    fun setUp() {
        every { context.getString(R.string.error_detection_model_unavailable) } returns "model belum ada"
        every { context.getString(R.string.error_detection_failed) } returns "gagal periksa"
        repository = DiseaseRepositoryImpl(classifier, dao, context, clock)
    }

    @Test
    fun `penyakit yakin menghasilkan Diagnosed dan tersimpan ke riwayat`() = runTest {
        coEvery { classifier.classify(any()) } returns listOf(DiseasePrediction("blast", 0.95f))

        val result = repository.classify(uri)

        assertTrue(result is NetworkResult.Success)
        val outcome = (result as NetworkResult.Success).data
        assertTrue(outcome is DetectionOutcome.Diagnosed)
        assertEquals("blast", (outcome as DetectionOutcome.Diagnosed).label)
        // Menjaga argumen bernama di DiseaseRepositoryImpl tidak tertukar:
        // strongThreshold yang keliru diisi ambang tampil akan menyebut semua
        // hasil KUAT tanpa satu pun test lain yang mengeluh.
        assertEquals(ConfidenceBand.STRONG, (outcome as DetectionOutcome.Diagnosed).band)
        coVerify {
            dao.insert(match { it.outcomeType == DetectionOutcomeType.DIAGNOSED.name && it.label == "blast" })
        }
        coVerify { dao.trimTo(any()) }
    }

    @Test
    fun `keyakinan rendah menghasilkan Unsure dan disimpan tanpa label`() = runTest {
        coEvery { classifier.classify(any()) } returns listOf(DiseasePrediction("blast", 0.30f))

        val result = repository.classify(uri)

        assertTrue((result as NetworkResult.Success).data is DetectionOutcome.Unsure)
        coVerify {
            dao.insert(match { it.outcomeType == DetectionOutcomeType.UNSURE.name && it.label == null })
        }
    }

    @Test
    fun `daun sehat menghasilkan Healthy`() = runTest {
        coEvery { classifier.classify(any()) } returns listOf(DiseasePrediction("normal", 0.90f))

        val result = repository.classify(uri)

        assertTrue((result as NetworkResult.Success).data is DetectionOutcome.Healthy)
    }

    @Test
    fun `model belum tersedia menghasilkan pesan khusus, bukan pesan gagal umum`() = runTest {
        coEvery { classifier.classify(any()) } throws ModelUnavailableException("no model")

        val result = repository.classify(uri)

        assertTrue(result is NetworkResult.Error)
        assertEquals("model belum ada", (result as NetworkResult.Error).message)
    }

    @Test
    fun `gambar tak terbaca menyuruh foto ulang, BUKAN mengaku fitur tak tersedia`() = runTest {
        // Regresi review Fase 4 (H1). Sebelumnya decode gagal dilempar sebagai
        // ModelUnavailableException, sehingga foto rusak — hal yang lumrah bila
        // penyimpanan penuh dan penulisan kamera terputus — dilaporkan kepada
        // petani sebagai "Fitur pemeriksa penyakit belum tersedia di versi
        // aplikasi ini". Salah secara fakta, dan lebih buruk lagi salah secara
        // tindakan: pesan itu menyuruhnya berhenti memakai fitur, padahal cukup
        // memotret ulang.
        coEvery { classifier.classify(any()) } throws ImageDecodeException("tidak terbaca")

        val result = repository.classify(uri)

        assertEquals("gagal periksa", (result as NetworkResult.Error).message)
    }

    @Test
    fun `kegagalan tak terduga menghasilkan pesan gagal umum`() = runTest {
        coEvery { classifier.classify(any()) } throws IllegalStateException("boom")

        val result = repository.classify(uri)

        assertEquals("gagal periksa", (result as NetworkResult.Error).message)
    }

    @Test
    fun `observeHistory memetakan entity ke domain dan membuang tipe tak dikenal`() = runTest {
        every { dao.observeRecent(any()) } returns flowOf(
            listOf(
                DetectionEntity(1, DetectionOutcomeType.DIAGNOSED.name, "blast", 0.9f, 1L),
                DetectionEntity(2, "TIPE_ASING", null, 0.1f, 2L),
            ),
        )

        val history = repository.observeHistory(10).first()

        assertEquals(1, history.size)
        assertEquals(DetectionOutcomeType.DIAGNOSED, history[0].outcomeType)
        assertEquals("blast", history[0].label)
    }
}
