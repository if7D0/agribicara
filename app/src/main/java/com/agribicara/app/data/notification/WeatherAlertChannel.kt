package com.agribicara.app.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.agribicara.app.R
import com.agribicara.app.core.common.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Channel notifikasi peringatan cuaca ekstrem.
 *
 * Diangkat keluar dari [WeatherNotifier] (Fase 9, temuan F6 L2) supaya bisa
 * dipanggil saat startup, BUKAN hanya saat notifikasi pertama muncul.
 *
 * Alasannya soal kendali pengguna, bukan kerapian: sebelum ini channel baru
 * lahir ketika peringatan pertama tampil, sehingga petani tidak punya cara
 * mengatur atau membisukannya lebih dulu — entri Setelan → Notifikasi-nya belum
 * ada. Ia baru bisa mengaturnya SETELAH terlanjur dikejutkan sekali. Untuk
 * peringatan yang sengaja dibuat IMPORTANCE_HIGH, itu urutan yang keliru.
 *
 * [ensure] tetap dipanggil juga dari [WeatherNotifier] sebelum menampilkan
 * notifikasi. Itu DISENGAJA dan jangan dihapus: `createNotificationChannel`
 * idempoten, dan memanggilnya di sana berarti channel-nya pasti ada tepat saat
 * dibutuhkan tanpa bergantung pada urutan inisialisasi `Application` yang bisa
 * berubah. Task ini MENAMBAH titik panggil, bukan memindahkannya.
 */
@Singleton
class WeatherAlertChannel @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Membuat channel bila perlu. Aman dipanggil berkali-kali.
     *
     * [Constants.WEATHER_ALERT_CHANNEL_ID] TIDAK BOLEH berubah: channel yang
     * sudah ada di perangkat tidak bisa diubah propertinya, dan id baru berarti
     * seluruh setelan yang sudah dipilih petani ditinggalkan begitu saja.
     */
    fun ensure() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            Constants.WEATHER_ALERT_CHANNEL_ID,
            context.getString(R.string.weather_alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.weather_alert_channel_description)
        }

        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }
}
