package com.agribicara.app.data.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.agribicara.app.R
import com.agribicara.app.core.common.Constants
import com.agribicara.app.core.common.DispatcherProvider
import com.agribicara.app.domain.model.SpeechEvent
import com.agribicara.app.domain.repository.SpeechRecognizerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber

/**
 * Implementasi [SpeechRecognizerRepository] di atas [SpeechRecognizer] milik Android.
 *
 * Dua aturan platform yang mengatur bentuk kelas ini, keduanya dibaca langsung
 * dari sumber Android SDK:
 *
 * 1. `createSpeechRecognizer` dan `startListening` ber-anotasi `@MainThread`.
 *    Karena itu seluruh producer di-`flowOn(dispatchers.main)` — memanggilnya
 *    dari `Dispatchers.IO` menghasilkan kegagalan diam yang membingungkan.
 * 2. Javadoc menyatakan pemanggil WAJIB memanggil `destroy()`. Itu dilakukan di
 *    `awaitClose`, sehingga membatalkan collect otomatis melepaskan mikrofon.
 *
 * Sesi berakhir setelah hasil akhir atau kegagalan; layar suara memulai sesi
 * baru untuk setiap pertanyaan.
 */
@Singleton
class AndroidSpeechRecognizerRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : SpeechRecognizerRepository {

    override fun isAvailable(): Boolean = try {
        SpeechRecognizer.isRecognitionAvailable(context)
    } catch (e: Exception) {
        // Beberapa ROM pihak ketiga melempar saat query layanan. Anggap tidak
        // tersedia dan biarkan pemanggil membuka jalur teks - kontraknya jelas:
        // repository tidak pernah melempar keluar.
        Timber.w(e, "Gagal memeriksa ketersediaan pengenalan suara")
        false
    }

    override fun listen(languageTag: String): Flow<SpeechEvent> = callbackFlow {
        if (!isAvailable()) {
            // Bukan error: emulator AOSP dan perangkat tanpa Google services
            // memang begini. Pemanggil akan membuka jalur teks.
            trySend(
                SpeechEvent.Failed(
                    message = context.getString(R.string.voice_error_unavailable),
                    isRecoverable = false,
                ),
            )
            close()
            return@callbackFlow
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        if (recognizer == null) {
            trySend(
                SpeechEvent.Failed(
                    message = context.getString(R.string.voice_error_unavailable),
                    isRecoverable = false,
                ),
            )
            close()
            return@callbackFlow
        }

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(SpeechEvent.Ready)
            }

            override fun onBeginningOfSpeech() {
                trySend(SpeechEvent.BeginningOfSpeech)
            }

            override fun onRmsChanged(rmsdB: Float) {
                trySend(SpeechEvent.RmsChanged(normalizeRms(rmsdB)))
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                val failure = SpeechErrorMapper.map(error)
                Timber.d("Pengenalan suara gagal, kode=%d", error)
                trySend(
                    SpeechEvent.Failed(
                        message = context.getString(failure.messageRes),
                        isRecoverable = failure.isRecoverable,
                    ),
                )
                // Satu sesi = satu hasil. Recognizer tidak akan mengirim apa pun
                // lagi setelah error, jadi menahan flow tetap terbuka hanya
                // membuat layar menggantung.
                close()
            }

            override fun onResults(results: Bundle?) {
                val text = results.firstTranscript()
                if (text.isNullOrBlank()) {
                    // Hasil kosong terjadi pada sebagian perangkat alih-alih
                    // ERROR_NO_MATCH; perlakukan sama supaya pengguna tetap
                    // mendapat pesan, bukan layar diam.
                    trySend(
                        SpeechEvent.Failed(
                            message = context.getString(R.string.voice_error_no_match),
                            isRecoverable = true,
                        ),
                    )
                } else {
                    trySend(SpeechEvent.FinalResult(text))
                }
                close()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults.firstTranscript()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { trySend(SpeechEvent.PartialResult(it)) }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }

        recognizer.setRecognitionListener(listener)

        try {
            recognizer.startListening(buildIntent(languageTag))
        } catch (e: Exception) {
            Timber.w(e, "startListening gagal")
            trySend(
                SpeechEvent.Failed(
                    message = context.getString(R.string.voice_error_generic),
                    isRecoverable = true,
                ),
            )
            close()
        }

        awaitClose {
            // WAJIB menurut javadoc SpeechRecognizer. Tanpa ini mikrofon tetap
            // tertahan dan sesi berikutnya gagal dengan ERROR_RECOGNIZER_BUSY.
            runCatching {
                recognizer.stopListening()
                recognizer.destroy()
            }.onFailure { Timber.w(it, "Gagal melepaskan SpeechRecognizer") }
        }
    }.flowOn(dispatchers.main)

    private fun buildIntent(languageTag: String): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, Constants.SPEECH_MAX_RESULTS)
            // Hasil sementara membuat layar terasa hidup selagi orang bicara;
            // tanpa ini layar diam sampai kalimat selesai dan terasa hang.
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Sebagian OEM menolak permintaan tanpa paket pemanggil.
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

    private companion object {

        /**
         * Android mendokumentasikan onRmsChanged sebagai "nilai RMS dalam dB"
         * tanpa menjamin rentangnya. Yang teramati di lapangan kira-kira
         * -2..10 dB, jadi itu yang dipakai lalu dijepit — animasi hanya butuh
         * proporsi kasar, bukan akurasi akustik.
         */
        const val RMS_MIN_DB = -2f
        const val RMS_MAX_DB = 10f

        fun normalizeRms(rmsdB: Float): Float =
            ((rmsdB - RMS_MIN_DB) / (RMS_MAX_DB - RMS_MIN_DB)).coerceIn(0f, 1f)

        fun Bundle?.firstTranscript(): String? = this
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
    }
}
