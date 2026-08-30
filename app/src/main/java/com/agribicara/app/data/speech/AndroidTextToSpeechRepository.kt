package com.agribicara.app.data.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import com.agribicara.app.core.common.Constants
import com.agribicara.app.core.common.DispatcherProvider
import com.agribicara.app.domain.model.TtsStatus
import com.agribicara.app.domain.repository.TextToSpeechRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Implementasi [TextToSpeechRepository] di atas [TextToSpeech] milik Android.
 *
 * Tiga hal yang menentukan bentuk kelas ini:
 *
 * 1. `onInit` bersifat asinkron dan pada sebagian perangkat TIDAK PERNAH
 *    dipanggil bila engine rusak. Karena itu penantiannya dibatasi
 *    [Constants.TTS_INIT_TIMEOUT_MS] — tanpa itu layar menggantung selamanya.
 * 2. `setLanguage` mengembalikan kode NEGATIF bila bahasa tak terpakai
 *    (LANG_MISSING_DATA = -1, LANG_NOT_SUPPORTED = -2). Nilai ini harus
 *    diperiksa; banyak HP tidak punya suara Indonesia terpasang.
 * 3. TTS yang gagal tidak boleh mematikan layar suara — petani masih bisa
 *    membaca teksnya. Semua kegagalan berujung status, bukan exception.
 */
@Singleton
class AndroidTextToSpeechRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : TextToSpeechRepository {

    private val initMutex = Mutex()

    private var engine: TextToSpeech? = null
    private var status: TtsStatus? = null

    override suspend fun prepare(): TtsStatus = initMutex.withLock {
        // Sudah pernah disiapkan: jangan menyalakan engine kedua.
        status?.let { return@withLock it }

        val resolved = withContext(dispatchers.main) { initEngine() }
        status = resolved
        resolved
    }

    private suspend fun initEngine(): TtsStatus {
        val ready = CompletableDeferred<Int>()
        val tts = try {
            TextToSpeech(context) { code ->
                // Dipanggil sekali; complete() kedua kali diabaikan dengan aman.
                ready.complete(code)
            }
        } catch (e: Exception) {
            Timber.w(e, "TextToSpeech gagal dibuat")
            return TtsStatus.ENGINE_UNAVAILABLE
        }

        val initCode = withTimeoutOrNull(Constants.TTS_INIT_TIMEOUT_MS) { ready.await() }

        if (initCode == null) {
            // Engine tidak pernah menjawab. Lepaskan supaya tidak menggantung.
            Timber.w("Inisialisasi TextToSpeech melewati batas waktu")
            runCatching { tts.shutdown() }
            return TtsStatus.ENGINE_UNAVAILABLE
        }

        if (initCode != TextToSpeech.SUCCESS) {
            Timber.w("Inisialisasi TextToSpeech gagal, kode=%d", initCode)
            runCatching { tts.shutdown() }
            return TtsStatus.ENGINE_UNAVAILABLE
        }

        // forLanguageTag memakai tag BCP-47 yang sama persis dengan yang
        // dikirim ke STT, jadi mustahil keduanya melenceng. Konstruktor
        // Locale(bahasa, negara) sudah deprecated.
        val locale = Locale.forLanguageTag(Constants.SPEECH_LANGUAGE_TAG)
        val languageResult = runCatching { tts.setLanguage(locale) }
            .getOrElse { TextToSpeech.LANG_NOT_SUPPORTED }

        val languageStatus = languageStatusOf(languageResult)
        if (languageStatus != TtsStatus.READY) {
            Timber.w("Bahasa Indonesia tidak terpakai di TTS, kode=%d", languageResult)
            // Engine tetap disimpan: statusnya sudah memberi tahu UI untuk
            // tidak memanggil speak(), dan menahan instance ini mencegah
            // percobaan init berulang yang pasti gagal lagi.
        }

        engine = tts
        return languageStatus
    }

    override suspend fun speak(text: String) {
        if (text.isBlank()) return

        // prepare() aman dipanggil berkali-kali dan mengembalikan status yang
        // sudah dihitung, jadi speak() tidak pernah bergantung pada urutan
        // pemanggilan dari UI.
        if (prepare() != TtsStatus.READY) return

        val tts = engine ?: return
        withContext(dispatchers.main) {
            runCatching {
                // QUEUE_FLUSH, bukan QUEUE_ADD: pertanyaan terbaru selalu yang
                // paling relevan, antrean ucapan lama hanya membingungkan.
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
            }.onFailure { Timber.w(it, "speak gagal") }
        }
    }

    override fun stop() {
        runCatching { engine?.stop() }
            .onFailure { Timber.w(it, "stop TTS gagal") }
    }

    override fun shutdown() {
        runCatching { engine?.shutdown() }
            .onFailure { Timber.w(it, "shutdown TTS gagal") }
        engine = null
        status = null
    }

    companion object {

        private const val UTTERANCE_ID = "agribicara-jawaban"

        /**
         * Menerjemahkan kode balikan `setLanguage`.
         *
         * Nilai NEGATIF berarti bahasa tidak bisa dipakai; nol dan positif
         * (LANG_AVAILABLE / LANG_COUNTRY_AVAILABLE / LANG_COUNTRY_VAR_AVAILABLE)
         * berarti siap. Dibuat internal agar bisa diuji langsung tanpa
         * menyalakan engine sungguhan.
         */
        internal fun languageStatusOf(languageResult: Int): TtsStatus = when (languageResult) {
            TextToSpeech.LANG_MISSING_DATA -> TtsStatus.LANGUAGE_MISSING_DATA
            TextToSpeech.LANG_NOT_SUPPORTED -> TtsStatus.LANGUAGE_NOT_SUPPORTED
            else -> if (languageResult >= TextToSpeech.LANG_AVAILABLE) {
                TtsStatus.READY
            } else {
                // Kode negatif lain yang tidak terdokumentasi: perlakukan
                // sebagai tidak didukung, jangan diasumsikan aman.
                TtsStatus.LANGUAGE_NOT_SUPPORTED
            }
        }
    }
}
