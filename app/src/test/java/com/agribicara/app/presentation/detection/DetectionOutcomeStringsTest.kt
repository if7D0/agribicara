package com.agribicara.app.presentation.detection

import com.agribicara.app.R
import com.agribicara.app.domain.model.ConfidenceBand
import com.agribicara.app.domain.model.DetectionOutcome
import com.agribicara.app.domain.model.DiseaseInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Memaku pemetaan hasil → teks yang dibaca petani.
 *
 * Kelas kesalahan yang dijaga di sini halus: id resource yang tertukar tetap
 * String yang sah, jadi kompilasi, lint, dan kover semuanya tetap hijau
 * sementara kartu daun sehat berjudul "Dugaan lemah". Hanya perbandingan id
 * yang eksplisit seperti ini yang menangkapnya tanpa perangkat.
 */
class DetectionOutcomeStringsTest {

    private val info = DiseaseInfo(
        label = "blast",
        displayName = "Blas",
        summary = "Bercak berbentuk belah ketupat pada daun.",
    )

    private fun diagnosed(band: ConfidenceBand) = DetectionOutcome.Diagnosed(
        label = "blast",
        confidence = if (band == ConfidenceBand.STRONG) 0.97f else 0.72f,
        info = info,
        band = band,
    )

    private fun healthy(band: ConfidenceBand) = DetectionOutcome.Healthy(
        confidence = if (band == ConfidenceBand.STRONG) 0.95f else 0.72f,
        band = band,
    )

    @Test
    fun `judul dugaan penyakit berbeda antara pita kuat dan lemah`() {
        assertEquals(
            R.string.detection_diagnosed_label,
            DetectionOutcomeStrings.titleFor(diagnosed(ConfidenceBand.STRONG)),
        )
        assertEquals(
            R.string.detection_diagnosed_label_weak,
            DetectionOutcomeStrings.titleFor(diagnosed(ConfidenceBand.WEAK)),
        )
    }

    @Test
    fun `judul daun sehat berbeda antara pita kuat dan lemah`() {
        assertEquals(
            R.string.detection_healthy_title,
            DetectionOutcomeStrings.titleFor(healthy(ConfidenceBand.STRONG)),
        )
        assertEquals(
            R.string.detection_healthy_title_weak,
            DetectionOutcomeStrings.titleFor(healthy(ConfidenceBand.WEAK)),
        )
    }

    @Test
    fun `judul sehat dan judul penyakit tidak pernah tertukar`() {
        // Justru inilah kesalahan yang tidak terlihat di layar mana pun sampai
        // seorang petani membacanya: keempat judul harus berbeda satu sama lain.
        val judul = listOf(
            DetectionOutcomeStrings.titleFor(diagnosed(ConfidenceBand.STRONG)),
            DetectionOutcomeStrings.titleFor(diagnosed(ConfidenceBand.WEAK)),
            DetectionOutcomeStrings.titleFor(healthy(ConfidenceBand.STRONG)),
            DetectionOutcomeStrings.titleFor(healthy(ConfidenceBand.WEAK)),
        )

        assertEquals("Keempat judul harus unik", judul.size, judul.toSet().size)
    }

    @Test
    fun `hasil belum yakin memakai judulnya sendiri`() {
        assertEquals(
            R.string.detection_unsure_title,
            DetectionOutcomeStrings.titleFor(DetectionOutcome.Unsure(topConfidence = 0.4f)),
        )
    }

    @Test
    fun `peringatan pita lemah hanya muncul pada hasil positif berpita lemah`() {
        assertTrue(DetectionOutcomeStrings.showsWeakWarning(diagnosed(ConfidenceBand.WEAK)))
        assertTrue(DetectionOutcomeStrings.showsWeakWarning(healthy(ConfidenceBand.WEAK)))
        assertFalse(DetectionOutcomeStrings.showsWeakWarning(diagnosed(ConfidenceBand.STRONG)))
        assertFalse(DetectionOutcomeStrings.showsWeakWarning(healthy(ConfidenceBand.STRONG)))
    }

    @Test
    fun `hasil belum yakin tidak menumpuk peringatan pita`() {
        assertFalse(
            "Layar 'belum yakin' sudah seluruhnya berisi ajakan foto ulang; " +
                "menumpuk dua peringatan melemahkan keduanya.",
            DetectionOutcomeStrings.showsWeakWarning(DetectionOutcome.Unsure(topConfidence = 0.4f)),
        )
    }
}
