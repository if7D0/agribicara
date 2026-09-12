package com.agribicara.app.domain.ml

import com.agribicara.app.domain.model.ConfidenceBand
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
 * Satu gerbang saja ternyata belum cukup: hasil tepat di atas ambang dan hasil
 * berkeyakinan tinggi sama-sama lolos, lalu tampil identik di layar. Karena itu
 * setiap hasil positif diberi [ConfidenceBand] lewat gerbang kedua
 * [strongThreshold], supaya UI bisa menyebut yang lemah sebagai dugaan lemah
 * alih-alih menyerahkan bedanya pada angka persen kecil.
 *
 * Objek murni tanpa Android/TFLite, punya unit test
 * (`DiseaseClassificationPolicyTest`). Mengikuti pola `CrashLogPolicy` (Fase 8):
 * kebijakan diangkat keluar dari pembungkus penyentuh-SDK agar bisa diuji.
 */
object DiseaseClassificationPolicy {

    /**
     * @param predictions keluaran mentah model (boleh kosong)
     * @param threshold keyakinan minimum agar sebuah dugaan ditampilkan
     * @param strongThreshold keyakinan minimum agar hasil disebut [ConfidenceBand.STRONG];
     *   di bawahnya (tetapi masih di atas [threshold]) hasilnya [ConfidenceBand.WEAK].
     *   Bila nilainya lebih rendah dari [threshold], semua hasil positif menjadi
     *   STRONG — tidak dijaga dengan lemparan, karena kontrak lapisan ini tidak
     *   melempar dan keduanya berasal dari `Constants` yang sama.
     */
    fun decide(
        predictions: List<DiseasePrediction>,
        threshold: Float,
        strongThreshold: Float,
    ): DetectionOutcome {
        // Tanpa prediksi sama sekali: perlakukan sebagai tidak yakin, bukan
        // melempar. Bisa terjadi bila model mengembalikan vektor kosong.
        val top = predictions.maxByOrNull { it.confidence }
            ?: return DetectionOutcome.Unsure(topConfidence = 0f)

        if (top.confidence < threshold) {
            return DetectionOutcome.Unsure(topConfidence = top.confidence)
        }

        // Batas pita inklusif di sisi bawah, sama arah dengan gerbang tampil.
        val band = if (top.confidence >= strongThreshold) {
            ConfidenceBand.STRONG
        } else {
            ConfidenceBand.WEAK
        }

        if (DiseaseCatalog.isHealthy(top.label)) {
            return DetectionOutcome.Healthy(confidence = top.confidence, band = band)
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
            band = band,
        )
    }
}
