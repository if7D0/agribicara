package com.agribicara.app.presentation.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.domain.model.AnswerSource
import com.agribicara.app.domain.model.SpeechEvent
import com.agribicara.app.domain.model.TtsStatus
import com.agribicara.app.domain.usecase.AskAgriUseCase
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

    /**
     * Pertanyaan sudah lengkap, jawaban sedang dicari (Fase 5).
     *
     * Fase tersendiri, bukan menumpang PROCESSING: menunggu Gemini bisa
     * memakan beberapa detik, dan tanpa keadaan yang terlihat berbeda petani
     * mengira aplikasi hang lalu menekan tombol berulang.
     */
    THINKING,

    /** Ada hasil akhir. */
    RESULT,
}

data class VoiceUiState(
    val phase: VoicePhase = VoicePhase.IDLE,
    /** Pertanyaan petani: hasil sementara saat mendengarkan, final setelahnya. */
    val transcript: String = "",
    /** Jawaban AI. Kosong sampai ada jawaban. */
    val answer: String = "",
    /** True bila [answer] diambil dari simpanan, bukan hasil tanya barusan. */
    val isAnswerFromCache: Boolean = false,
    /** 0f..1f untuk animasi pulsa mikrofon. */
    val soundLevel: Float = 0f,
    val errorMessage: String? = null,
    /**
     * Kegagalan mendapatkan JAWABAN, ditampilkan permanen di layar.
     *
     * Dipisah dari [errorMessage] yang tampil sebagai snackbar lalu hilang:
     * kegagalan suara bersifat sesaat dan cukup diberitahukan, sedangkan
     * kegagalan jawaban menuntut petani memutuskan sesuatu — mengulang, atau
     * melihat data cuaca yang tetap berguna tanpa internet. Pesan yang
     * menghilang sendiri tidak bisa menawarkan pilihan itu.
     */
    val answerError: String? = null,
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

    /** Ada sesuatu untuk dibacakan ulang, dan perangkat mampu membacakannya. */
    val canReplay: Boolean get() = canSpeak && answer.isNotBlank()

    val isListening: Boolean
        get() = phase == VoicePhase.PREPARING || phase == VoicePhase.LISTENING

    val isThinking: Boolean get() = phase == VoicePhase.THINKING
}

/**
 * Layar suara.
 *
 * Fase 3 membuktikan LOOP-nya dengan membacakan kembali apa yang didengar.
 * Fase 5 mengganti gema itu dengan jawaban Gemini: transkrip menjadi
 * pertanyaan, jawaban AI yang dibacakan. Sisa layar tidak berubah, persis
 * seperti yang direncanakan saat [speakBack] sengaja dipisah di Fase 3.
 */
@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val listenForSpeech: ListenForSpeechUseCase,
    private val speakText: SpeakTextUseCase,
    private val prepareTextToSpeech: PrepareTextToSpeechUseCase,
    private val stopSpeaking: StopSpeakingUseCase,
    private val isSpeechRecognitionAvailable: IsSpeechRecognitionAvailableUseCase,
    private val askAgri: AskAgriUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private var listenJob: Job? = null
    private var askJob: Job? = null

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

        // TTS disiapkan lebih awal supaya jawaban pertama tidak tertunda oleh
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
        // Permintaan AI yang masih berjalan juga dibatalkan: jawabannya milik
        // pertanyaan lama, dan bila dibiarkan ia akan mendarat di layar
        // setelah petani mengajukan pertanyaan baru.
        askJob?.cancel()

        _uiState.update {
            it.copy(
                phase = VoicePhase.PREPARING,
                transcript = "",
                answer = "",
                answerError = null,
                isAnswerFromCache = false,
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
                    transcript = event.text,
                    soundLevel = 0f,
                    errorMessage = null,
                )
            }
            ask(event.text)
        }

        is SpeechEvent.Failed ->
            _uiState.update {
                it.copy(
                    // Kembali ke IDLE, bukan RESULT: tidak ada pertanyaan untuk
                    // dijawab, dan tombol harus siap ditekan lagi.
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
                transcript = trimmed,
                errorMessage = null,
                soundLevel = 0f,
            )
        }
        ask(trimmed)
    }

    /**
     * Menanyakan ke AI lalu membacakan jawabannya.
     *
     * Aturan yang dipegang dari Fase 3 dan tetap berlaku di sini: kegagalan
     * TIDAK PERNAH menghapus pertanyaan petani. Ia sudah bersusah payah
     * mengucapkannya; memaksa mengulang karena jaringan putus adalah hukuman
     * atas kesalahan yang bukan miliknya.
     */
    private fun ask(question: String) {
        askJob?.cancel()
        askJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    phase = VoicePhase.THINKING,
                    answer = "",
                    answerError = null,
                    isAnswerFromCache = false,
                    errorMessage = null,
                )
            }

            when (val result = askAgri(question)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            phase = VoicePhase.RESULT,
                            answer = result.data.text,
                            isAnswerFromCache = result.data.source == AnswerSource.CACHE,
                        )
                    }
                    speak(result.data.text)
                }

                is NetworkResult.Error ->
                    _uiState.update {
                        it.copy(
                            // RESULT, bukan IDLE: pertanyaannya tetap harus
                            // terlihat di layar bersama pesan kegagalannya.
                            phase = VoicePhase.RESULT,
                            // answerError, BUKAN errorMessage: ini harus
                            // bertahan di layar bersama tawaran melihat cuaca,
                            // bukan lewat sebagai snackbar yang hilang sendiri.
                            answerError = result.message,
                            isRetryable = true,
                        )
                    }

                NetworkResult.Loading -> Unit
            }
        }
    }

    /** Membacakan teks. Kegagalannya adalah penurunan kualitas, bukan kegagalan. */
    private fun speak(text: String) {
        viewModelScope.launch {
            // Teks jawaban tetap terbaca di layar walau TTS gagal.
            runCatching { speakText(text) }
                .onFailure { Timber.w(it, "Pembacaan jawaban gagal") }
        }
    }

    /** Mengulang pembacaan JAWABAN terakhir, bukan pertanyaannya. */
    fun replay() {
        val text = _uiState.value.answer
        if (text.isNotBlank()) speak(text)
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
        // Melepaskan mikrofon, membatalkan permintaan AI yang menggantung, dan
        // menghentikan ucapan yang masih berjalan. Tanpa ini, suara terus
        // terdengar setelah layar ditutup.
        listenJob?.cancel()
        askJob?.cancel()
        runCatching { stopSpeaking() }
            .onFailure { Timber.w(it, "Gagal menghentikan TTS") }
    }
}
