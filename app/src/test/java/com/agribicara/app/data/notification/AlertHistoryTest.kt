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

        assertFalse(history.belumPernahDikirim(badaiHariIni))
        assertEquals(LocalDate.of(2026, 8, 31), badaiHariIni.date)
    }
}
