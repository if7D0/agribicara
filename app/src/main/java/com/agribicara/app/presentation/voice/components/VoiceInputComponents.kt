// Bagian layar suara yang MENERIMA masukan: tombol bicara, kolom teks, dan
// jalan keluar ketika TTS tidak tersedia atau izin ditolak permanen.
//
// Dipisah dari VoiceScreen.kt di Fase 9 (temuan F5 L5). Ini refactor TANPA
// perubahan perilaku.
//
// Alasan nama berkas WAJIB berakhiran Components.kt sama dengan
// TranscriptComponents.kt - lihat catatan berkas di sana.

package com.agribicara.app.presentation.voice.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import com.agribicara.app.R
import com.agribicara.app.domain.model.TtsStatus
import com.agribicara.app.presentation.theme.Dimens
import com.agribicara.app.presentation.voice.VoiceUiState

/**
 * Kendali suara: tombol mikrofon, "dengar lagi", dan pindah ke mode teks.
 *
 * Tombol "dengar lagi" hanya muncul bila `canReplay` — lihat komentar di
 * dalamnya untuk sebabnya.
 */
@Composable
internal fun VoiceControls(
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

        // canReplay sudah mensyaratkan ADA jawaban: sebelumnya tombol muncul
        // di fase RESULT walau jawabannya gagal didapat, lalu tidak melakukan
        // apa-apa saat ditekan.
        if (uiState.canReplay) {
            TextButton(
                onClick = onReplay,
                modifier = Modifier
                    .heightIn(min = Dimens.TouchTargetMin)
                    .testTag("voice_replay"),
            ) {
                Text(stringResource(R.string.voice_replay))
            }
        }

        TextButton(
            onClick = onSwitchToText,
            modifier = Modifier
                .heightIn(min = Dimens.TouchTargetMin)
                .testTag("voice_switch_text"),
        ) {
            Text(stringResource(R.string.voice_switch_to_text))
        }
    }
}

@Composable
internal fun TextQuestionInput(
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
            TextButton(
                onClick = onSwitchToVoice,
                modifier = Modifier
                    .heightIn(min = Dimens.TouchTargetMin)
                    .testTag("voice_switch_voice"),
            ) {
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
internal fun TtsNotice(status: TtsStatus?) {
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
internal fun OpenSettingsAction(context: Context) {
    Spacer(modifier = Modifier.height(Dimens.SpaceMedium))
    TextButton(
        onClick = {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            runCatching { context.startActivity(intent) }
        },
        modifier = Modifier
            .heightIn(min = Dimens.TouchTargetMin)
            .testTag("voice_open_settings"),
    ) {
        Text(stringResource(R.string.voice_permission_open_settings))
    }
}

