package com.agribicara.app.data.notification

import com.agribicara.app.data.local.dao.SentAlertDao
import com.agribicara.app.data.local.entity.SentAlertEntity
import com.agribicara.app.domain.model.AlertReason
import com.agribicara.app.domain.model.WeatherAlert
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gerbang anti-pengulangan peringatan cuaca (H3 review Fase 6).
 *
 * Tanpa gerbang ini satu badai yang diramalkan untuk besok diberitahukan sampai
 * empat kali sehari — pemeriksaan berjalan tiap 6 jam sementara jangkauannya 2
 * hari. Itu persis kegagalan yang KDoc `ExtremeWeatherRule` peringatkan:
 * notifikasi yang mengganggu berulang kali akan dimatikan petani, dan setelah
 * dimatikan peringatan yang benar pun tidak akan pernah sampai lagi.
 *
 * Diuji di JVM dengan DAO palsu, bukan instrumented: yang diuji di sini adalah
 * KEBIJAKAN pembandingnya. Bahwa Room benar-benar bisa menyimpan dan membaca
 * barisnya diuji terpisah di `androidTest/SentAlertDaoTest`.
 *
 * Test terakhir di berkas ini memaku sebuah CELAH, bukan sebuah jaminan.
 * Gerbang ini hanya mengingat satu peringatan terakhir, jadi kejadian lain
 * yang menyela bisa membuat peringatan lama lolos lagi. Ia ditulis supaya
 * batas itu terlihat dan tidak dikira sudah tertutup.
 */
class AlertHistoryTest {

    private val dao = mockk<SentAlertDao>(relaxed = true)

    private val clock: Clock = Clock.fixed(
        Instant.parse("2026-08-31T06:00:00Z"),
        ZoneId.of("Asia/Jakarta"),
    )

    private val history = AlertHistory(dao, clock)

    private val badaiHariIni = WeatherAlert(
        regionCode = "11.01.01.2001",
        regionName = "Keude Bakongan",
        date = LocalDate.of(2026, 8, 31),
        reason = AlertReason.THUNDERSTORM,
    )

    private fun tercatat(
        regionCode: String = "11.01.01.2001",
        date: String = "2026-08-31",
        reason: String = "THUNDERSTORM",
    ) = SentAlertEntity(
        regionCode = regionCode,
        date = date,
        reason = reason,
        notifiedAt = 0L,
    )

    @Test
    fun `tanpa catatan apa pun peringatan pertama boleh lewat`() = runTest {
        coEvery { dao.get(any()) } returns null

        assertTrue(history.belumPernahDikirim(badaiHariIni))
    }

    @Test
    fun `peringatan yang identik ditahan`() = runTest {
        coEvery { dao.get(any()) } returns tercatat()

        assertFalse(history.belumPernahDikirim(badaiHariIni))
    }

    @Test
    fun `tanggal berbeda adalah kejadian berbeda`() = runTest {
        coEvery { dao.get(any()) } returns tercatat(date = "2026-08-30")

        assertTrue(history.belumPernahDikirim(badaiHariIni))
    }

    @Test
    fun `alasan berbeda adalah kejadian berbeda`() = runTest {
        // Badai yang berubah menjadi hujan lebat menuntut persiapan berbeda.
        coEvery { dao.get(any()) } returns tercatat(reason = "HEAVY_RAIN")

        assertTrue(history.belumPernahDikirim(badaiHariIni))
    }

    @Test
    fun `wilayah berbeda adalah kejadian berbeda`() = runTest {
        coEvery { dao.get(any()) } returns tercatat(regionCode = "32.77.01.1002")

        assertTrue(history.belumPernahDikirim(badaiHariIni))
    }

    @Test
    fun `nama wilayah yang berbeda ejaan BUKAN kejadian baru`() = runTest {
        // BMKG dan Open-Meteo bisa mengeja nama desa berbeda. Kalau nama ikut
        // dibandingkan, satu badai akan diberitahukan dua kali hanya karena
        // sumber datanya berganti.
        coEvery { dao.get(any()) } returns tercatat()

        val ejaanLain = badaiHariIni.copy(regionName = "KEUDE BAKONGAN")

        assertFalse(history.belumPernahDikirim(ejaanLain))
    }

    @Test
    fun `mencatat menyimpan wilayah tanggal dan alasan beserta waktunya`() = runTest {
        history.catat(badaiHariIni)

        coVerify(exactly = 1) {
            dao.upsert(
                match<SentAlertEntity> {
                    it.regionCode == "11.01.01.2001" &&
                        it.date == "2026-08-31" &&
                        it.reason == "THUNDERSTORM" &&
                        it.notifiedAt == clock.millis()
                },
            )
        }
    }

    @Test
    fun `tanggal disimpan dalam bentuk ISO yang bisa dibandingkan sebagai teks`() = runTest {
        // Project ini sengaja tidak punya TypeConverter; bentuk ISO membuat
        // perbandingan string tetap benar tanpa parsing.
        history.catat(badaiHariIni.copy(date = LocalDate.of(2026, 9, 1)))

        coVerify { dao.upsert(match<SentAlertEntity> { it.date == "2026-09-01" }) }
    }

    @Test
    fun `peringatan besok yang menjadi hari ini TIDAK dikirim ulang`() = runTest {
        // Batas yang disengaja dan dicatat terbuka: labelnya berubah dari
        // "besok" menjadi "hari ini", tetapi tanggalnya sama dan petani sudah
        // diberi tahu tentang kejadian itu.
        coEvery { dao.get(any()) } returns tercatat(date = "2026-08-31")

        // `badaiHariIni` bertanggal 2026-08-31, sama dengan yang tercatat di
        // atas — itulah yang membuat kasus ini bermakna. Dulu fakta itu ditulis
        // sebagai assertEquals, tetapi ia hanya memeriksa fixture milik test
        // ini sendiri: tidak ada perubahan kode produksi yang bisa
        // memerahkannya. Jaring pengaman palsu lebih buruk daripada tidak ada,
        // karena ia terlihat seperti perlindungan.
        assertFalse(history.belumPernahDikirim(badaiHariIni))
    }

    @Test
    fun `peringatan yang diselingi kejadian lain LOLOS lagi - batas yang diketahui`() = runTest {
        // Ini BUKAN perilaku yang diinginkan. Test ini memakunya supaya
        // batasnya terlihat, bukan supaya ia dianggap benar.
        //
        // Hanya SATU peringatan terakhir yang diingat (SentAlertDao.get()
        // membaca satu baris ber-id tetap), jadi yang dijamin bukan "belum
        // pernah dikirim" melainkan "tidak identik dengan yang TERAKHIR
        // dikirim". Begitu ada kejadian lain menyela, catatan peringatan
        // pertama tertimpa dan ia bisa dikirim ulang.
        //
        // Urutan ini bisa terjadi sungguhan: ExtremeWeatherRule memakai
        // firstNotNullOfOrNull sehingga selalu mengambil hari memenuhi syarat
        // PALING AWAL. Hujan sore hari ini terkirim pagi; siang harinya slot
        // itu lewat sehingga yang paling awal menjadi badai besok; sore
        // pembaruan BMKG menambah slot hujan malam untuk hari ini, dan hari
        // ini kembali menjadi yang paling awal.
        var tersimpan: SentAlertEntity? = null
        coEvery { dao.get(any()) } answers { tersimpan }
        coEvery { dao.upsert(any()) } answers { tersimpan = firstArg() }

        val hujanHariIni = badaiHariIni.copy(reason = AlertReason.HEAVY_RAIN)
        val badaiBesok = badaiHariIni.copy(date = LocalDate.of(2026, 9, 1))

        assertTrue(history.belumPernahDikirim(hujanHariIni))
        history.catat(hujanHariIni)

        assertTrue(history.belumPernahDikirim(badaiBesok))
        history.catat(badaiBesok)

        // Petani sudah diberi tahu tentang hujan hari ini, tetapi gerbangnya
        // melewatkannya lagi karena catatannya sudah tertimpa badai besok.
        assertTrue(
            "Batas yang diketahui: peringatan yang diselingi kejadian lain lolos lagi",
            history.belumPernahDikirim(hujanHariIni),
        )
    }
}
