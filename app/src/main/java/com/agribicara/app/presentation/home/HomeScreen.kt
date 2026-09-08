package com.agribicara.app.presentation.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agribicara.app.R
import com.agribicara.app.core.util.WeatherCodeMapper
import com.agribicara.app.domain.model.DailyForecast
import com.agribicara.app.presentation.theme.Dimens

/**
 * Kerangka layar Home.
 *
 * Tombol mikrofon adalah elemen paling menonjol di layar (prinsip "satu aksi
 * utama per layar" dari PRD) - sengaja BUKAN FAB kecil di pojok.
 *
 * Sejak Fase 3 tombol ini membuka layar suara sungguhan. Izin mikrofon TIDAK
 * diminta di sini melainkan di layar suara, tepat saat dibutuhkan - Home harus
 * tetap bisa dipakai oleh petani yang belum mau memberi izin.
 * Kartu cuaca sudah terisi data nyata sejak Fase 2.
 *
 * Sejak Fase 6 layar ini hanya mengumpulkan state dan meneruskannya ke
 * [HomeContent]. Pemisahan itu bukan kosmetik: `hiltViewModel()` menuntut graf
 * Hilt yang hidup, sehingga selama isi layar menempel padanya, satu-satunya
 * cara mengujinya adalah membangun infrastruktur test Hilt. [HomeContent] bisa
 * diuji dengan `createComposeRule()` biasa.
 */
@Composable
fun HomeScreen(
    onOpenWeather: () -> Unit,
    onChooseRegion: () -> Unit,
    onOpenVoice: () -> Unit,
    onOpenDetection: () -> Unit,
    onOpenLicense: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Sapaan bisa basi bila app lama di background; hitung ulang tiap resume.
    // Cuaca ikut dimuat ulang di sini supaya kembali dari region picker
    // langsung menampilkan wilayah yang baru dipilih.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshGreeting()
        viewModel.loadWeather()
        onPauseOrDispose { }
    }

    NotificationPermissionRequest(
        hasRegion = !uiState.needsRegion,
        sudahDiminta = uiState.notificationPromptAsked,
        onDiminta = viewModel::onNotificationPromptShown,
    )

    HomeContent(
        uiState = uiState,
        onOpenWeather = onOpenWeather,
        onChooseRegion = onChooseRegion,
        onOpenVoice = onOpenVoice,
        onOpenDetection = onOpenDetection,
        onOpenLicense = onOpenLicense,
        modifier = modifier,
    )
}

/**
 * Meminta izin notifikasi, sekali, dan hanya setelah wilayah dipilih.
 *
 * Bukan saat app pertama dibuka: pada saat itu aplikasi belum tahu wilayah
 * mana pun sehingga belum ada yang bisa diperingatkan, dan permintaan izin
 * yang datang tanpa konteks lebih mungkin ditolak. Setelah petani memilih
 * desanya, peringatan cuaca punya arti yang jelas.
 *
 * Di bawah API 33 izin ini diberikan otomatis, jadi tidak ada yang diminta.
 * Penolakan tidak diikuti apa pun — tidak ada dialog penjelasan, tidak ada
 * permintaan ulang. Notifikasi adalah tambahan, dan memaksa tambahan hanya
 * membuat petani belajar menutup dialog tanpa membaca.
 *
 * [sudahDiminta] datang dari penyimpanan lewat ViewModel, BUKAN dari
 * `rememberSaveable` seperti sebelumnya (temuan F6 L5). `rememberSaveable`
 * bertahan terhadap rotasi dan process death, tetapi **tidak** terhadap entri
 * navigasi yang dibuang — dan pada saat itu petani yang sudah menolak ditanya
 * lagi. `null` berarti nilainya belum sempat dibaca, dan pada keadaan itu
 * jangan meminta apa pun.
 */
@Composable
private fun NotificationPermissionRequest(
    hasRegion: Boolean,
    sudahDiminta: Boolean?,
    onDiminta: () -> Unit,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* Ditolak pun tidak ada yang berubah selain notifikasi. */ }

    LaunchedEffect(hasRegion, sudahDiminta) {
        // sudahDiminta == null berarti penyimpanan belum menjawab. Menganggapnya
        // "belum pernah" akan memunculkan dialog sekejap sebelum jawabannya
        // tiba — persis kambuhnya temuan yang sedang ditutup ini.
        if (!hasRegion || sudahDiminta != false) return@LaunchedEffect

        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        onDiminta()
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/** Isi layar Home tanpa ketergantungan ke ViewModel. Diuji langsung. */
@Composable
internal fun HomeContent(
    uiState: HomeUiState,
    onOpenWeather: () -> Unit,
    onChooseRegion: () -> Unit,
    onOpenVoice: () -> Unit,
    // Default no-op agar HomeContentTest yang sudah ada tetap kompilasi tanpa
    // perubahan; layar sungguhan selalu mengirim aksi dari navigasi.
    onOpenDetection: () -> Unit = {},
    onOpenLicense: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("home_screen"),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Dimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(uiState.greetingRes),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    // Sapaan adalah judul layar ini. Ditandai heading supaya
                    // TalkBack bisa melompat antar bagian, bukan memaksa
                    // petani menyapu seluruh layar satu per satu.
                    .semantics { heading() },
            )

            Spacer(modifier = Modifier.height(Dimens.SpaceLarge))

            WeatherCard(
                uiState = uiState,
                onOpenWeather = onOpenWeather,
                onChooseRegion = onChooseRegion,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FilledIconButton(
                    onClick = onOpenVoice,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .size(Dimens.MicButtonSize)
                        .testTag("mic_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = stringResource(R.string.cd_mic_button),
                        modifier = Modifier.size(Dimens.MicIconSize),
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.SpaceMedium))

                Text(
                    text = stringResource(R.string.home_mic_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(Dimens.SpaceLarge))

                // Aksi sekunder: mikrofon tetap aksi utama layar ini. Deteksi
                // penyakit dibuka sebagai tombol jelas, bukan FAB tersembunyi.
                OutlinedButton(
                    onClick = onOpenDetection,
                    modifier = Modifier
                        .heightIn(min = Dimens.TouchTargetMin)
                        .testTag("detection_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.size(Dimens.SpaceSmall))
                    Text(text = stringResource(R.string.detection_home_button))
                }
            }

            // Atribusi pihak ketiga WAJIB bisa dicapai dari dalam aplikasi
            // (lisensi Apache 2.0 dataset Paddy Doctor). Ditaruh paling bawah
            // dan sebagai TextButton, bukan tombol setara: ini kewajiban
            // hukum, bukan sesuatu yang perlu bersaing dengan mikrofon.
            //
            // heightIn WAJIB — TextButton Material3 lebih pendek dari 48dp,
            // dan TouchTargetInvariantTest memindai SEMUA node yang bisa
            // diklik, termasuk yang ditambahkan belakangan seperti ini.
            TextButton(
                onClick = onOpenLicense,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .heightIn(min = Dimens.TouchTargetMin)
                    .testTag("license_button"),
            ) {
                Text(text = stringResource(R.string.license_home_button))
            }
        }
    }
}

/**
 * Kartu cuaca hari ini.
 *
 * testTag "weather_placeholder" DIPERTAHANKAN dari Fase 1 meskipun isinya
 * bukan lagi placeholder — instrumented test Fase 1 memakainya, dan mengganti
 * tag hanya demi penamaan akan mematahkan test tanpa manfaat.
 */
@Composable
private fun WeatherCard(
    uiState: HomeUiState,
    onOpenWeather: () -> Unit,
    onChooseRegion: () -> Unit,
) {
    val clickAction = if (uiState.needsRegion) onChooseRegion else onOpenWeather
    val summary = uiState.today?.let { weatherCardDescription(it, uiState) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.TouchTargetMin)
            .clickable(onClick = clickAction)
            .testTag("weather_placeholder")
            // Digabung jadi SATU node. Tanpa ini TalkBack membacakan ikon,
            // suhu, deskripsi, dan nama wilayah sebagai empat perhentian
            // terpisah — informasinya benar tetapi susunannya tidak bisa
            // dipahami sebagai satu kalimat.
            .semantics(mergeDescendants = true) {
                if (summary != null) contentDescription = summary
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.SpaceLarge),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.isWeatherLoading && uiState.today == null -> CircularProgressIndicator()

                uiState.needsRegion -> CardMessage(stringResource(R.string.weather_no_region))

                uiState.today == null -> CardMessage(
                    stringResource(R.string.error_weather_unavailable),
                )

                else -> TodaySummary(day = uiState.today, uiState = uiState)
            }
        }
    }
}

/**
 * Satu kalimat utuh untuk TalkBack.
 *
 * Disusun dari bagian yang memang terlihat di layar, bukan teks terpisah:
 * pengguna TalkBack dan pengguna awas harus mendapat informasi yang sama.
 */
@Composable
private fun weatherCardDescription(day: DailyForecast, uiState: HomeUiState): String {
    val parts = listOfNotNull(
        stringResource(R.string.cd_weather_today),
        uiState.regionName,
        day.temperatureMax?.let { stringResource(R.string.cd_weather_temperature, it.toInt()) },
        day.description ?: stringResource(WeatherCodeMapper.labelFor(day.weatherCode)),
        stringResource(R.string.weather_offline_banner).takeIf { uiState.isOffline },
    )
    return parts.joinToString(", ")
}

@Composable
private fun TodaySummary(day: DailyForecast, uiState: HomeUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = WeatherCodeMapper.iconFor(day.weatherCode),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Dimens.MicIconSize),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Dimens.SpaceMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceExtraSmall),
        ) {
            Text(
                text = day.temperatureMax?.let { "${it.toInt()}°C" } ?: "—",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = day.description
                    ?: stringResource(WeatherCodeMapper.labelFor(day.weatherCode)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            uiState.regionName?.let { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (uiState.isOffline) {
                Text(
                    text = stringResource(R.string.weather_offline_banner),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CardMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
