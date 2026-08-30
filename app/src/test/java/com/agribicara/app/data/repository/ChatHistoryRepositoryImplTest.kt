package com.agribicara.app.data.repository

import com.agribicara.app.core.common.Constants
import com.agribicara.app.core.common.DispatcherProvider
import com.agribicara.app.data.local.dao.ChatMessageDao
import com.agribicara.app.data.local.entity.ChatMessageEntity
import com.agribicara.app.domain.model.ChatMessage
import com.agribicara.app.domain.model.ChatRole
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Riwayat percakapan: menyimpan, mencari jawaban tersimpan, dan memetakan baris.
 *
 * Dua sifat yang diuji di sini menentukan apakah petani kehilangan jawaban atau
 * tidak: kegagalan penyimpanan HARUS ditelan (jawaban yang sudah di layar tidak
 * boleh hilang gara-gara Room), sedangkan pembatalan coroutine TIDAK boleh
 * ditelan.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatHistoryRepositoryImplTest {

    private val dao = mockk<ChatMessageDao>(relaxed = true)

    private val dispatchers = object : DispatcherProvider {
        private val test = UnconfinedTestDispatcher()
        override val io = test
        override val default = test
        override val main = test
    }

    private val repository = ChatHistoryRepositoryImpl(
        chatMessageDao = dao,
        dispatchers = dispatchers,
    )

    private fun pesan(
        role: ChatRole = ChatRole.USER,
        text: String = "kapan waktu terbaik memupuk padi",
    ) = ChatMessage(
        role = role,
        text = text,
        questionKey = "kapan waktu terbaik memupuk padi",
        regionCode = "11.01.01.2001",
        createdAt = 1_756_000_000_000L,
    )

    private fun baris(
        id: Long = 1L,
        role: String = "USER",
        text: String = "kapan waktu terbaik memupuk padi",
    ) = ChatMessageEntity(
        id = id,
        role = role,
        text = text,
        questionKey = "kapan waktu terbaik memupuk padi",
        regionCode = "11.01.01.2001",
        createdAt = 1_756_000_000_000L,
    )

    @Test
    fun `append menyimpan baris lalu memangkas tabel`() = runTest {
        repository.append(pesan())

        coVerify(exactly = 1) {
            dao.insert(
                match {
                    it.role == "USER" && it.text == "kapan waktu terbaik memupuk padi"
                },
            )
        }
        // Dipangkas setiap kali menulis, bukan lewat pekerjaan terjadwal:
        // perangkat murah tidak selalu sempat menjalankan WorkManager.
        coVerify(exactly = 1) { dao.trimTo(Constants.CHAT_HISTORY_LIMIT) }
    }

    @Test
    fun `kegagalan menyimpan ditelan supaya jawaban tidak ikut hilang`() = runTest {
        coEvery { dao.insert(any()) } throws IOException("disk penuh")

        // Tidak ada assertion selain "tidak melempar": jawaban yang sudah
        // berhasil didapat tidak boleh lenyap dari layar hanya karena
        // penyimpanan riwayat bermasalah.
        repository.append(pesan())

        coVerify(exactly = 0) { dao.trimTo(any()) }
    }

    @Test
    fun `findCachedAnswer meneruskan kunci wilayah dan batas waktu apa adanya`() = runTest {
        coEvery {
            dao.findAnswer("kapan memupuk", "11.01.01.2001", 1_755_000_000_000L)
        } returns "Besok cerah."

        val hasil = repository.findCachedAnswer(
            questionKey = "kapan memupuk",
            regionCode = "11.01.01.2001",
            notBefore = 1_755_000_000_000L,
        )

        assertEquals("Besok cerah.", hasil)
    }

    @Test
    fun `kegagalan membaca cache menjadi null bukan exception`() = runTest {
        coEvery { dao.findAnswer(any(), any(), any()) } throws IllegalStateException("db rusak")

        // Cache adalah jalur darurat ketika AI gagal. Kalau jalur darurat ikut
        // melempar, petani mendapat crash justru pada saat paling buruk.
        assertNull(repository.findCachedAnswer("kapan memupuk", null, 0L))
    }

    @Test
    fun `pembatalan coroutine diteruskan bukan ditelan`() = runTest {
        coEvery { dao.insert(any()) } throws CancellationException("dibatalkan")

        var terlempar = false
        try {
            repository.append(pesan())
        } catch (e: CancellationException) {
            terlempar = true
        }

        assertTrue("CancellationException harus diteruskan", terlempar)
    }

    @Test
    fun `baris dengan peran tak dikenal dibuang bukan membuat crash`() = runTest {
        // Bisa berasal dari APK versi lain yang menulis peran baru; saat
        // downgrade, baris itu tetap ada di database yang sama.
        every { dao.observeRecent(any()) } returns flowOf(
            listOf(
                baris(id = 1, role = "USER"),
                baris(id = 2, role = "SYSTEM", text = "peran masa depan"),
                baris(id = 3, role = "ASSISTANT", text = "Besok cerah."),
            ),
        )

        val riwayat = repository.observeHistory(Constants.CHAT_HISTORY_LIMIT).first()

        assertEquals(listOf(1L, 3L), riwayat.map { it.id })
        assertEquals(listOf(ChatRole.USER, ChatRole.ASSISTANT), riwayat.map { it.role })
    }

    @Test
    fun `seluruh isi baris ikut terbawa saat dipetakan`() = runTest {
        every { dao.observeRecent(any()) } returns flowOf(listOf(baris(id = 7)))

        val pesanRiwayat = repository.observeHistory(Constants.CHAT_HISTORY_LIMIT).first().single()

        assertEquals(7L, pesanRiwayat.id)
        assertEquals(ChatRole.USER, pesanRiwayat.role)
        assertEquals("kapan waktu terbaik memupuk padi", pesanRiwayat.text)
        assertEquals("kapan waktu terbaik memupuk padi", pesanRiwayat.questionKey)
        assertEquals("11.01.01.2001", pesanRiwayat.regionCode)
        assertEquals(1_756_000_000_000L, pesanRiwayat.createdAt)
    }

    @Test
    fun `batas riwayat diteruskan ke query`() = runTest {
        every { dao.observeRecent(any()) } returns flowOf(emptyList())

        repository.observeHistory(Constants.CHAT_HISTORY_LIMIT).first()

        coVerify { dao.observeRecent(Constants.CHAT_HISTORY_LIMIT) }
    }
}
