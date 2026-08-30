package com.agribicara.app.presentation.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agribicara.app.R
import com.agribicara.app.domain.model.TtsStatus
import com.agribicara.app.presentation.theme.Dimens
import com.agribicara.app.presentation.voice.components.MicIndicator

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
            TranscriptArea(uiState = uiState)

            Spacer(modifier = Modifier.height(Dimens.SpaceLarge))

            if (uiState.isTextMode) {
                TextQuestionInput(
                    onSubmit = viewModel::submitTypedText,
                    canSwitchToVoice = !uiState.isSpeechUnavailable,
                    onSwitchToVoice = viewModel::disableTextMode,
                )
            } else {
                VoiceControls(
                    uiState = uiState,
                    onMicClick = {
                        if (uiState.isListening) {
                            viewModel.stopListening()
                        } else if (context.hasRecordAudioPermission()) {
                            viewModel.startListening()
                        } else {
                            // Izin diminta saat mic ditekan, bukan saat app
                            // dibuka: onboarding sudah menjelaskan alasannya,
                            // jadi permintaannya datang tepat saat dibutuhkan.
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onReplay = viewModel::replay,
                    onSwitchToText = viewModel::enableTextMode,
                )
            }

            TtsNotice(status = uiState.ttsStatus)

            if (!uiState.isRetryable && uiState.isTextMode) {
                OpenSettingsAction(context = context)
            }
        }
    }
}

@Composable
private fun TranscriptArea(uiState: VoiceUiState) {
    val promptRes = when (uiState.phase) {
        VoicePhase.PREPARING -> R.string.voice_prompt_preparing
        VoicePhase.LISTENING -> R.string.voice_prompt_listening
        VoicePhase.PROCESSING -> R.string.voice_prompt_processing
        else -> R.string.voice_prompt_idle
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TRANSCRIPT_MIN_HEIGHT),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (uiState.transcript.isBlank()) {
            Text(
                text = stringResource(promptRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = uiState.transcript,
                // Teks hasil sengaja besar: ini satu-satunya informasi yang
                // dicari petani di layar ini.
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("voice_transcript"),
            )
        }
    }
}

@Composable
private fun VoiceControls(
    uiState: VoiceUiState,
    onMicClick: () -> Unit,
    onReplay: () -> Unit,
    onSwitchToText: () -> Unit,
) {
    val listeningDescription = stringResource(R.string.cd_voice_listening)
    val idleDescription = stringResource(R.string.cd_mic_button)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MicIndicator(
            isListening = uiState.isListening,
            soundLevel = uiState.soundLevel,
            contentDescription = if (uiState.isListening) {
                listeningDescription
            } else {
                idleDescription
            },
            onClick = onMicClick,
        )

        Spacer(modifier = Modifier.height(Dimens.SpaceMedium))

        if (uiState.phase == VoicePhase.RESULT && uiState.canSpeak) {
            TextButton(onClick = onReplay, modifier = Modifier.testTag("voice_replay")) {
                Text(stringResource(R.string.voice_replay))
            }
        }

        TextButton(onClick = onSwitchToText, modifier = Modifier.testTag("voice_switch_text")) {
            Text(stringResource(R.string.voice_switch_to_text))
        }
    }
}

@Composable
private fun TextQuestionInput(
    onSubmit: (String) -> Unit,
    canSwitchToVoice: Boolean,
    onSwitchToVoice: () -> Unit,
) {
    // rememberSaveable: ketikan tidak boleh hilang saat layar dirotasi.
    var text by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    fun submit() {
        if (text.isNotBlank()) {
            onSubmit(text)
            text = ""
            keyboard?.hide()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.voice_text_mode_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(Dimens.SpaceSmall))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(stringResource(R.string.voice_text_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { submit() }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("voice_text_field"),
        )

        Spacer(modifier = Modifier.height(Dimens.SpaceMedium))

        Button(
            onClick = { submit() },
            enabled = text.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.ButtonHeight)
                .testTag("voice_text_send"),
        ) {
            Text(stringResource(R.string.voice_text_send))
        }

        if (canSwitchToVoice) {
            TextButton(onClick = onSwitchToVoice, modifier = Modifier.testTag("voice_switch_voice")) {
                Text(stringResource(R.string.voice_switch_to_voice))
            }
        }
    }
}

/**
 * Memberi tahu bahwa jawaban tidak bisa dibacakan.
 *
 * Ditampilkan sebagai catatan, bukan error: teksnya tetap terbaca, jadi ini
 * penurunan kualitas dan tidak boleh terlihat seperti kegagalan.
 */
@Composable
private fun TtsNotice(status: TtsStatus?) {
    val noticeRes = when (status) {
        TtsStatus.LANGUAGE_MISSING_DATA -> R.string.voice_tts_missing_data
        TtsStatus.LANGUAGE_NOT_SUPPORTED, TtsStatus.ENGINE_UNAVAILABLE ->
            R.string.voice_tts_unavailable
        else -> null
    } ?: return

    Spacer(modifier = Modifier.height(Dimens.SpaceLarge))
    Text(
        text = stringResource(noticeRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag("voice_tts_notice"),
    )
}

/**
 * Jalan keluar saat izin ditolak permanen.
 *
 * Setelah "Jangan tanya lagi", dialog izin tidak akan pernah muncul lagi -
 * satu-satunya jalan adalah Pengaturan sistem.
 */
@Composable
private fun OpenSettingsAction(context: Context) {
    Spacer(modifier = Modifier.height(Dimens.SpaceMedium))
    TextButton(
        onClick = {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            runCatching { context.startActivity(intent) }
        },
        modifier = Modifier.testTag("voice_open_settings"),
    ) {
        Text(stringResource(R.string.voice_permission_open_settings))
    }
}

private fun Context.hasRecordAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

private val TRANSCRIPT_MIN_HEIGHT = Dimens.MicButtonSize
