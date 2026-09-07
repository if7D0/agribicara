package com.agribicara.app.presentation.license

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agribicara.app.BuildConfig
import com.agribicara.app.R
import com.agribicara.app.presentation.theme.Dimens

/**
 * Layar "Tentang & Lisensi".
 *
 * Ada karena KEWAJIBAN, bukan karena fitur: dataset Paddy Doctor berlisensi
 * Apache 2.0 dan mewajibkan atribusinya disertakan pada distribusi aplikasi
 * yang memuat model turunannya. `ml/NOTICE` menyebut layar seperti inilah
 * tempatnya. Tanpa layar ini, aplikasi tidak boleh dirilis ke publik.
 *
 * Teksnya dibaca dari aset, tidak ditulis ulang di `strings.xml`, dan
 * `LicenseNoticeInvariantTest` menjaga aset itu tetap identik dengan
 * `ml/NOTICE`. Karena isinya adalah kutipan hukum berbahasa Inggris, ia
 * sengaja TIDAK diterjemahkan — yang wajib disertakan adalah atribusi apa
 * adanya. Kalimat pengantar di sekitarnya yang berbahasa Indonesia.
 *
 * Mengikuti pemisahan yang sama dengan [com.agribicara.app.presentation.home.HomeContent]:
 * [LicenseContent] tidak menyentuh Hilt sehingga bisa diuji dengan
 * `createComposeRule()` biasa.
 */
@Composable
fun LicenseScreen(
    modifier: Modifier = Modifier,
    viewModel: LicenseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LicenseContent(uiState = uiState, modifier = modifier)
}

/** Isi layar Lisensi tanpa ketergantungan ke ViewModel. Diuji langsung. */
@Composable
internal fun LicenseContent(
    uiState: LicenseUiState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("license_screen"),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Dimens.ScreenPadding)
                // Isi atribusi bisa lebih panjang dari layar, terutama pada
                // perangkat kecil dengan ukuran font sistem diperbesar —
                // keduanya lazim di antara pengguna sasaran aplikasi ini.
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.license_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() },
            )

            Spacer(modifier = Modifier.height(Dimens.SpaceSmall))

            Text(
                text = stringResource(R.string.license_app_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(Dimens.SpaceLarge))

            Text(
                text = stringResource(R.string.license_data_sources_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )

            Spacer(modifier = Modifier.height(Dimens.SpaceSmall))

            Text(
                text = stringResource(R.string.license_data_sources_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(Dimens.SpaceLarge))

            Text(
                text = stringResource(R.string.license_model_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )

            Spacer(modifier = Modifier.height(Dimens.SpaceSmall))

            Text(
                text = stringResource(R.string.license_model_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(Dimens.SpaceMedium))

            when {
                uiState.hasFailed -> Text(
                    text = stringResource(R.string.license_notice_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("license_notice_error"),
                )

                uiState.isLoading -> CircularProgressIndicator()

                else -> Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = uiState.noticeText,
                        // Monospace menandai bahwa ini kutipan mentah, bukan
                        // kalimat yang ditulis untuk layar ini. Perlu dicatat
                        // apa adanya: `ml/NOTICE` sudah dibungkus keras pada
                        // ~76 kolom, jadi di layar ponsel barisnya terbungkus
                        // dua kali dan URL panjang tetap terpotong. Itu harga
                        // yang dibayar demi menampilkan atribusi VERBATIM, dan
                        // memformat ulangnya berarti menyunting teks lisensi.
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(Dimens.SpaceMedium)
                            .testTag("license_notice_text"),
                    )
                }
            }
        }
    }
}
