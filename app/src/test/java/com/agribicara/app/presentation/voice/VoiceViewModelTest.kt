package com.agribicara.app.presentation.voice

import com.agribicara.app.MainDispatcherRule
import com.agribicara.app.domain.model.SpeechEvent
import com.agribicara.app.domain.model.TtsStatus
import com.agribicara.app.domain.usecase.IsSpeechRecognitionAvailableUseCase
import com.agribicara.app.domain.usecase.ListenForSpeechUseCase
import com.agribicara.app.domain.usecase.PrepareTextToSpeechUseCase
import com.agribicara.app.domain.usecase.SpeakTextUseCase
import com.agribicara.app.domain.usecase.StopSpeakingUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val listenForSpeech = mockk<ListenForSpeechUseCase>()
    private val speakText = mockk<SpeakTextUseCase>(relaxed = true)
    private val prepareTextToSpeech = mockk<PrepareTextToSpeechUseCase>()
    private val stopSpeaking = mockk<StopSpeakingUseCase>(relaxed = true)
    private val isAvailable = mockk<IsSpeechRecognitionAvailableUseCase>()

    private fun viewModel(
        speechAvailable: Boolean = true,
        ttsStatus: TtsStatus = TtsStatus.READY,
        events: Flow<SpeechEvent> = flowOf(),
    ): VoiceViewModel {
        every { isAvailable() } returns speechAvailable
        coEvery { prepareTextToSpeech() } returns ttsStatus
        every { listenForSpeech(any()) } returns events
        return VoiceViewModel(
            listenForSpeech = listenForSpeech,
            speakText = speakText,
            prepareTextToSpeech = prepareTextToSpeech,
            stopSpeaking = stopSpeaking,
            isSpeechRecognitionAvailable = isAvailable,
        )
    }

    @Test
    fun `perangkat tanpa pengenalan suara langsung membuka mode teks`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = viewModel(speechAvailable = false)
            advanceUntilIdle()

            assertTrue(vm.uiState.value.isSpeechUnavailable)
            // Kunci: tidak boleh buntu. Mode teks terbuka tanpa pernah meminta
            // izin mikrofon untuk kemampuan yang memang tidak ada.
            assertTrue(vm.uiState.value.isTextMode)
        }

    @Test
    fun `startListening diabaikan saat pengenalan suara tidak tersedia`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = viewModel(speechAvailable = false)
            advanceUntilIdle()

            vm.startListening()
            advanceUntilIdle()

            assertEquals(VoicePhase.IDLE, vm.uiState.value.phase)
        }

    @Test
    fun `hasil sementara memperbarui transkrip lalu digantikan hasil akhir`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = viewModel(
                events = flowOf(
                    SpeechEvent.Ready,
                    SpeechEvent.PartialResult("kapan waktu"),
                    SpeechEvent.FinalResult("kapan waktu memupuk padi"),
                ),
            )
            advanceUntilIdle()

            vm.startListening()
            advanceUntilIdle()

            assertEquals(VoicePhase.RESULT, vm.uiState.value.phase)
            assertEquals("kapan waktu memupuk padi", vm.uiState.value.transcript)
        }

    @Test
    fun `hasil akhir dibacakan kembali sebagai bukti loop suara`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = viewModel(events = flowOf(SpeechEvent.FinalResult("halo")))
            advanceUntilIdle()

            vm.startListening()
            advanceUntilIdle()

            coVerify(exactly = 1) { speakText("halo") }
        }

    @Test
    fun `level suara diteruskan untuk animasi`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = viewModel(events = flowOf(SpeechEvent.Ready, SpeechEvent.RmsChanged(0.75f)))
            advanceUntilIdle()

            vm.startListening()
            advanceUntilIdle()

            assertEquals(0.75f, vm.uiState.value.soundLevel, 0.001f)
        }

    @Test
    fun `kegagalan yang bisa diulang menampilkan pesan tanpa memaksa mode teks`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = viewModel(
                events = flowOf(
                    SpeechEvent.Failed("Ucapan belum dikenali.", isRecoverable = true),
                ),
            )
            advanceUntilIdle()

            vm.startListening()
            advanceUntilIdle()

            assertEquals("Ucapan belum dikenali.", vm.uiState.value.errorMessage)
            assertTrue(vm.uiState.value.isRetryable)
            assertFalse(vm.uiState.value.isTextMode)
            assertEquals(VoicePhase.IDLE, vm.uiState.value.phase)
        }

    @Test
    fun `kegagalan yang tidak bisa diulang membuka mode teks agar tidak buntu`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = viewModel(
                events = flowOf(
                    SpeechEvent.Failed("Bahasa belum tersedia.", isRecoverable = false),
                ),
            )
            advanceUntilIdle()

            vm.startListening()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isRetryable)
            // Inti mitigasi risiko PRD: mengulang tidak akan menolong, jadi
            // pengguna harus punya jalan lain.
            assertTrue(vm.uiState.value.isTextMode)
        }

    @Test
    fun `teks yang diketik diperlakukan setara hasil suara`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = viewModel(speechAvailable = false)
            advanceUntilIdle()

            vm.submitTypedText("  kapan musim tanam  ")
            advanceUntilIdle()

            assertEquals("kapan musim tanam", vm.uiState.value.transcript)
            assertEquals(VoicePhase.RESULT, vm.uiState.value.phase)
            coVerify { speakText("kapan musim tanam") }
        }

    @Test
    fun `teks kosong diabaikan`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        val vm = viewModel(speechAvailable = false)
        advanceUntilIdle()

        vm.submitTypedText("   ")
        advanceUntilIdle()

        assertEquals("", vm.uiState.value.transcript)
        coVerify(exactly = 0) { speakText(any()) }
    }

    @Test
    fun `TTS gagal tidak menghapus transkrip`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = viewModel(
                ttsStatus = TtsStatus.LANGUAGE_MISSING_DATA,
                events = flowOf(SpeechEvent.FinalResult("padi")),
            )
            advanceUntilIdle()

            vm.startListening()
            advanceUntilIdle()

            // Petani tetap bisa MEMBACA hasilnya walau tidak bisa mendengarnya.
            assertEquals("padi", vm.uiState.value.transcript)
            assertEquals(VoicePhase.RESULT, vm.uiState.value.phase)
            assertFalse(vm.uiState.value.canSpeak)
        }

    @Test
    fun `speakText yang melempar tidak merobohkan layar`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            coEvery { speakText(any()) } throws IllegalStateException("engine mati")

            val vm = viewModel(events = flowOf(SpeechEvent.FinalResult("jagung")))
            advanceUntilIdle()

            vm.startListening()
            advanceUntilIdle()

            assertEquals("jagung", vm.uiState.value.transcript)
            assertEquals(VoicePhase.RESULT, vm.uiState.value.phase)
        }

    @Test
    fun `penyiapan TTS yang melempar menjadi status engine tidak tersedia`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            every { isAvailable() } returns true
            every { listenForSpeech(any()) } returns flowOf()
            coEvery { prepareTextToSpeech() } throws IllegalStateException("boom")

            val vm = VoiceViewModel(
                listenForSpeech = listenForSpeech,
                speakText = speakText,
                prepareTextToSpeech = prepareTextToSpeech,
                stopSpeaking = stopSpeaking,
                isSpeechRecognitionAvailable = isAvailable,
            )
            advanceUntilIdle()

            assertEquals(TtsStatus.ENGINE_UNAVAILABLE, vm.uiState.value.ttsStatus)
            assertFalse(vm.uiState.value.canSpeak)
        }

    @Test
    fun `izin ditolak membuka mode teks dan menandai tidak bisa diulang`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = viewModel()
            advanceUntilIdle()

            vm.onPermissionDenied("Izin mikrofon belum diberikan.")
            advanceUntilIdle()

            assertEquals("Izin mikrofon belum diberikan.", vm.uiState.value.errorMessage)
            assertTrue(vm.uiState.value.isTextMode)
            assertFalse(vm.uiState.value.isRetryable)
        }

    @Test
    fun `keluar dari mode teks ditolak bila perangkat tidak punya pengenalan suara`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = viewModel(speechAvailable = false)
            advanceUntilIdle()

            vm.disableTextMode()

            // Kalau ini boleh, layar akan menampilkan tombol mic yang tidak
            // akan pernah berfungsi.
            assertTrue(vm.uiState.value.isTextMode)
        }

    @Test
    fun `replay membacakan ulang hasil terakhir`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = viewModel(events = flowOf(SpeechEvent.FinalResult("cuaca besok")))
            advanceUntilIdle()

            vm.startListening()
            advanceUntilIdle()
            vm.replay()
            advanceUntilIdle()

            coVerify(exactly = 2) { speakText("cuaca besok") }
        }

    @Test
    fun `replay tanpa transkrip tidak melakukan apa pun`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = viewModel()
            advanceUntilIdle()

            vm.replay()
            advanceUntilIdle()

            coVerify(exactly = 0) { speakText(any()) }
        }

    @Test
    fun `onErrorShown menghapus pesan agar tidak muncul dua kali`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = viewModel(
                events = flowOf(SpeechEvent.Failed("gagal", isRecoverable = true)),
            )
            advanceUntilIdle()

            vm.startListening()
            advanceUntilIdle()
            vm.onErrorShown()

            assertNull(vm.uiState.value.errorMessage)
        }

    @Test
    fun `stopListening mengembalikan layar ke keadaan siap`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            val vm = viewModel(events = flowOf(SpeechEvent.Ready, SpeechEvent.RmsChanged(0.9f)))
            advanceUntilIdle()

            vm.startListening()
            advanceUntilIdle()
            vm.stopListening()

            assertEquals(VoicePhase.IDLE, vm.uiState.value.phase)
            assertEquals(0f, vm.uiState.value.soundLevel, 0.001f)
        }
}
