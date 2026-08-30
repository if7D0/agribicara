package com.agribicara.app.presentation

import android.content.Context
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.agribicara.app.R
import com.agribicara.app.domain.model.DailyForecast
import com.agribicara.app.domain.model.WeatherSource
import com.agribicara.app.presentation.home.HomeContent
import com.agribicara.app.presentation.home.HomeUiState
import com.agribicara.app.presentation.theme.AgriBicaraTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

/**
 * Invarian aksesibilitas layar Home.
 *
 * Ada 27 `testTag` di aplikasi ini sejak Fase 1 dan sampai Fase 5 NOL test
 * memakainya. Test di sini menjaga hal yang mudah rusak diam-diam: tombol yang
 * kehilangan contentDescription, dan kartu cuaca yang pecah kembali menjadi
 * beberapa perhentian TalkBack terpisah.
 *
 * [HomeContent] yang dipanggil, bukan `HomeScreen`: yang terakhir memakai
 * `hiltViewModel()` dan menuntut graf Hilt yang hidup.
 */
class HomeContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun teks(id: Int): String = context.getString(id)

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

    private fun tampilkan(uiState: HomeUiState) {
        composeRule.setContent {
            AgriBicaraTheme {
                HomeContent(
                    uiState = uiState,
                    onOpenWeather = {},
                    onChooseRegion = {},
                    onOpenVoice = {},
                )
            }
        }
    }

    @Test
    fun tombolMicSelaluPunyaContentDescription() {
        tampilkan(HomeUiState())

        // Tombol ini satu-satunya aksi utama layar. Tanpa contentDescription,
        // pengguna TalkBack hanya mendengar "tombol" tanpa tahu fungsinya.
        composeRule.onNodeWithTag("mic_button")
            .assertIsDisplayed()
            .assertContentDescriptionEquals(teks(R.string.cd_mic_button))
    }

    @Test
    fun kartuCuacaDibacaSebagaiSatuKalimatUtuh() {
        tampilkan(
            HomeUiState(
                regionName = "Keude Bakongan",
                today = hariIni,
                source = WeatherSource.BMKG,
            ),
        )

        // Satu node, satu kalimat. Kalau penggabungan hilang, TalkBack kembali
        // membacakan suhu, deskripsi, dan wilayah sebagai perhentian terpisah.
        composeRule.onNodeWithTag("weather_placeholder")
            .assertIsDisplayed()
            .assertContentDescriptionEquals(
                "Cuaca hari ini, Keude Bakongan, 30 derajat Celsius, Berawan",
            )
    }

    @Test
    fun modeOfflineIkutTerbacaDiKartu() {
        tampilkan(
            HomeUiState(
                regionName = "Keude Bakongan",
                today = hariIni,
                source = WeatherSource.CACHE,
            ),
        )

        // Data lama yang tidak diberi tanda membuat petani memutuskan
        // berdasarkan cuaca kemarin tanpa sadar — pengguna TalkBack tidak
        // boleh kehilangan peringatan yang dilihat pengguna awas.
        composeRule.onNodeWithTag("weather_placeholder")
            .assertContentDescriptionEquals(
                "Cuaca hari ini, Keude Bakongan, 30 derajat Celsius, Berawan, " +
                    teks(R.string.weather_offline_banner),
            )
    }

    @Test
    fun belumPilihWilayahTetapMemunculkanAjakan() {
        tampilkan(HomeUiState(needsRegion = true))

        composeRule.onNodeWithText(teks(R.string.weather_no_region)).assertIsDisplayed()
    }

    @Test
    fun tombolMicTetapAdaSaatCuacaGagalDimuat() {
        // Kegagalan cuaca TIDAK boleh memblokir aksi utama layar.
        tampilkan(HomeUiState(today = null, regionName = "Keude Bakongan"))

        composeRule.onNodeWithTag("mic_button").assertIsDisplayed()
    }
}
