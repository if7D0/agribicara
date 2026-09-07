package com.agribicara.app.data.ml

import android.net.Uri
import com.agribicara.app.domain.model.DiseasePrediction

/**
 * Batas tipis ke mesin inferensi gambar.
 *
 * Mengikuti pola [com.agribicara.app.data.ai.AiTextGenerator]: kelas
 * penyentuh-SDK di balik interface, sehingga kebijakan
 * (`DiseaseClassificationPolicy`) dan orkestrasi (`DiseaseRepositoryImpl`) bisa
 * diuji tanpa Firebase ML maupun TFLite.
 *
 * Menerima [Uri] (bukan Bitmap) supaya seluruh urusan decode gambar tetap di
 * satu tempat penyentuh-Android, dan test bisa memakai fake yang mengabaikan
 * input.
 */
interface ImageClassifier {

    /**
     * Mengklasifikasi gambar pada [imageUri] menjadi daftar skor per kelas.
     *
     * KONTRAK: melempar [ModelUnavailableException] bila model belum bisa
     * diperoleh (mis. belum pernah terunduh dan perangkat sedang offline), dan
     * exception lain untuk kegagalan tak terduga. Pemetaan ke pesan Bahasa
     * Indonesia adalah tugas [com.agribicara.app.data.repository.DiseaseRepositoryImpl].
     */
    suspend fun classify(imageUri: Uri): List<DiseasePrediction>
}

/**
 * Model deteksi belum tersedia: berkas model/label belum ada di assets aplikasi
 * (mis. build sebelum Task 0 mengunggah model terlatih).
 *
 * Dibedakan dari kegagalan lain karena artinya berbeda — fitur belum aktif di
 * versi ini, bukan sekadar "coba lagi".
 */
class ModelUnavailableException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
