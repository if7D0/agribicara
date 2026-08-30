package com.agribicara.app.presentation.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agribicara.app.domain.model.SpeechEvent
import com.agribicara.app.domain.model.TtsStatus
import com.agribicara.app.domain.usecase.IsSpeechRecognitionAvailableUseCase
import com.agribicara.app.domain.usecase.ListenForSpeechUseCase
import com.agribicara.app.domain.usecase.PrepareTextToSpeechUseCase
import com.agribicara.app.domain.usecase.SpeakTextUseCase
import com.agribicara.app.domain.usecase.StopSpeakingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Tahapan sesi suara.
 *
 * Dipisah dari pesan error supaya UI bisa menampilkan error TANPA kehilangan
 * teks hasil sebelumnya — petani yang gagal di percobaan kedua tetap melihat
 * jawaban pertamanya.
 */
enum class VoicePhase {
    /** Belum mulai; tombol siap ditekan. */
    IDLE,

    /** Mikrofon sedang dibuka. */
    PREPARING,

    /** Mikrofon terbuka, menunggu/menerima ucapan. */
    LISTENING,

    /** Ucapan selesai, hasil sedang difinalkan. */
    PROCESSING,

    /** Ada hasil akhir. */
    RESULT,
}

data class VoiceUiState(
    val phase: VoicePhase = VoicePhase.IDLE,
    /** Teks berjalan: hasil sementara saat mendengarkan, hasil akhir setelahnya. */
    val transcript: String = "",
    /** 0f..1f untuk animasi pulsa mikrofon. */
    val soundLevel: Float = 0f,
    val errorMessage: String? = null,
    /** False bila mengulang pasti gagal lagi (izin belum ada, bahasa tak ada). */
    val isRetryable: Boolean = true,
    /**
     * True bila perangkat tidak punya pengenalan suara sama sekali.
     * Layar langsung membuka jalur teks dan TIDAK meminta izin mikrofon.
     */
    val isSpeechUnavailable: Boolean = false,
    /** True saat pengguna memilih mengetik, atau dipaksa karena STT tak ada. */
    val isTextMode: Boolean = false,
    val ttsStatus: TtsStatus? = null,
) {
    /** TTS berfungsi penuh; tombol "Dengarkan lagi" hanya masuk akal bila ini true. */
    val canSpeak: Boolean get() = ttsStatus == TtsStatus.READY

    val isListening: Boolean
        get() = phase == VoicePhase.PREPARING || phase == VoicePhase.LISTENING
}

/**
 * Layar suara Fase 3.
 *
 * Fase 3 membuktikan LOOP-nya: apa pun yang didengar dibacakan kembali. Fase 5
 * menyisipkan Gemini di antara keduanya — transkrip menjadi prompt, jawaban AI
 * yang dibacakan. Karena itu pembacaan sengaja lewat [speakBack] yang terpisah,
 * agar Fase 5 hanya perlu mengganti isinya.
 */
@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val listenForSpeech: ListenForSpeechUseCase,
    private val speakText: SpeakTextUseCase,
    private val prepareTextToSpeech: PrepareTextToSpeechUseCase,
    private val stopSpeaking: StopSpeakingUseCase,
    private val isSpeechRecognitionAvailable: IsSpeechRecognitionAvailableUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private var listenJob: Job? = null

    init {
        val available = isSpeechRecognitionAvailable()
        _uiState.update {
            it.copy(
                isSpeechUnavailable = !available,
                // Tanpa pengenalan suara, satu-satunya jalan adalah mengetik.
                // Membuka mode teks sejak awal mencegah layar buntu.
                isTextMode = !available,
            )
        }

        // TTS disiapkan lebih awal supaya hasil pertama tidak tertunda oleh
        // inisialisasi engine yang bisa memakan detik.
        viewModelScope.launch {
            val status = runCatching { prepareTextToSpeech() }
                .getOrElse {
                    Timber.w(it, "Penyiapan TTS gagal")
                    TtsStatus.ENGINE_UNAVAILABLE
                }
            _uiState.update { it.copy(ttsStatus = status) }
        }
    }

    /**
     * Memulai sesi mendengarkan.
     *
     * Pemanggil WAJIB memastikan izin RECORD_AUDIO sudah diberikan; tanpa itu
     * recognizer hanya mengembalikan ERROR_INSUFFICIENT_PERMISSIONS.
     */
    fun startListening() {
        if (_uiState.value.isSpeechUnavailable) return

        // Sesi lama dibatalkan lebih dulu. Tanpa ini, tap ganda menyisakan dua
        // recognizer hidup dan yang kedua gagal dengan ERROR_RECOGNIZER_BUSY.
        listenJob?.cancel()

        _uiState.update {
            it.copy(
                phase = VoicePhase.PREPARING,
                transcript = "",
                errorMessage = null,
                soundLevel = 0f,
            )
        }

        listenJob = viewModelScope.launch {
            listenForSpeech().collect { event -> onSpeechEvent(event) }
        }
    }

    private fun onSpeechEvent(event: SpeechEvent) = when (event) {
        SpeechEvent.Ready ->
            _uiState.update { it.copy(phase = VoicePhase.LISTENING) }

        SpeechEvent.BeginningOfSpeech ->
            _uiState.update { it.copy(phase = VoicePhase.LISTENING) }

        is SpeechEvent.RmsChanged ->
            _uiState.update { it.copy(soundLevel = event.level) }

        is SpeechEvent.PartialResult ->
            _uiState.update { it.copy(phase = VoicePhase.LISTENING, transcript = event.text) }

        is SpeechEvent.FinalResult -> {
            _uiState.update {
                it.copy(
                    phase = VoicePhase.RESULT,
                    transcript = event.text,
                    soundLevel = 0f,
                    errorMessage = null,
                )
            }
            speakBack(event.text)
        }

        is SpeechEvent.Failed ->
            _uiState.update {
                it.copy(
                    // Kembali ke IDLE, bukan RESULT: tidak ada hasil untuk
                    // dibacakan, dan tombol harus siap ditekan lagi.
                    phase = VoicePhase.IDLE,
                    soundLevel = 0f,
                    errorMessage = event.message,
                    isRetryable = event.isRecoverable,
                    // Bila mengulang tidak akan menolong, buka jalur teks
                    // supaya petani tidak menemui jalan buntu.
                    isTextMode = if (event.isRecoverable) it.isTextMode else true,
                )
            }
    }

    /** Mengirim pertanyaan yang diketik, jalur setara hasil suara. */
    fun submitTypedText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        listenJob?.cancel()
        _uiState.update {
            it.copy(
                phase = VoicePhase.RESULT,
                transcript = trimmed,
                errorMessage = null,
                soundLevel = 0f,
            )
        }
        speakBack(trimmed)
    }

    /**
     * Membacakan kembali teks.
     *
     * Fase 3: gemanya adalah buktinya - apa yang didengar dibacakan ulang.
     * Fase 5: ganti isinya dengan jawaban Gemini, sisa layar tidak berubah.
     */
    private fun speakBack(text: String) {
        viewModelScope.launch {
            // Kegagalan TTS TIDAK pernah menghapus transkrip: teksnya tetap
            // terbaca di layar, jadi ini penurunan kualitas, bukan kegagalan.
            runCatching { speakText(text) }
                .onFailure { Timber.w(it, "Pembacaan teks gagal") }
        }
    }

    /** Mengulang pembacaan hasil terakhir. */
    fun replay() {
        val text = _uiState.value.transcript
        if (text.isNotBlank()) speakBack(text)
    }

    fun enableTextMode() {
        listenJob?.cancel()
        _uiState.update {
            it.copy(isTextMode = true, phase = VoicePhase.IDLE, soundLevel = 0f)
        }
    }

    fun disableTextMode() {
        // Perangkat tanpa pengenalan suara tidak boleh keluar dari mode teks.
        if (_uiState.value.isSpeechUnavailable) return
        _uiState.update { it.copy(isTextMode = false, errorMessage = null) }
    }

    /** Dipanggil setelah pesan error ditampilkan, agar tidak muncul dua kali. */
    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Izin ditolak: sampaikan alasannya dan sediakan jalur teks. */
    fun onPermissionDenied(message: String) {
        _uiState.update {
            it.copy(
                phase = VoicePhase.IDLE,
                errorMessage = message,
                isRetryable = false,
                isTextMode = true,
                soundLevel = 0f,
            )
        }
    }

    fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        _uiState.update { it.copy(phase = VoicePhase.IDLE, soundLevel = 0f) }
    }

    override fun onCleared() {
        super.onCleared()
        // Melepaskan mikrofon dan menghentikan ucapan yang masih berjalan.
        // Tanpa ini, suara terus terdengar setelah layar ditutup.
        listenJob?.cancel()
        runCatching { stopSpeaking() }
            .onFailure { Timber.w(it, "Gagal menghentikan TTS") }
    }
}
