package com.agribicara.app.presentation.detection

import androidx.annotation.StringRes
import com.agribicara.app.R
import com.agribicara.app.domain.model.ConfidenceBand
import com.agribicara.app.domain.model.DetectionOutcome

/**
 * Memetakan satu [DetectionOutcome] ke judul dan peringatan yang dilihat petani.
 *
 * Diangkat keluar dari composable dengan alasan yang sama seperti
 * [com.agribicara.app.data.speech.SpeechErrorMapper] dan `CrashLogPolicy`:
 * project ini sengaja tidak memakai Robolectric, jadi apa pun yang tinggal di
 * dalam `@Composable` hanya bisa diuji lewat instrumented test — yang menuntut
 * perangkat dan tidak dijalankan CI.
 *
 * Yang dijaga di sini bukan gaya, melainkan satu kelas kesalahan yang tidak
 * akan tertangkap gerbang mana pun: tertukarnya id resource. Judul "Dugaan
 * lemah" yang nyasar ke kartu daun sehat tetap String yang sah, tetap lolos
 * kompilasi, lint, dan kover — dan salah pesan itu baru terlihat oleh petani.
 */
internal object DetectionOutcomeStrings {

    /** Judul kartu hasil; berbeda per [ConfidenceBand] untuk dua hasil positif. */
    @StringRes
    fun titleFor(outcome: DetectionOutcome): Int = when (outcome) {
        is DetectionOutcome.Diagnosed -> when (outcome.band) {
            ConfidenceBand.STRONG -> R.string.detection_diagnosed_label
            ConfidenceBand.WEAK -> R.string.detection_diagnosed_label_weak
        }

        is DetectionOutcome.Healthy -> when (outcome.band) {
            ConfidenceBand.STRONG -> R.string.detection_healthy_title
            ConfidenceBand.WEAK -> R.string.detection_healthy_title_weak
        }

        is DetectionOutcome.Unsure -> R.string.detection_unsure_title
    }

    /**
     * Apakah kartu perlu memuat peringatan pita lemah.
     *
     * [DetectionOutcome.Unsure] TIDAK memakainya: layar itu sudah seluruhnya
     * berisi ajakan foto ulang, dan menumpuk dua peringatan justru melemahkan
     * keduanya.
     */
    fun showsWeakWarning(outcome: DetectionOutcome): Boolean = when (outcome) {
        is DetectionOutcome.Diagnosed -> outcome.band == ConfidenceBand.WEAK
        is DetectionOutcome.Healthy -> outcome.band == ConfidenceBand.WEAK
        is DetectionOutcome.Unsure -> false
    }
}
