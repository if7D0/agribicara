package com.agribicara.app.presentation

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.agribicara.app.R
import com.agribicara.app.presentation.license.LicenseContent
import com.agribicara.app.presentation.license.LicenseUiState
import com.agribicara.app.presentation.theme.AgriBicaraTheme
import org.junit.Rule
import org.junit.Test

/**
 * Layar Lisensi benar-benar MENAMPILKAN atribusinya.
 *
 * `LicenseNoticeInvariantTest` sudah menjaga isi aset tetap identik dengan
 * `ml/NOTICE`, tetapi aset yang benar tidak ada gunanya bila layarnya tidak
 * pernah merendernya. Kewajiban Apache 2.0 baru terpenuhi ketika teks itu
 * sampai ke mata pengguna, dan itulah yang diuji di sini.
 *
 * [LicenseContent] yang dipanggil, bukan `LicenseScreen`: yang terakhir
 * memakai `hiltViewModel()` dan menuntut graf Hilt yang hidup — alasan yang
 * sama dengan [HomeContentTest].
 */
class LicenseContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun teks(id: Int): String = context.getString(id)

    /** Cuplikan seperlunya; isi sungguhannya dijaga test invarian terpisah. */
    private val contohNotice = """
        AgriBicara — model deteksi penyakit padi (Fase 4)

        Paddy Doctor: A Visual Image Dataset for Automated Paddy Disease
        Classification and Benchmarking.

        Dataset dilisensikan di bawah Apache License, Version 2.0.
    """.trimIndent()

    @Test
    fun teksAtribusiDitampilkanApaAdanya() {
        composeRule.setContent {
            AgriBicaraTheme {
                LicenseContent(uiState = LicenseUiState(noticeText = contohNotice))
            }
        }

        composeRule.onNodeWithTag("license_notice_text").assertIsDisplayed()
    }

    @Test
    fun judulDanSumberDataIkutTampil() {
        composeRule.setContent {
            AgriBicaraTheme {
                LicenseContent(uiState = LicenseUiState(noticeText = contohNotice))
            }
        }

        composeRule.onNodeWithText(teks(R.string.license_title)).assertIsDisplayed()
        composeRule.onNodeWithText(teks(R.string.license_data_sources_body)).assertIsDisplayed()
    }

    @Test
    fun kegagalanMembacaAsetDijelaskanBukanLayarKosong() {
        // Layar KOSONG pada layar lisensi terlihat seperti aplikasi
        // menyembunyikan atribusinya. Pesan penggantinya menyebut di mana
        // teksnya masih bisa ditemukan.
        composeRule.setContent {
            AgriBicaraTheme {
                LicenseContent(uiState = LicenseUiState(hasFailed = true))
            }
        }

        composeRule.onNodeWithTag("license_notice_error").assertIsDisplayed()
    }
}
