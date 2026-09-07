package com.agribicara.app.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.agribicara.app.MainActivity
import com.agribicara.app.R
import com.agribicara.app.core.common.Constants
import com.agribicara.app.domain.model.AlertReason
import com.agribicara.app.domain.model.WeatherAlert
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Memunculkan notifikasi peringatan cuaca (Fase 6).
 *
 * Kalimatnya disusun di sini, bukan di `domain/`: penyusunannya butuh resource
 * string Android, sedangkan paket domain tidak boleh mengenal Android.
 */
@Singleton
class WeatherNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
    private val alertChannel: WeatherAlertChannel,
) {

    /**
     * Menampilkan [alert], atau diam bila izinnya belum ada.
     *
     * Mengembalikan true HANYA bila notifikasinya benar-benar diserahkan ke
     * sistem. Nilai ini bukan hiasan: `AlertHistory` memakainya untuk
     * memutuskan apakah peringatan boleh dicatat sebagai sudah tersampaikan.
     * Mencatat peringatan yang ternyata tidak pernah tampil — karena izinnya
     * ditolak, misalnya — akan membungkam kejadian itu selamanya, bahkan
     * setelah petani memberikan izinnya.
     *
     * Tidak pernah melempar. Pemanggilnya adalah Worker latar belakang, dan
     * gagal memberi tahu bukan alasan untuk menandai pekerjaannya gagal.
     */
    fun notify(alert: WeatherAlert): Boolean {
        // Diperiksa, bukan ditangkap. Sebelumnya SecurityException-nya ditelan
        // runCatching; lint benar menolak itu, dan memang memeriksa lebih baik:
        // menolak izin adalah keadaan normal yang layak dicatat sebagai
        // "dilewati", bukan sebagai kegagalan yang tertangkap.
        val bolehMemberitahu = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

        if (!bolehMemberitahu) {
            Timber.d("Notifikasi cuaca dilewati: izin POST_NOTIFICATIONS belum diberikan")
            return false
        }

        ensureChannel()

        val notification = NotificationCompat.Builder(context, Constants.WEATHER_ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_weather)
            .setContentTitle(titleFor(alert))
            .setContentText(context.getString(R.string.weather_alert_body, alert.regionName))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    context.getString(R.string.weather_alert_body, alert.regionName),
                ),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()

        // Izinnya sudah diperiksa di atas, tetapi masih ada kegagalan lain yang
        // di luar kendali kita: channel dimatikan pengguna, atau batas
        // notifikasi sistem tercapai. Ditelan dengan sengaja — pemanggilnya
        // Worker latar belakang, dan gagal memberi tahu bukan alasan untuk
        // menandai pekerjaannya gagal lalu mengulanginya terus-menerus.
        // Kegagalannya tetap DILAPORKAN lewat nilai kembalian, bukan hilang.
        return runCatching {
            NotificationManagerCompat.from(context)
                .notify(Constants.WEATHER_ALERT_NOTIFICATION_ID, notification)
        }.onFailure { Timber.w(it, "Notifikasi cuaca tidak bisa ditampilkan") }
            .isSuccess
    }

    /**
     * Channel dipastikan ada setiap kali, bukan hanya sekali saat startup.
     *
     * `createNotificationChannel` bersifat idempoten, dan memanggilnya di sini
     * berarti channel-nya pasti ada tepat ketika dibutuhkan — tanpa
     * bergantung pada urutan inisialisasi Application yang bisa berubah.
     *
     * Sejak Fase 9 isinya tinggal di [WeatherAlertChannel] supaya
     * `AgriBicaraApp` bisa memanggilnya juga saat startup, sehingga petani bisa
     * mengatur channel-nya SEBELUM peringatan pertama muncul. Pemanggilan di
     * sini TETAP ADA dan jangan dihapus — itu yang menjaga jaminan di atas.
     */
    private fun ensureChannel() = alertChannel.ensure()

    private fun titleFor(alert: WeatherAlert): String {
        val titleRes = when (alert.reason) {
            AlertReason.THUNDERSTORM -> R.string.weather_alert_title_thunderstorm
            AlertReason.HEAVY_RAIN -> R.string.weather_alert_title_heavy_rain
        }
        return context.getString(titleRes, dayLabel(alert.date))
    }

    /**
     * "hari ini" / "besok", bukan tanggal angka.
     *
     * Aturan yang sama dengan `domain/ai/PromptBuilder`: tanggal dalam angka
     * menuntut pembacanya menghitung sendiri, dan itu beban yang tidak perlu
     * pada notifikasi yang dibaca sekilas.
     */
    private fun dayLabel(date: LocalDate): String {
        val today = LocalDate.now(clock)
        return when (date) {
            today -> context.getString(R.string.weather_alert_today)
            today.plusDays(1) -> context.getString(R.string.weather_alert_tomorrow)
            else -> date.toString()
        }
    }

    /**
     * FLAG_IMMUTABLE WAJIB: sejak API 31 membuat PendingIntent tanpa flag
     * mutability melempar IllegalArgumentException saat runtime.
     */
    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
