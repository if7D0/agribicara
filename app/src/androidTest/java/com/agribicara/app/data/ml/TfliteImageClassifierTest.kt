package com.agribicara.app.data.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agribicara.app.domain.ml.DiseaseCatalog
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Kontrak model TFLite yang DIBUNDEL, diuji terhadap aset sungguhan di perangkat.
 *
 * Ada karena kegagalan nyata: model dikonversi konverter Colab baru memancarkan
 * FULLY_CONNECTED v12, sedangkan runtime di-pin 2.16.1 (max v11), sehingga
 * `Interpreter` gagal dibangun dan pengguna hanya melihat "Foto belum bisa
 * diperiksa". Unit test tidak bisa menangkapnya — TFLite butuh perangkat.
 *
 * Test ini TIDAK menguji akurasi (butuh foto berlabel); ia menguji bahwa model
 * MUAT dan bentuk keluarannya cocok dengan label + katalog.
 */
@RunWith(AndroidJUnit4::class)
class TfliteImageClassifierTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val classifier = TfliteImageClassifier(context)

    @Test
    fun classify_memuatModelBundel_danMengembalikanSatuSkorPerLabel() = runTest {
        val predictions = classifier.classify(syntheticImageUri())

        // Runtime sanggup membangun Interpreter dari aset (regresi FULLY_CONNECTED v12).
        assertEquals(
            "Jumlah keluaran model harus sama dengan jumlah label",
            EXPECTED_LABEL_COUNT,
            predictions.size,
        )
        predictions.forEach { p ->
            assertTrue(
                "Keyakinan ${p.label} harus di [0,1], bukan ${p.confidence}",
                p.confidence in 0f..1f,
            )
        }
        val total = predictions.sumOf { it.confidence.toDouble() }
        assertEquals("Keluaran harus softmax (jumlah ~1)", 1.0, total, 0.01)
    }

    @Test
    fun setiapLabelModel_dikenaliKatalog() = runTest {
        val predictions = classifier.classify(syntheticImageUri())

        predictions.forEach { p ->
            val dikenali = DiseaseCatalog.isHealthy(p.label) || DiseaseCatalog.infoFor(p.label) != null
            assertTrue(
                "Label '${p.label}' dari model tidak ada di DiseaseCatalog — " +
                    "urutan/nama label dan katalog harus sinkron",
                dikenali,
            )
        }
    }

    @Test
    fun classify_duaKali_memakaiUlangInterpreter() = runTest {
        val uri = syntheticImageUri()

        val first = classifier.classify(uri)
        val second = classifier.classify(uri)

        assertNotNull(first)
        assertEquals("Inferensi harus deterministik untuk gambar yang sama", first, second)
    }

    /** Gambar sintetis (hijau polos) — cukup untuk menguji pemuatan & bentuk, bukan akurasi. */
    private fun syntheticImageUri(): Uri {
        val bitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.rgb(60, 140, 60))
        val file = File(context.cacheDir, "tflite-test-input.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return Uri.fromFile(file)
    }

    private companion object {
        const val EXPECTED_LABEL_COUNT = 10
    }
}
