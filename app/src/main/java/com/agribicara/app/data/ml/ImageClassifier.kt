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
     * KONTRAK: melempar [ModelUnavailableException] bila MODEL belum bisa
     * diperoleh (berkas model/label belum ada di assets), [ImageDecodeException]
     * bila GAMBARNYA yang tidak terbaca, dan exception lain untuk kegagalan tak
     * terduga. Pemetaan ke pesan Bahasa Indonesia adalah tugas
     * [com.agribicara.app.data.repository.DiseaseRepositoryImpl].
     *
     * Perbedaan dua yang pertama bukan kerapian belaka: yang satu berarti fitur
     * belum ada di versi aplikasi ini, yang lain berarti cukup potret ulang.
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

/**
 * Gambar yang diberikan tidak bisa di-decode (berkas terpotong, kosong, atau
 * bukan gambar yang dikenali).
 *
 * WAJIB dibedakan dari [ModelUnavailableException]. Keduanya sempat memakai
 * tipe yang sama, dan akibatnya foto rusak dilaporkan kepada petani sebagai
 * "Fitur pemeriksa penyakit belum tersedia di versi aplikasi ini" — salah
 * secara fakta, dan lebih buruk lagi salah secara tindakan: pesan itu tidak
 * menyisakan apa pun untuk dilakukan selain berhenti memakai fitur, padahal
 * yang dibutuhkan hanya memotret ulang.
 *
 * Tidak ditangani khusus di [com.agribicara.app.data.repository.DiseaseRepositoryImpl];
 * ia sengaja jatuh ke cabang kegagalan umum yang pesannya memang sudah tepat
 * ("Foto belum bisa diperiksa. Coba lagi dengan foto yang lebih jelas.").
 * Tipe tersendiri ada supaya niat itu terbaca dan bisa diuji.
 */
class ImageDecodeException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
