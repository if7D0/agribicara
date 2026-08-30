package com.agribicara.app.domain.ai

import com.agribicara.app.core.common.Constants
import com.agribicara.app.domain.repository.ChatHistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnswerCacheTest {

    private val chatHistoryRepository = mockk<ChatHistoryRepository>()

    private val now = Instant.parse("2026-08-30T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("UTC"))

    private fun cache() = AnswerCache(chatHistoryRepository, clock)

    // --- normalisasi kunci -------------------------------------------------

    @Test
    fun `huruf besar dan tanda tanya menghasilkan kunci yang sama`() {
        assertEquals(
            AnswerCache.keyOf("kapan memupuk padi"),
            AnswerCache.keyOf("Kapan Memupuk Padi?"),
        )
    }

    @Test
    fun `spasi berlebih dirapikan`() {
        assertEquals("kapan memupuk padi", AnswerCache.keyOf("  kapan   memupuk  padi  "))
    }

    @Test
    fun `tanda baca dibuang tanpa memecah kata`() {
        assertEquals("padi", AnswerCache.keyOf("padi?"))
        assertEquals("padi jagung", AnswerCache.keyOf("padi, jagung"))
    }

    @Test
    fun `angka dipertahankan`() {
        assertEquals("pupuk npk 15", AnswerCache.keyOf("Pupuk NPK 15!"))
    }

    @Test
    fun `pertanyaan kosong menghasilkan kunci kosong`() {
        assertEquals("", AnswerCache.keyOf("   "))
        assertEquals("", AnswerCache.keyOf("???"))
    }

    // --- pencarian ---------------------------------------------------------

    @Test
    fun `pertanyaan kosong tidak menyentuh penyimpanan`() = runTest {
        val result = cache().find("???", "32.77.01.1002")

        assertNull(result)
        coVerify(exactly = 0) { chatHistoryRepository.findCachedAnswer(any(), any(), any()) }
    }

    @Test
    fun `pencarian memakai kunci yang sudah dinormalkan`() = runTest {
        val key = slot<String>()
        coEvery {
            chatHistoryRepository.findCachedAnswer(capture(key), any(), any())
        } returns "Jawaban."

        cache().find("Kapan Memupuk Padi?", "32.77.01.1002")

        assertEquals("kapan memupuk padi", key.captured)
    }

    @Test
    fun `batas kesegaran dihitung dari jam cache yang ditetapkan`() = runTest {
        val notBefore = slot<Long>()
        coEvery {
            chatHistoryRepository.findCachedAnswer(any(), any(), capture(notBefore))
        } returns null

        cache().find("kapan memupuk padi", null)

        // Jawaban ber-grounding cuaca tidak boleh hidup lebih lama daripada
        // cuaca yang mendasarinya.
        val expected = now.toEpochMilli() -
            TimeUnit.HOURS.toMillis(Constants.ANSWER_CACHE_HOURS)
        assertEquals(expected, notBefore.captured)
    }

    @Test
    fun `kode wilayah diteruskan apa adanya termasuk null`() = runTest {
        val region = slot<String>()
        coEvery {
            chatHistoryRepository.findCachedAnswer(any(), capture(region), any())
        } returns null

        cache().find("kapan memupuk padi", "32.77.01.1002")

        assertEquals("32.77.01.1002", region.captured)
    }

    @Test
    fun `jawaban yang ditemukan dikembalikan apa adanya`() = runTest {
        coEvery {
            chatHistoryRepository.findCachedAnswer(any(), any(), any())
        } returns "Pupuk Sabtu pagi."

        assertEquals("Pupuk Sabtu pagi.", cache().find("kapan memupuk padi", null))
    }

    @Test
    fun `tidak ada yang cocok mengembalikan null`() = runTest {
        coEvery { chatHistoryRepository.findCachedAnswer(any(), any(), any()) } returns null

        assertNull(cache().find("kapan memupuk padi", null))
    }
}
