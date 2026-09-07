package com.agribicara.app.domain.model

/**
 * Model domain deteksi penyakit padi (Fase 4).
 *
 * Sama seperti [Chat] dan [Forecast], paket `domain/` tidak mengimpor apa pun
 * dari `data/`. Satu-satunya jenis nilai mentah yang lewat batas ini adalah
 * skor model ([DiseasePrediction]); pemetaan ke keputusan yang bisa ditindak
 * lanjuti ([DetectionOutcome]) dikerjakan `domain/ml/DiseaseClassificationPolicy`.
 */

/**
 * Satu keluaran mentah model: label kelas + skor keyakinannya (0f..1f).
 *
 * [label] adalah kunci kelas apa adanya dari model (mis. "blast", "brown_spot",
 * "normal"), BUKAN teks yang ditampilkan — penerjemahan ke Bahasa Indonesia
 * ada di `DiseaseCatalog`.
 */
data class DiseasePrediction(
    val label: String,
    val confidence: Float,
)

/**
 * Info tampilan sebuah penyakit dalam Bahasa Indonesia.
 *
 * Sengaja TANPA saran obat/pestisida atau takaran: sama dengan sikap Fase 5,
 * aplikasi tidak menyebut dosis kimia. Hanya nama dan ciri yang terlihat.
 */
data class DiseaseInfo(
    val label: String,
    val displayName: String,
    val summary: String,
)

/**
 * Hasil satu deteksi, sudah melewati gerbang keyakinan.
 *
 * Sengaja tiga kemungkinan terpisah, bukan sekadar label + skor: UI menampilkan
 * ketiganya berbeda, dan [Unsure] adalah pengaman inti fase ini — menahan
 * tebakan yang tidak yakin agar tidak muncul sebagai diagnosis pasti.
 */
sealed interface DetectionOutcome {

    /** Model cukup yakin pada sebuah penyakit yang dikenal katalog. */
    data class Diagnosed(
        val label: String,
        val confidence: Float,
        val info: DiseaseInfo,
    ) : DetectionOutcome

    /** Model cukup yakin daun dalam kondisi sehat. */
    data class Healthy(val confidence: Float) : DetectionOutcome

    /**
     * Keyakinan di bawah ambang, atau label tak dikenal katalog.
     *
     * Bukan kegagalan — ini justru perilaku yang diinginkan untuk foto yang
     * bukan daun padi, penyakit di luar kelas model, atau foto yang buruk.
     */
    data class Unsure(val topConfidence: Float) : DetectionOutcome
}

/**
 * Satu baris riwayat deteksi tersimpan.
 *
 * Entity Room dipetakan ke model ini di data layer, sama polanya dengan
 * [ChatMessage]. [label] null untuk hasil [DetectionOutcome.Unsure].
 */
data class DiseaseDetection(
    val id: Long = 0,
    /** Nama konstanta [DetectionOutcomeType]. */
    val outcomeType: DetectionOutcomeType,
    val label: String?,
    val confidence: Float,
    val createdAt: Long,
)

/** Jenis hasil yang disimpan di riwayat; dipetakan ke/dari kolom TEXT Room. */
enum class DetectionOutcomeType {
    DIAGNOSED,
    HEALTHY,
    UNSURE,
}
