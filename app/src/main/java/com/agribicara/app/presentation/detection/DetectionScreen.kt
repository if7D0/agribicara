package com.agribicara.app.presentation.detection

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agribicara.app.R
import com.agribicara.app.domain.model.DetectionOutcome
import com.agribicara.app.presentation.theme.Dimens
import java.io.File

/**
 * Layar deteksi penyakit padi (Fase 4).
 *
 * Gambar diambil lewat kamera sistem ([ActivityResultContracts.TakePicture],
 * hasil full-res ke Uri FileProvider) atau galeri (Photo Picker) — keduanya
 * kontrak ActivityResult standar, TANPA izin CAMERA. Panduan pembingkaian
 * ("satu daun, latar polos") ditampilkan sebagai teks: itu mitigasi OOD yang
 * membuat input menyerupai kondisi latih model.
 *
 * Mengumpulkan state lalu mendelegasikan tampilan hasil ke [DetectionResult],
 * mengikuti pemisahan Screen/Content di [com.agribicara.app.presentation.home.HomeScreen]
 * agar bagian visual bisa diuji tanpa graf Hilt.
 */
@Composable
fun DetectionScreen(
    modifier: Modifier = Modifier,
    viewModel: DetectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Uri tujuan kamera diingat sampai hasilnya kembali (TakePicture hanya
    // mengembalikan Boolean sukses, bukan Uri-nya). Tidak disimpan lintas
    // process-death demi kesederhanaan — kasus itu berujung foto ulang, bukan
    // crash.
    val pendingCameraUri = remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri.value
        if (success && uri != null) viewModel.analyze(uri)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.analyze(uri)
    }

    Scaffold(modifier = modifier.testTag("detection_screen")) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Dimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.detection_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() },
            )

            Spacer(modifier = Modifier.height(Dimens.SpaceLarge))

            when {
                uiState.isAnalyzing -> AnalyzingState()

                uiState.outcome != null -> DetectionResult(
                    outcome = uiState.outcome!!,
                    onRetake = viewModel::reset,
                )

                else -> CaptureState(
                    errorMessage = uiState.errorMessage,
                    onTakePhoto = {
                        val uri = createCaptureUri(context)
                        pendingCameraUri.value = uri
                        cameraLauncher.launch(uri)
                    },
                    onPickGallery = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun AnalyzingState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMedium),
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.detection_analyzing),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun CaptureState(
    errorMessage: String?,
    onTakePhoto: () -> Unit,
    onPickGallery: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMedium),
    ) {
        Text(
            text = stringResource(R.string.detection_guidance),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Button(
            onClick = onTakePhoto,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.TouchTargetMin)
                .testTag("take_photo_button"),
        ) {
            Icon(imageVector = Icons.Filled.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.size(Dimens.SpaceSmall))
            Text(text = stringResource(R.string.detection_take_photo))
        }

        OutlinedButton(
            onClick = onPickGallery,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.TouchTargetMin)
                .testTag("pick_gallery_button"),
        ) {
            Icon(imageVector = Icons.Filled.PhotoLibrary, contentDescription = null)
            Spacer(modifier = Modifier.size(Dimens.SpaceSmall))
            Text(text = stringResource(R.string.detection_pick_gallery))
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Kartu hasil. Bentuk berbeda untuk tiap [DetectionOutcome], dan
 * [DetectionOutcome.Unsure] SENGAJA tidak menampilkan label apa pun — itu
 * pengaman inti fase ini.
 *
 * Dua hasil positif dibedakan lagi oleh pita keyakinannya: pita lemah
 * mendapat judul sendiri plus [WeakBandWarning], karena hasil di pita itu
 * hanya benar sekitar dua dari tiga kali. Pemetaan hasil → teks ada di
 * [DetectionOutcomeStrings] agar bisa diuji tanpa perangkat.
 *
 * Tanpa ketergantungan ViewModel agar bisa diuji langsung dengan `createComposeRule`.
 */
@Composable
internal fun DetectionResult(
    outcome: DetectionOutcome,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("detection_result"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMedium),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.SpaceLarge),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSmall),
            ) {
                when (outcome) {
                    is DetectionOutcome.Diagnosed -> {
                        SectionTitle(stringResource(DetectionOutcomeStrings.titleFor(outcome)))
                        Text(
                            text = outcome.info.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(
                                R.string.detection_confidence,
                                (outcome.confidence * 100).toInt(),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = outcome.info.summary,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (DetectionOutcomeStrings.showsWeakWarning(outcome)) {
                            WeakBandWarning()
                        }
                        Text(
                            text = stringResource(R.string.detection_disclaimer),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    is DetectionOutcome.Healthy -> {
                        SectionTitle(stringResource(DetectionOutcomeStrings.titleFor(outcome)))
                        Text(
                            text = stringResource(R.string.detection_healthy_body),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (DetectionOutcomeStrings.showsWeakWarning(outcome)) {
                            WeakBandWarning()
                        }
                    }

                    is DetectionOutcome.Unsure -> {
                        SectionTitle(stringResource(DetectionOutcomeStrings.titleFor(outcome)))
                        Text(
                            text = stringResource(R.string.detection_unsure_body),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Button(
            onClick = onRetake,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.TouchTargetMin)
                .testTag("retake_button"),
        ) {
            Text(text = stringResource(R.string.detection_retake))
        }
    }
}

/**
 * Peringatan untuk hasil berpita lemah.
 *
 * Memakai warna `error` alih-alih `onSurfaceVariant` supaya terbaca sebagai
 * peringatan, bukan keterangan tambahan; ukurannya tetap `bodyLarge` karena
 * teks peringatan adalah bagian terpenting kartu ini, bukan catatan kaki.
 *
 * Kontras di atas `surfaceVariant` kartu, diukur seperti di `Color.kt`:
 *   ErrorRed di atas SurfaceVariantLight ....... 5.68:1
 *   ErrorRedLight di atas SurfaceVariantDark ... 5.53:1
 * Keduanya lolos WCAG AA untuk teks normal (4.5:1).
 */
@Composable
private fun WeakBandWarning() {
    Text(
        text = stringResource(R.string.detection_weak_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag("weak_band_warning"),
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

/**
 * Membuat Uri FileProvider untuk foto kamera baru di cacheDir/detection/.
 * Authorities harus cocok dengan yang dideklarasikan di AndroidManifest.
 */
private fun createCaptureUri(context: Context): Uri {
    val dir = File(context.cacheDir, "detection").apply { mkdirs() }

    // Tangkapan sebelumnya dibuang lebih dulu. Tanpa ini setiap penekanan
    // "Ambil foto" meninggalkan JPEG resolusi penuh yang TIDAK PERNAH dihapus
    // siapa pun — Android hanya membersihkan cache saat penyimpanan sudah
    // tertekan, dan itu terlambat bagi perangkat murah yang jadi sasaran
    // aplikasi ini. Penyimpanan penuh juga membuat penulisan foto terputus,
    // yang berujung pada foto gagal di-decode.
    //
    // Aman dilakukan di sini: layar hasil tidak menampilkan fotonya, jadi tidak
    // ada berkas lama yang masih dibutuhkan. Membersihkan SEBELUM membuat yang
    // baru, bukan sesudah klasifikasi, menjaga berkas yang sedang dibaca kamera
    // atau classifier tidak ikut terhapus.
    dir.listFiles()?.forEach { it.delete() }

    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
