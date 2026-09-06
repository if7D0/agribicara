package com.agribicara.app.data.logging

import android.util.Log

/**
 * Level log mana yang layak menempuh jaringan menuju Crashlytics.
 *
 * VERBOSE, DEBUG, dan INFO dibuang. Dua alasannya, dan keduanya berpihak pada
 * petani: di build rilis ketiganya memang tidak pernah ditanam (lihat
 * [com.agribicara.app.AgriBicaraApp]), dan meneruskannya hanya menghabiskan
 * kuota data justru pada saat sinyal sedang buruk — yaitu saat error terjadi.
 *
 * ASSERT (`Log.wtf`) DITERUSKAN dengan sengaja. Ia berada di atas ERROR dan
 * menandai keadaan yang menurut kode seharusnya mustahil; membuangnya berarti
 * kehilangan justru laporan yang paling perlu dilihat. Diputuskan eksplisit di
 * sini, bukan dibiarkan jatuh ke cabang `else` tanpa siapa pun menyadarinya.
 *
 * Objek murni tanpa Android SDK selain konstanta [Log], mengikuti pola
 * [com.agribicara.app.data.speech.SpeechErrorMapper]: kebijakannya bisa diuji di
 * JVM, sementara pembungkus yang menyentuh SDK Firebase dijaga sedangkal
 * mungkin di [CrashReportingTree].
 */
object CrashLogPolicy {

    fun shouldReport(priority: Int): Boolean =
        priority == Log.WARN || priority == Log.ERROR || priority == Log.ASSERT

    /**
     * Apakah sebuah log harus disertai objek [Throwable]-nya sebagai laporan
     * non-fatal, bukan sekadar baris teks.
     *
     * Hanya ERROR dan ASSERT. WARN tetap dicatat sebagai jejak, tetapi
     * mengangkat setiap WARN menjadi non-fatal akan menenggelamkan Crashlytics
     * dengan kebisingan sampai crash sungguhan tidak terlihat lagi.
     */
    fun shouldRecordAsNonFatal(priority: Int): Boolean =
        priority == Log.ERROR || priority == Log.ASSERT
}
