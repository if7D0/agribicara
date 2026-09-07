package com.agribicara.app.domain.repository

import android.net.Uri
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.domain.model.DetectionOutcome
import com.agribicara.app.domain.model.DiseaseDetection
import kotlinx.coroutines.flow.Flow

/**
 * Deteksi penyakit padi dari sebuah foto (Fase 4).
 *
 * Kontrak (lihat [NetworkResult]): implementasi TIDAK PERNAH melempar exception
 * melewati batas ini kecuali pembatalan coroutine. Setiap kegagalan menjadi
 * [NetworkResult.Error] dengan pesan Bahasa Indonesia sederhana.
 *
 * CATATAN: [classify] menerima [Uri] — satu-satunya tipe Android yang diizinkan
 * masuk ke `domain/` di proyek ini, karena input fitur ini memang inheren
 * sebuah gambar dari kamera/galeri. Seluruh decode + pra-proses + inferensi
 * terjadi di data layer; domain tidak menyentuh piksel.
 */
interface DiseaseRepository {

    /**
     * Mengklasifikasi gambar pada [imageUri] dan menyimpannya ke riwayat.
     *
     * Hasil sudah melewati gerbang keyakinan: bisa [DetectionOutcome.Diagnosed],
     * [DetectionOutcome.Healthy], atau [DetectionOutcome.Unsure]. Unsure adalah
     * hasil yang SAH (bukan error) — foto yang bukan daun padi atau tak jelas.
     */
    suspend fun classify(imageUri: Uri): NetworkResult<DetectionOutcome>

    /** Riwayat deteksi terbaru, terbaru dulu. */
    fun observeHistory(limit: Int): Flow<List<DiseaseDetection>>
}
