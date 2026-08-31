package com.agribicara.app.presentation

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import com.agribicara.app.domain.model.DailyForecast
import com.agribicara.app.domain.model.TtsStatus
import com.agribicara.app.domain.model.WeatherSource
import com.agribicara.app.presentation.home.HomeContent
import com.agribicara.app.presentation.home.HomeUiState
import com.agribicara.app.presentation.theme.AgriBicaraTheme
import com.agribicara.app.presentation.theme.Dimens
import com.agribicara.app.presentation.voice.VoiceContent
import com.agribicara.app.presentation.voice.VoiceUiState
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * NFR aksesibilitas PRD: setiap elemen interaktif minimal 48dp.
 *
 * `Dimens.TouchTargetMin` sudah ada sejak Fase 1, tetapi adanya token bukan
 * bukti token itu dipatuhi — sampai Fase 6 tidak ada satu pun pemeriksaan.
 * Test ini menelusuri SEMUA node yang bisa diklik, jadi elemen interaktif baru
 * ikut terjaring otomatis tanpa perlu menambah test.
 *
 * Ambangnya dibaca dari [Dimens.TouchTargetMin], bukan angka 48 yang ditulis
 * ulang: kalau tokennya diturunkan diam-diam, test ini harus ikut memerah.
 */
class TouchTargetInvariantTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val hariIni = DailyForecast(
        date = LocalDate.of(2026, 8, 31),
        temperatureMax = 30.0,
        temperatureMin = 24.0,
        precipitationMm = 0.0,
        windSpeed = 4.0,
        weatherCode = 3,
        description = "Berawan",
        source = WeatherSource.BMKG,
    )

    private fun semuaTargetSentuhCukupBesar() {
        // waitForIdle() WAJIB sebelum fetchSemanticsNodes(): berbeda dengan
        // assertIsDisplayed() yang menyinkronkan sendiri, pengambilan node
        // massal tidak menunggu komposisi pertama selesai dan gagal dengan
        // "No compose hierarchies found" — pesan yang menyesatkan, karena
        // hierarkinya ada, hanya belum sempat terbentuk.
        composeRule.waitForIdle()

        val nodes = composeRule.onAllNodes(hasClickAction())
        val jumlah = nodes.fetchSemanticsNodes().size
        // Nol node berarti layarnya tidak jadi dirender — itu kegagalan test,
        // bukan kelulusan diam-diam.
        // assertTrue JUnit, BUKAN kotlin.assert: yang terakhir dijaga
        // _Assertions.ENABLED yang berasal dari desiredAssertionStatus(), dan
        // di Android nilainya false. Penjaga ini sebelumnya no-op — persis
        // kelulusan diam-diam yang komentarnya klaim dicegah.
        assertTrue("Tidak ada elemen interaktif yang ditemukan", jumlah > 0)

        repeat(jumlah) { index ->
            nodes[index]
                .assertWidthIsAtLeast(Dimens.TouchTargetMin)
                .assertHeightIsAtLeast(Dimens.TouchTargetMin)
        }
    }

    @Test
    fun semuaElemenInteraktifHomeMinimal48dp() {
        composeRule.setContent {
            AgriBicaraTheme {
                HomeContent(
                    uiState = HomeUiState(regionName = "Keude Bakongan", today = hariIni),
                    onOpenWeather = {},
                    onChooseRegion = {},
                    onOpenVoice = {},
                )
            }
        }

        semuaTargetSentuhCukupBesar()
    }

    @Test
    fun semuaElemenInteraktifLayarSuaraMinimal48dp() {
        composeRule.setContent {
            AgriBicaraTheme {
                VoiceContent(
                    uiState = VoiceUiState(
                        transcript = "kapan waktu terbaik memupuk padi",
                        answer = "Hari ini cuaca berawan.",
                        answerError = "Layanan jawaban sedang tidak bisa dihubungi.",
                        ttsStatus = TtsStatus.READY,
                    ),
                    onOpenWeather = {},
                    onSubmitText = {},
                    onSwitchToVoice = {},
                    onMicClick = {},
                    onReplay = {},
                    onSwitchToText = {},
                )
            }
        }

        semuaTargetSentuhCukupBesar()
    }

    @Test
    fun elemenInteraktifJalurTeksMinimal48dp() {
        // Jalur teks adalah cadangan ketika suara tidak bisa dipakai; ia tidak
        // boleh jadi jalur kelas dua dari sisi aksesibilitas.
        composeRule.setContent {
            AgriBicaraTheme {
                VoiceContent(
                    uiState = VoiceUiState(isTextMode = true),
                    onOpenWeather = {},
                    onSubmitText = {},
                    onSwitchToVoice = {},
                    onMicClick = {},
                    onReplay = {},
                    onSwitchToText = {},
                )
            }
        }

        semuaTargetSentuhCukupBesar()
    }
}
