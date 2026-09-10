package com.agribicara.app.data.notification

import com.agribicara.app.domain.model.WeatherAlert

/**
 * Kemampuan memberi tahu petani soal cuaca ekstrem.
 *
 * Diangkat dari [WeatherNotifier] supaya
 * [com.agribicara.app.data.worker.WeatherCheckWorker] bergantung pada
 * kemampuan, bukan pada kelas yang menyentuh NotificationManager. Tanpa ini
 * test worker terpaksa memakai MockK, dan agen Android MockK menyuntikkan JAR
 * ke boot classpath lalu meng-instrumentasi java.lang.Object — yang di
 * Android 15 dengan targetSdk 37 membuat suite instrumented tertahan begitu
 * cukup banyak kelas termuat di proses. Lihat AgriBicaraTestRunner.
 *
 * Pola yang sama sudah dipakai WeatherRepository dan RegionRepository.
 */
interface WeatherAlertNotifier {

    /**
     * Menampilkan [alert], atau diam bila izinnya belum ada.
     *
     * Mengembalikan true HANYA bila notifikasinya benar-benar diserahkan ke
     * sistem. Nilai ini bukan hiasan: [AlertHistory] memakainya untuk
     * memutuskan apakah peringatan boleh dicatat sebagai sudah tersampaikan.
     *
     * Tidak boleh melempar. Pemanggilnya Worker latar belakang, dan gagal
     * memberi tahu bukan alasan menandai pekerjaannya gagal.
     */
    fun notify(alert: WeatherAlert): Boolean
}
