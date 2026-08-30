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
) {

    /**
     * Menampilkan [alert], atau diam bila izinnya belum ada.
     *
     * Tidak pernah melempar. Pemanggilnya adalah Worker latar belakang, dan
     * gagal memberi tahu bukan alasan untuk menandai pekerjaannya gagal.
     */
    fun notify(alert: WeatherAlert) {
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
            return
        }

        ensureChannel()

        val notification = NotificationCompat.Builder(context, Constants.WEATHER_ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
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
        runCatching {
            NotificationManagerCompat.from(context)
                .notify(Constants.WEATHER_ALERT_NOTIFICATION_ID, notification)
        }.onFailure { Timber.w(it, "Notifikasi cuaca tidak bisa ditampilkan") }
    }

    /**
     * Channel dibuat ulang setiap kali, bukan sekali saat startup.
     *
     * `createNotificationChannel` bersifat idempoten, dan memanggilnya di sini
     * berarti channel-nya pasti ada tepat ketika dibutuhkan — tanpa
     * bergantung pada urutan inisialisasi Application yang bisa berubah.
     */
    private fun ensureChannel() {
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
