package com.agribicara.app.presentation

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.agribicara.app.R
import com.agribicara.app.domain.model.TtsStatus
import com.agribicara.app.presentation.theme.AgriBicaraTheme
import com.agribicara.app.presentation.voice.VoiceContent
import com.agribicara.app.presentation.voice.VoicePhase
import com.agribicara.app.presentation.voice.VoiceUiState
import org.junit.Rule
import org.junit.Test

/**
 * Tangga degradasi Fase 5, dijaga otomatis.
 *
 * Aturan layar ini: TIDAK PERNAH ada jalan buntu. Sampai kini aturan itu hanya
 * dijaga oleh ingatan dan satu percobaan manual di perangkat. Justru jalur
 * inilah yang paling sering ditemui petani bersinyal lemah.
 */
class VoiceContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun teks(id: Int): String = context.getString(id)

    private fun tampilkan(uiState: VoiceUiState) {
        composeRule.setContent {
            AgriBicaraTheme {
                VoiceContent(
                    uiState = uiState,
                    onOpenWeather = {},
                    onSubmitText = {},
                    onSwitchToVoice = {},
                    onMicClick = {},
                    onReplay = {},
                    onSwitchToText = {},
                )
            }
        }
    }

    @Test
    fun kegagalanJawabanSelaluMenawarkanLayarCuaca() {
        tampilkan(
            VoiceUiState(
                transcript = "kapan waktu terbaik memupuk padi",
                answerError = "Layanan jawaban sedang tidak bisa dihubungi.",
            ),
        )

        // Tiga hal sekaligus, dan ketiganya adalah janji Fase 5:
        // pertanyaannya tidak hilang, pesannya menetap, dan ada jalan keluar.
        composeRule.onNodeWithTag("voice_transcript").assertIsDisplayed()
        composeRule.onNodeWithTag("voice_answer_error").assertIsDisplayed()
        composeRule.onNodeWithTag("voice_open_weather").assertIsDisplayed()
    }

    @Test
    fun pertanyaanTetapTerlihatSaatJawabanSedangDicari() {
        tampilkan(
            VoiceUiState(
                phase = VoicePhase.THINKING,
                transcript = "kapan waktu terbaik memupuk padi",
            ),
        )

        composeRule.onNodeWithTag("voice_transcript").assertIsDisplayed()
        composeRule.onNodeWithTag("voice_thinking").assertIsDisplayed()
    }

    @Test
    fun jawabanTersimpanDiberiTandaBahwaItuJawabanLama() {
        tampilkan(
            VoiceUiState(
                transcript = "kapan memupuk",
                answer = "Hari ini cuaca berawan.",
                isAnswerFromCache = true,
            ),
        )

        composeRule.onNodeWithTag("voice_answer").assertIsDisplayed()
        composeRule.onNodeWithTag("voice_answer_cached").assertIsDisplayed()
    }

    @Test
    fun tombolDengarkanLagiHanyaMunculSaatTtsSiap() {
        tampilkan(
            VoiceUiState(
                transcript = "kapan memupuk",
                answer = "Hari ini cuaca berawan.",
                ttsStatus = TtsStatus.ENGINE_UNAVAILABLE,
            ),
        )

        // Menawarkan "Dengarkan lagi" pada perangkat tanpa TTS yang berfungsi
        // adalah janji palsu; petani menekannya dan tidak terjadi apa-apa.
        composeRule.onNodeWithTag("voice_replay").assertDoesNotExist()
    }

    @Test
    fun tanpaPengenalanSuaraLayarLangsungMembukaJalurTeks() {
        tampilkan(VoiceUiState(isSpeechUnavailable = true, isTextMode = true))

        // Perangkat tanpa STT tidak boleh berakhir di layar mati.
        composeRule.onNodeWithTag("voice_text_field").assertIsDisplayed()
        composeRule.onNodeWithTag("voice_text_send").assertIsDisplayed()
    }

    @Test
    fun modeTeksTetapMenawarkanKembaliKeSuaraBilaTersedia() {
        tampilkan(VoiceUiState(isTextMode = true, isSpeechUnavailable = false))

        composeRule.onNodeWithTag("voice_switch_voice").assertIsDisplayed()
    }

    @Test
    fun keadaanAwalMenampilkanAjakanBicara() {
        tampilkan(VoiceUiState())

        composeRule.onNodeWithText(teks(R.string.voice_prompt_idle)).assertIsDisplayed()
    }
}
