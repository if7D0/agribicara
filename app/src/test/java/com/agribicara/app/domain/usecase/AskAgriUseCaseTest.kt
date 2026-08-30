package com.agribicara.app.domain.usecase

import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.domain.ai.AnswerCache
import com.agribicara.app.domain.model.AiAnswer
import com.agribicara.app.domain.model.AnswerSource
import com.agribicara.app.domain.model.ChatMessage
import com.agribicara.app.domain.model.ChatRole
import com.agribicara.app.domain.model.DailyForecast
import com.agribicara.app.domain.model.Forecast
import com.agribicara.app.domain.model.Region
import com.agribicara.app.domain.model.RegionLevel
import com.agribicara.app.domain.model.WeatherSource
import com.agribicara.app.domain.repository.AiRepository
import com.agribicara.app.domain.repository.ChatHistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AskAgriUseCaseTest {

    private val aiRepository = mockk<AiRepository>()
    private val chatHistoryRepository = mockk<ChatHistoryRepository>(relaxed = true)
    private val answerCache = mockk<AnswerCache>()
    private val getForecast = mockk<GetForecastUseCase>()
    private val observeSelectedRegion = mockk<ObserveSelectedRegionUseCase>()

    private val clock = Clock.fixed(Instant.parse("2026-08-30T03:00:00Z"), ZoneId.of("UTC"))

    private val cibeureum = Region("32.77.01.1002", "Cibeureum", RegionLevel.VILLAGE)

    private val forecast = Forecast(
        regionCode = cibeureum.code,
        regionName = cibeureum.name,
        days = listOf(
            DailyForecast(
                date = LocalDate.of(2026, 8, 30),
                temperatureMax = 31.0,
                temperatureMin = 24.0,
                precipitationMm = 0.0,
                windSpeed = 4.0,
                weatherCode = 1,
                description = "Cerah",
                source = WeatherSource.BMKG,
            ),
        ),
        source = WeatherSource.BMKG,
        fetchedAt = 0L,
    )

    private fun useCase() = AskAgriUseCase(
        aiRepository = aiRepository,
        chatHistoryRepository = chatHistoryRepository,
        answerCache = answerCache,
        getForecast = getForecast,
        observeSelectedRegion = observeSelectedRegion,
        clock = clock,
    )

    private fun withRegion(region: Region? = cibeureum) {
        every { observeSelectedRegion() } returns flowOf(region)
    }

    private fun withForecast(result: NetworkResult<Forecast> = NetworkResult.Success(forecast)) {
        coEvery { getForecast(any(), any()) } returns result
    }

    @Test
    fun `jawaban sukses dikembalikan sebagai sumber AI`() = runTest {
        withRegion()
        withForecast()
        coEvery { aiRepository.ask(any()) } returns NetworkResult.Success("Pupuk Sabtu pagi.")

        val result = useCase()("kapan memupuk padi")

        assertEquals(
            NetworkResult.Success(AiAnswer("Pupuk Sabtu pagi.", AnswerSource.AI)),
            result,
        )
    }

    @Test
    fun `jawaban sukses menyimpan dua baris riwayat`() = runTest {
        withRegion()
        withForecast()
        coEvery { aiRepository.ask(any()) } returns NetworkResult.Success("Pupuk Sabtu pagi.")

        val saved = mutableListOf<ChatMessage>()
        coEvery { chatHistoryRepository.append(capture(saved)) } returns Unit

        useCase()("kapan memupuk padi")

        assertEquals(2, saved.size)
        assertEquals(ChatRole.USER, saved[0].role)
        assertEquals(ChatRole.ASSISTANT, saved[1].role)
        // Kedua baris memakai kunci yang sama; kalau berbeda, cache tidak akan
        // pernah menemukan jawaban ini lagi.
        assertEquals(saved[0].questionKey, saved[1].questionKey)
        assertEquals(cibeureum.code, saved[1].regionCode)
    }

    @Test
    fun `prompt memuat cuaca ketika wilayah dan prakiraan tersedia`() = runTest {
        withRegion()
        withForecast()
        val prompt = slot<String>()
        coEvery { aiRepository.ask(capture(prompt)) } returns NetworkResult.Success("ok")

        useCase()("kapan memupuk padi")

        assertTrue(prompt.captured.contains("Cibeureum"))
        assertTrue(prompt.captured.contains("Cerah"))
    }

    @Test
    fun `AI gagal jatuh ke jawaban tersimpan`() = runTest {
        withRegion()
        withForecast()
        coEvery { aiRepository.ask(any()) } returns NetworkResult.Error("jaringan mati")
        coEvery { answerCache.find(any(), any()) } returns "Jawaban kemarin."

        val result = useCase()("kapan memupuk padi")

        assertEquals(
            NetworkResult.Success(AiAnswer("Jawaban kemarin.", AnswerSource.CACHE)),
            result,
        )
    }

    @Test
    fun `AI gagal tanpa cache meneruskan pesan kegagalan aslinya`() = runTest {
        withRegion()
        withForecast()
        // Pesan asli dipertahankan, bukan diganti pesan generik: "tidak ada
        // internet" dan "layanan bermasalah" menuntut tindakan berbeda.
        coEvery { aiRepository.ask(any()) } returns NetworkResult.Error("tidak ada internet")
        coEvery { answerCache.find(any(), any()) } returns null

        val result = useCase()("kapan memupuk padi")

        assertEquals(NetworkResult.Error("tidak ada internet"), result)
    }

    @Test
    fun `cuaca gagal tidak memblokir pertanyaan`() = runTest {
        withRegion()
        withForecast(NetworkResult.Error("cuaca mati"))
        val prompt = slot<String>()
        coEvery { aiRepository.ask(capture(prompt)) } returns NetworkResult.Success("ok")

        val result = useCase()("berapa jarak tanam padi")

        assertTrue(result is NetworkResult.Success)
        // Ketiadaan cuaca harus DINYATAKAN di prompt, bukan dihilangkan diam-diam.
        assertTrue(prompt.captured.contains("tidak tersedia"))
    }

    @Test
    fun `wilayah belum dipilih tetap bertanya tanpa grounding`() = runTest {
        withRegion(region = null)
        val prompt = slot<String>()
        coEvery { aiRepository.ask(capture(prompt)) } returns NetworkResult.Success("ok")

        val result = useCase()("berapa jarak tanam padi")

        assertTrue(result is NetworkResult.Success)
        assertTrue(prompt.captured.contains("tidak tersedia"))
        // Cuaca tidak boleh diminta sama sekali kalau wilayah belum dipilih.
        coVerify(exactly = 0) { getForecast(any(), any()) }
    }

    @Test
    fun `pertanyaan kosong ditolak tanpa memanggil AI`() = runTest {
        withRegion()

        val result = useCase()("   ")

        assertTrue(result is NetworkResult.Error)
        coVerify(exactly = 0) { aiRepository.ask(any()) }
    }

    @Test
    fun `pertanyaan hanya tanda baca ditolak tanpa memanggil AI`() = runTest {
        withRegion()

        val result = useCase()("???")

        assertTrue(result is NetworkResult.Error)
        coVerify(exactly = 0) { aiRepository.ask(any()) }
    }

    @Test
    fun `pertanyaan sangat panjang tetap diteruskan`() = runTest {
        withRegion()
        withForecast()
        coEvery { aiRepository.ask(any()) } returns NetworkResult.Success("ok")

        val long = "kapan memupuk padi ".repeat(200)
        val result = useCase()(long)

        assertTrue(result is NetworkResult.Success)
    }

    @Test
    fun `cache dicari dengan kode wilayah saat ini`() = runTest {
        withRegion()
        withForecast()
        coEvery { aiRepository.ask(any()) } returns NetworkResult.Error("gagal")
        val region = slot<String>()
        coEvery { answerCache.find(any(), capture(region)) } returns null

        useCase()("kapan memupuk padi")

        assertEquals(cibeureum.code, region.captured)
    }
}
