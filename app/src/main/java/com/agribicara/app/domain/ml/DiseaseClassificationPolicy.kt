package com.agribicara.app.domain.ml

import com.agribicara.app.domain.model.DetectionOutcome
import com.agribicara.app.domain.model.DiseasePrediction

/**
 * Mengubah skor mentah model → keputusan yang bisa ditindaklanjuti.
 *
 * Ini pengaman inti Fase 4, dan alasan fase ini pernah di-block: model
 * klasifikasi bersifat closed-set — ia akan SELALU mengembalikan salah satu
 * kelasnya, bahkan untuk foto daun sehat, penyakit di luar kelasnya, tangan,
 * atau foto buram. Menampilkan label paling mungkin apa adanya berarti
 * diagnosis yang terlihat yakin padahal salah, dan itu bisa membuat petani
 * salah bertindak.
 *
 * Karena itu ada gerbang [threshold]: di bawahnya hasilnya
 * [DetectionOutcome.Unsure], bukan diagnosis. Kelas sehat dipetakan ke
 * [DetectionOutcome.Healthy]. Label yang tak dikenal katalog juga jatuh ke
 * Unsure — sehingga model versi berikutnya yang menambah kelas tidak membuat
 * aplikasi menampilkan label mentah tanpa terjemahan.
 *
 * Objek murni tanpa Android/TFLite, punya unit test
 * (`DiseaseClassificationPolicyTest`). Mengikuti pola `CrashLogPolicy` (Fase 8):
 * kebijakan diangkat keluar dari pembungkus penyentuh-SDK agar bisa diuji.
 */
object DiseaseClassificationPolicy {

    /**
     * @param predictions keluaran mentah model (boleh kosong)
     * @param threshold keyakinan minimum agar sebuah dugaan ditampilkan
     */
    fun decide(
        predictions: List<DiseasePrediction>,
        threshold: Float,
    ): DetectionOutcome {
        // Tanpa prediksi sama sekali: perlakukan sebagai tidak yakin, bukan
        // melempar. Bisa terjadi bila model mengembalikan vektor kosong.
        val top = predictions.maxByOrNull { it.confidence }
            ?: return DetectionOutcome.Unsure(topConfidence = 0f)

        if (top.confidence < threshold) {
            return DetectionOutcome.Unsure(topConfidence = top.confidence)
        }

        if (DiseaseCatalog.isHealthy(top.label)) {
            return DetectionOutcome.Healthy(confidence = top.confidence)
        }

        // Label yakin tetapi tak dikenal katalog → tetap Unsure. Menampilkan
        // label mentah tanpa terjemahan lebih menyesatkan daripada mengaku
        // belum yakin.
        val info = DiseaseCatalog.infoFor(top.label)
            ?: return DetectionOutcome.Unsure(topConfidence = top.confidence)

        return DetectionOutcome.Diagnosed(
            label = top.label,
            confidence = top.confidence,
            info = info,
        )
    }
}
