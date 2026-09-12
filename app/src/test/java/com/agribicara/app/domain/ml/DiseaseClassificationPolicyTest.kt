package com.agribicara.app.domain.ml

import com.agribicara.app.domain.model.ConfidenceBand
import com.agribicara.app.domain.model.DetectionOutcome
import com.agribicara.app.domain.model.DiseasePrediction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Memaku pengaman inti Fase 4: gerbang keyakinan.
 *
 * Model klasifikasi bersifat closed-set — ia selalu mengembalikan salah satu
 * kelasnya untuk foto APA PUN. Tanpa gerbang ini, foto daun sehat, penyakit di
 * luar kelas, tangan, atau foto buram akan tampil sebagai diagnosis yang
 * terlihat yakin padahal salah, dan itu bisa membuat petani salah bertindak.
 * Test ini menjaga agar hasil di bawah ambang jatuh ke "belum yakin", bukan
 * dipaksa jadi diagnosis.
 */
class DiseaseClassificationPolicyTest {

    private val threshold = 0.70f
    private val strongThreshold = 0.80f

    @Test
    fun `keyakinan tinggi pada penyakit dikenal menghasilkan Diagnosed`() {
        val outcome = DiseaseClassificationPolicy.decide(
            predictions = listOf(
                DiseasePrediction("blast", 0.92f),
                DiseasePrediction("brown_spot", 0.05f),
            ),
            threshold = threshold,
            strongThreshold = strongThreshold,
        )

        assertTrue(outcome is DetectionOutcome.Diagnosed)
        outcome as DetectionOutcome.Diagnosed
        assertEquals("blast", outcome.label)
        assertEquals("Blas", outcome.info.displayName)
    }

    @Test
    fun `keyakinan di bawah ambang menghasilkan Unsure, bukan diagnosis paksa`() {
        val outcome = DiseaseClassificationPolicy.decide(
            predictions = listOf(
                DiseasePrediction("blast", 0.40f),
                DiseasePrediction("brown_spot", 0.35f),
            ),
            threshold = threshold,
            strongThreshold = strongThreshold,
        )

        assertTrue(
            "Skor tertinggi 0.40 di bawah ambang 0.70 harus jadi Unsure. Menampilkan " +
                "label paling mungkin apa adanya adalah persis risiko yang memblokir Fase 4.",
            outcome is DetectionOutcome.Unsure,
        )
    }

    @Test
    fun `label sehat menghasilkan Healthy`() {
        val outcome = DiseaseClassificationPolicy.decide(
            predictions = listOf(DiseasePrediction(DiseaseCatalog.NORMAL_LABEL, 0.88f)),
            threshold = threshold,
            strongThreshold = strongThreshold,
        )

        assertTrue(outcome is DetectionOutcome.Healthy)
    }

    @Test
    fun `label yakin tetapi tak dikenal katalog jatuh ke Unsure`() {
        // Model versi berikutnya menambah kelas yang belum ada di DiseaseCatalog:
        // lebih baik mengaku belum yakin daripada menampilkan label mentah.
        val outcome = DiseaseClassificationPolicy.decide(
            predictions = listOf(DiseasePrediction("kelas_baru_belum_dikatalog", 0.95f)),
            threshold = threshold,
            strongThreshold = strongThreshold,
        )

        assertTrue(outcome is DetectionOutcome.Unsure)
    }

    @Test
    fun `daftar prediksi kosong menghasilkan Unsure tanpa melempar`() {
        val outcome = DiseaseClassificationPolicy.decide(
            predictions = emptyList(),
            threshold = threshold,
            strongThreshold = strongThreshold,
        )

        assertTrue(outcome is DetectionOutcome.Unsure)
        assertEquals(0f, (outcome as DetectionOutcome.Unsure).topConfidence, 0f)
    }

    @Test
    fun `argmax memilih skor tertinggi, bukan yang pertama`() {
        val outcome = DiseaseClassificationPolicy.decide(
            predictions = listOf(
                DiseasePrediction("brown_spot", 0.20f),
                DiseasePrediction("blast", 0.90f),
                DiseasePrediction("tungro", 0.10f),
            ),
            threshold = threshold,
            strongThreshold = strongThreshold,
        )

        outcome as DetectionOutcome.Diagnosed
        assertEquals("blast", outcome.label)
    }

    @Test
    fun `keyakinan di atas ambang kuat menghasilkan pita KUAT`() {
        val outcome = DiseaseClassificationPolicy.decide(
            predictions = listOf(DiseasePrediction("blast", 0.97f)),
            threshold = threshold,
            strongThreshold = strongThreshold,
        )

        outcome as DetectionOutcome.Diagnosed
        assertEquals(ConfidenceBand.STRONG, outcome.band)
    }

    @Test
    fun `keyakinan tepat di ambang kuat sudah terhitung KUAT`() {
        // Batas inklusif, sama dengan gerbang tampil (>= threshold). Dua aturan
        // batas yang berbeda arah di satu kebijakan hanya mengundang salah baca.
        val outcome = DiseaseClassificationPolicy.decide(
            predictions = listOf(DiseasePrediction("blast", 0.80f)),
            threshold = threshold,
            strongThreshold = strongThreshold,
        )

        outcome as DetectionOutcome.Diagnosed
        assertEquals(ConfidenceBand.STRONG, outcome.band)
    }

    @Test
    fun `keyakinan tampil tetapi di bawah ambang kuat menghasilkan pita LEMAH`() {
        // Inti Utang #2: sebelum ini 0.79 dan 0.97 tampil identik di layar,
        // padahal dari tabel kalibrasi pita 0.60-0.80 hanya benar 66,3% —
        // dua dari tiga — sedangkan >= 0.80 benar 96,2%.
        val outcome = DiseaseClassificationPolicy.decide(
            predictions = listOf(DiseasePrediction("blast", 0.79f)),
            threshold = threshold,
            strongThreshold = strongThreshold,
        )

        outcome as DetectionOutcome.Diagnosed
        assertEquals(ConfidenceBand.WEAK, outcome.band)
    }

    @Test
    fun `hasil sehat juga membawa pita, karena sehat yang keliru sama berbahayanya`() {
        // "Daun tampak sehat" yang salah membuat petani membiarkan penyakit
        // menyebar. Gerbangnya harus setegas sisi penyakit.
        val lemah = DiseaseClassificationPolicy.decide(
            predictions = listOf(DiseasePrediction(DiseaseCatalog.NORMAL_LABEL, 0.72f)),
            threshold = threshold,
            strongThreshold = strongThreshold,
        )
        val kuat = DiseaseClassificationPolicy.decide(
            predictions = listOf(DiseasePrediction(DiseaseCatalog.NORMAL_LABEL, 0.95f)),
            threshold = threshold,
            strongThreshold = strongThreshold,
        )

        assertEquals(ConfidenceBand.WEAK, (lemah as DetectionOutcome.Healthy).band)
        assertEquals(ConfidenceBand.STRONG, (kuat as DetectionOutcome.Healthy).band)
    }
}
