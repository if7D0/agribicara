package com.agribicara.app.presentation.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agribicara.app.R
import com.agribicara.app.presentation.theme.Dimens
import com.agribicara.app.presentation.voice.components.OpenSettingsAction
import com.agribicara.app.presentation.voice.components.TextQuestionInput
import com.agribicara.app.presentation.voice.components.TranscriptArea
import com.agribicara.app.presentation.voice.components.TtsNotice
import com.agribicara.app.presentation.voice.components.VoiceControls

/**
 * Layar suara.
 *
 * Fase 3 membuktikan loop-nya: apa yang didengar dibacakan kembali. Fase 5
 * menyisipkan jawaban Gemini di antara keduanya tanpa mengubah kerangka layar.
 *
 * Aturan yang dipegang layar ini: TIDAK PERNAH ada jalan buntu. Izin ditolak,
 * pengenalan suara absen, atau bahasa tidak terpasang - semuanya berakhir pada
 * kolom teks yang bisa dipakai, bukan pada layar mati.
 */
@Composable
fun VoiceScreen(
    onOpenWeather: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VoiceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val permissionDeniedMessage = stringResource(R.string.voice_permission_denied)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.startListening()
        } else {
            viewModel.onPermissionDenied(permissionDeniedMessage)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onErrorShown()
        }
    }

    // Melepaskan mikrofon saat layar ditinggalkan. onCleared saja tidak cukup:
    // ViewModel bertahan melewati perubahan konfigurasi, sedangkan mikrofon
    // harus dilepas begitu layar tidak terlihat.
    DisposableEffect(Unit) {
        onDispose { viewModel.stopListening() }
    }

    VoiceContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onOpenWeather = onOpenWeather,
        onSubmitText = viewModel::submitTypedText,
        onSwitchToVoice = viewModel::disableTextMode,
        onMicClick = {
            if (uiState.isListening) {
                viewModel.stopListening()
            } else if (context.hasRecordAudioPermission()) {
                viewModel.startListening()
            } else {
                // Izin diminta saat mic ditekan, bukan saat app dibuka:
                // onboarding sudah menjelaskan alasannya, jadi permintaannya
                // datang tepat saat dibutuhkan.
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onReplay = viewModel::replay,
        onSwitchToText = viewModel::enableTextMode,
        modifier = modifier,
    )
}

/**
 * Isi layar suara tanpa ketergantungan ke ViewModel.
 *
 * Dipisah supaya tangga degradasi Fase 5 — pesan kegagalan yang permanen dan
 * tawaran "lihat cuaca saja" — bisa diuji tanpa membangun graf Hilt. Itu jalur
 * yang paling sering ditemui petani bersinyal lemah, jadi justru jalur itu yang
 * paling perlu penjaga regresi.
 */
@Composable
internal fun VoiceContent(
    uiState: VoiceUiState,
    onOpenWeather: () -> Unit,
    onSubmitText: (String) -> Unit,
    onSwitchToVoice: () -> Unit,
    onMicClick: () -> Unit,
    onReplay: () -> Unit,
    onSwitchToText: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.testTag("voice_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Dimens.ScreenPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TranscriptArea(uiState = uiState, onOpenWeather = onOpenWeather)

            Spacer(modifier = Modifier.height(Dimens.SpaceLarge))

            if (uiState.isTextMode) {
                TextQuestionInput(
                    onSubmit = onSubmitText,
                    canSwitchToVoice = !uiState.isSpeechUnavailable,
                    onSwitchToVoice = onSwitchToVoice,
                )
            } else {
                VoiceControls(
                    uiState = uiState,
                    onMicClick = onMicClick,
                    onReplay = onReplay,
                    onSwitchToText = onSwitchToText,
                )
            }

            TtsNotice(status = uiState.ttsStatus)

            if (!uiState.isRetryable && uiState.isTextMode) {
                OpenSettingsAction(context = context)
            }
        }
    }
}

/**
 * Apakah izin mikrofon sudah diberikan.
 *
 * Ekstensi, bukan Composable: dipanggil dari dalam lambda `onMicClick` saat
 * tombol ditekan, jadi ia harus bisa dijalankan di luar komposisi.
 */
private fun Context.hasRecordAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

