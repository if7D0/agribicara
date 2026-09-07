package com.agribicara.app.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.agribicara.app.core.common.Constants
import com.agribicara.app.domain.model.DiseasePrediction
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter

/**
 * Inferensi on-device dengan model TFLite yang DIBUNDEL di assets aplikasi.
 *
 * Rencana awal memakai Firebase ML Model Hosting (unduh online), tetapi layanan
 * itu sudah deprecated (shutdown Juni 2027) dan UI unggahnya dihapus dari
 * Console. Model kini di-bundel di `app/src/main/assets/` dan dimuat langsung —
 * tanpa jaringan, tanpa backend, tanpa App Check untuk model. Pembaruan model
 * berarti rilis aplikasi baru; itu pertukaran yang diterima untuk v1.
 *
 * Kelas ini SATU-SATUNYA yang menyentuh TFLite + decode Uri — sengaja dangkal,
 * tanpa kebijakan. Semua keputusan (ambang keyakinan, pemetaan label) ada di
 * `DiseaseClassificationPolicy`/`DiseaseCatalog` yang teruji. Karena itu kelas
 * ini dikecualikan Kover, mengikuti pola
 * [com.agribicara.app.data.ai.FirebaseTextGenerator].
 *
 * Interpreter + label dibangun sekali di balik [mutex] (Interpreter tidak
 * thread-safe; classify dipanggil serial dari ViewModel, mutex menjaga bila
 * kelak bersamaan).
 */
@Singleton
class TfliteImageClassifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : ImageClassifier {

    private val mutex = Mutex()
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    override suspend fun classify(imageUri: Uri): List<DiseasePrediction> =
        withContext(Dispatchers.Default) {
            val interp = ensureReady()
            val bitmap = decodeBitmap(imageUri)
            val input = preprocess(bitmap)
            val output = Array(1) { FloatArray(labels.size) }
            interp.run(input, output)
            output[0].mapIndexed { index, score ->
                DiseasePrediction(label = labels[index], confidence = score)
            }
        }

    /** Membangun interpreter + label sekali; melempar [ModelUnavailableException] bila asset belum ada. */
    private suspend fun ensureReady(): Interpreter = mutex.withLock {
        interpreter?.let { return it }
        labels = loadLabels()
        Interpreter(loadModelBuffer()).also { interpreter = it }
    }

    private fun loadLabels(): List<String> = try {
        context.assets.open(Constants.DISEASE_LABELS_ASSET)
            .bufferedReader()
            .use { it.readLines() }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .also { require(it.isNotEmpty()) { "Berkas label kosong" } }
    } catch (e: FileNotFoundException) {
        throw ModelUnavailableException(
            "Berkas label ${Constants.DISEASE_LABELS_ASSET} belum ada di assets; model belum disiapkan.",
            e,
        )
    }

    /** Memetakan berkas .tflite di assets ke memori (mmap) untuk Interpreter. */
    private fun loadModelBuffer(): MappedByteBuffer = try {
        context.assets.openFd(Constants.DISEASE_MODEL_ASSET).use { afd ->
            FileInputStream(afd.fileDescriptor).use { fis ->
                fis.channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
            }
        }
    } catch (e: FileNotFoundException) {
        throw ModelUnavailableException(
            "Berkas model ${Constants.DISEASE_MODEL_ASSET} belum ada di assets; model belum disiapkan.",
            e,
        )
    }

    /**
     * Bitmap -> ByteBuffer FLOAT32 [1, size, size, 3] berisi piksel MENTAH
     * [0..255] (MEAN 0, STD 1). Kontrak ini WAJIB cocok dengan pelatihan
     * (`ml/train.py` memakai MobileNetV3 `include_preprocessing=True`).
     */
    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        val size = Constants.DISEASE_MODEL_INPUT_SIZE
        val argb = if (bitmap.config == Bitmap.Config.ARGB_8888) {
            bitmap
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        }
        val scaled = Bitmap.createScaledBitmap(argb, size, size, true)

        val mean = Constants.DISEASE_INPUT_MEAN
        val std = Constants.DISEASE_INPUT_STD
        val buffer = ByteBuffer
            .allocateDirect(BYTES_PER_FLOAT * size * size * CHANNELS)
            .order(ByteOrder.nativeOrder())

        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)
        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF).toFloat()
            val g = ((pixel shr 8) and 0xFF).toFloat()
            val b = (pixel and 0xFF).toFloat()
            buffer.putFloat((r - mean) / std)
            buffer.putFloat((g - mean) / std)
            buffer.putFloat((b - mean) / std)
        }
        buffer.rewind()
        return buffer
    }

    private fun decodeBitmap(imageUri: Uri): Bitmap {
        val resolver = context.contentResolver

        // Pass 1: baca ukuran untuk hitung inSampleSize (hemat memori di
        // perangkat murah — jangan decode foto 12MP penuh lalu langsung buang).
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(imageUri).use { BitmapFactory.decodeStream(it, null, bounds) }

        val target = Constants.DISEASE_MODEL_INPUT_SIZE
        var sample = 1
        var halfW = bounds.outWidth / 2
        var halfH = bounds.outHeight / 2
        while (halfW >= target && halfH >= target) {
            sample *= 2
            halfW /= 2
            halfH /= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(imageUri).use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw ModelUnavailableException("Gambar tidak bisa dibaca.")
    }

    private companion object {
        const val BYTES_PER_FLOAT = 4
        const val CHANNELS = 3
    }
}
