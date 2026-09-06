package com.agribicara.app.data.logging

import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Timber tree untuk build RILIS: meneruskan log penting ke Crashlytics.
 *
 * Sengaja dijaga sedangkal mungkin dan tanpa satu pun percabangan sendiri —
 * seluruh keputusannya ada di [CrashLogPolicy], yang punya unit test. Pola dan
 * alasannya sama dengan [com.agribicara.app.data.ai.FirebaseTextGenerator]:
 * kelas yang menyentuh SDK Firebase tidak bisa dijalankan di JVM, jadi ia
 * dikecualikan dari Kover dan kebijakannya diangkat ke tempat yang bisa diuji.
 *
 * **Yang TIDAK pernah dikirim: isi pertanyaan petani dan nama wilayahnya.**
 * README menetapkan "tanpa data pribadi di rilis" sejak Fase 1, dan pertanyaan
 * suara adalah kalimat bebas yang bisa memuat apa saja — nama orang, nama desa,
 * keluhan pribadi. Yang dikirim hanyalah pesan log yang ditulis developer, dan
 * itu menjadi tanggung jawab setiap pemanggil Timber.
 */
class CrashReportingTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean =
        CrashLogPolicy.shouldReport(priority)

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val crashlytics = FirebaseCrashlytics.getInstance()

        // Jejak untuk membaca urutan kejadian sebelum crash. Tanpa Firebase
        // Analytics, inilah satu-satunya breadcrumb yang dimiliki proyek ini —
        // Analytics sengaja tidak dipasang di Fase 8, lihat plan bagian
        // NOT Building.
        crashlytics.log(if (tag != null) "$tag: $message" else message)

        if (t != null && CrashLogPolicy.shouldRecordAsNonFatal(priority)) {
            crashlytics.recordException(t)
        }
    }
}
