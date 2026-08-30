package com.agribicara.app.presentation.weather.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.agribicara.app.R
import com.agribicara.app.core.util.WeatherCodeMapper
import com.agribicara.app.domain.model.DailyForecast
import com.agribicara.app.domain.model.WeatherSource
import com.agribicara.app.presentation.theme.Dimens
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Locale Indonesia agar nama hari tampil "Sab", bukan "Sat". */
private val INDONESIA = Locale.forLanguageTag("id-ID")
private val DAY_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, d MMM", INDONESIA)

/**
 * Banner mode offline.
 *
 * Muncul HANYA saat data benar-benar dibaca dari cache. Menampilkan data lama
 * tanpa memberi tahu asalnya akan membuat petani mengambil keputusan
 * berdasarkan cuaca kemarin tanpa sadar.
 */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(Dimens.CardCornerRadius),
            )
            .padding(Dimens.SpaceMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
        Text(
            text = stringResource(R.string.weather_offline_banner),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(start = Dimens.SpaceSmall),
        )
    }
}

/**
 * Satu baris prakiraan harian.
 *
 * Hari yang berasal dari Open-Meteo diberi badge "Data estimasi" — pembedaan
 * ini penting karena BMKG hiperlokal sampai level kelurahan sedangkan
 * Open-Meteo hanya berbasis grid.
 */
@Composable
fun DailyForecastRow(day: DailyForecast, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpaceMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = WeatherCodeMapper.iconFor(day.weatherCode),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Dimens.SpaceExtraLarge),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Dimens.SpaceMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceExtraSmall),
        ) {
            Text(
                text = day.date.format(DAY_FORMATTER),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                // BMKG mengirim deskripsi Bahasa Indonesia resmi; hanya
                // Open-Meteo yang perlu diterjemahkan dari kode WMO.
                text = day.description ?: stringResource(WeatherCodeMapper.labelFor(day.weatherCode)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            day.precipitationMm?.let { rain ->
                Text(
                    text = stringResource(R.string.weather_rain_amount, formatOneDecimal(rain)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (day.source == WeatherSource.OPEN_METEO) {
                Text(
                    text = stringResource(R.string.weather_estimated_badge),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        Text(
            text = formatRange(day),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** "31° / 18°", atau "—" bila suhu tidak tersedia sama sekali. */
private fun formatRange(day: DailyForecast): String {
    val max = day.temperatureMax?.let { "${it.toInt()}°" }
    val min = day.temperatureMin?.let { "${it.toInt()}°" }
    return when {
        max != null && min != null -> "$max / $min"
        max != null -> max
        min != null -> min
        else -> "—"
    }
}

/**
 * String.format dihindari di sini: dengan Locale default perangkat, angka
 * desimal bisa memakai koma atau titik tergantung setelan, dan itu tidak
 * konsisten dengan angka lain di layar.
 */
private fun formatOneDecimal(value: Double): String {
    val rounded = kotlin.math.round(value * 10).toInt()
    return "${rounded / 10},${rounded % 10}"
}
