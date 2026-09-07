package com.agribicara.app.presentation.voice.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.agribicara.app.R
import com.agribicara.app.presentation.theme.Dimens
import com.agribicara.app.presentation.voice.VoicePhase
import com.agribicara.app.presentation.voice.VoiceUiState

/**
 * Bagian layar suara yang MENAMPILKAN: pertanyaan, jawaban, dan keadaannya.
 *
 * Dipisah dari `VoiceScreen.kt` di Fase 9 (temuan F5 L5) yang sebelumnya 499
 * baris. Ini refactor TANPA perubahan perilaku.
 *
 * Nama berkasnya WAJIB berakhiran `Components.kt`. Nama berkas menentukan nama
 * kelas (`TranscriptComponentsKt`), dan nama kelas menentukan apakah Kover
 * menghitungnya lewat glob `*ComponentsKt` yang sudah ada. Berkas bernama
 * `TranscriptArea.kt` tidak akan cocok dengan satu pun glob dan diam-diam
 * menurunkan angka coverage.
 */
@Composable
internal fun TranscriptArea(uiState: VoiceUiState, onOpenWeather: () -> Unit) {
    val promptRes = when (uiState.phase) {
        VoicePhase.PREPARING -> R.string.voice_prompt_preparing
        VoicePhase.LISTENING -> R.string.voice_prompt_listening
        VoicePhase.PROCESSING -> R.string.voice_prompt_processing
        VoicePhase.THINKING -> R.string.ai_thinking
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
            return@Column
        }

        Text(
            text = stringResource(R.string.ai_question_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = uiState.transcript,
            // Pertanyaan mengecil setelah ada jawaban: jawabannya yang dicari
            // petani, pertanyaan hanya konteks.
            style = if (uiState.answer.isBlank()) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("voice_transcript"),
        )

        if (uiState.isThinking) {
            Spacer(modifier = Modifier.height(Dimens.SpaceMedium))
            CircularProgressIndicator(modifier = Modifier.testTag("voice_thinking"))
            Spacer(modifier = Modifier.height(Dimens.SpaceSmall))
            Text(
                text = stringResource(R.string.ai_thinking),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        if (uiState.answer.isNotBlank()) {
            Spacer(modifier = Modifier.height(Dimens.SpaceMedium))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(Dimens.SpaceMedium))
            Text(
                text = uiState.answer,
                // Jawaban adalah informasi utama layar ini, jadi ia yang
                // mendapat ukuran terbesar.
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .testTag("voice_answer")
                    // Jawaban muncul tanpa petani menyentuh apa pun. Tanpa
                    // liveRegion, pengguna TalkBack tidak diberi tahu bahwa
                    // jawabannya sudah datang dan harus menyapu layar untuk
                    // menemukannya sendiri.
                    //
                    // Polite, BUKAN Assertive: Assertive menyela, dan aplikasi
                    // ini membacakan jawabannya sendiri lewat TTS — menyela
                    // suara sendiri hanya membuat keduanya tidak terdengar.
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )

            if (uiState.isAnswerFromCache) {
                Spacer(modifier = Modifier.height(Dimens.SpaceSmall))
                Text(
                    text = stringResource(R.string.ai_answer_from_cache),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("voice_answer_cached"),
                )
            }
        }
    }

        if (uiState.answerError != null) {
            Spacer(modifier = Modifier.height(Dimens.SpaceMedium))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(Dimens.SpaceMedium))
            Text(
                text = uiState.answerError,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .testTag("voice_answer_error")
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
            Spacer(modifier = Modifier.height(Dimens.SpaceSmall))
            // Cuaca tersimpan tetap berguna tanpa internet, jadi layar ini
            // tidak pernah menjadi jalan buntu meski jawaban gagal didapat.
            TextButton(
                onClick = onOpenWeather,
                // heightIn eksplisit: tinggi bawaan TextButton Material3
                // hanya 40dp, di bawah NFR 48dp. Preseden yang sama sudah
                // dipakai tombol "ganti wilayah" di layar cuaca.
                modifier = Modifier
                    .heightIn(min = Dimens.TouchTargetMin)
                    .testTag("voice_open_weather"),
            ) {
                Text(stringResource(R.string.ai_open_weather))
            }
        }
}


private val TRANSCRIPT_MIN_HEIGHT = Dimens.MicButtonSize
