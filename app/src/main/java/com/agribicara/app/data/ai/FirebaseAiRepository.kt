package com.agribicara.app.data.ai

import android.content.Context
import com.agribicara.app.R
import com.agribicara.app.core.common.Constants
import com.agribicara.app.core.common.DispatcherProvider
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.domain.repository.AiRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Gemini lewat Firebase AI Logic.
 *
 * Firebase dipakai, BUKAN REST langsung ke Gemini: kunci API tidak bisa
 * disembunyikan di dalam APK, dan pola itu sedang dieksploitasi di lapangan.
 * Dengan jalur ini kunci tetap di server Firebase dan akses dijaga App Check.
 *
 * Mengikuti gaya [com.agribicara.app.data.repository.WeatherRepositoryImpl]:
 * `withContext(dispatchers.io)`, pesan pengguna dari `context.getString`, dan
 * [CancellationException] SELALU dilempar ulang agar structured concurrency
 * tidak rusak diam-diam.
 */
@Singleton
class FirebaseAiRepository @Inject constructor(
    private val dispatchers: DispatcherProvider,
    @ApplicationContext private val context: Context,
) : AiRepository {

    /**
     * Dibuat malas, bukan di constructor.
     *
     * Constructor repository ini dipanggil Hilt saat graf dibangun. Bila
     * Firebase belum siap pada saat itu, kegagalannya akan muncul sebagai
     * crash saat injeksi — jauh dari penyebab sebenarnya. Dengan `lazy`,
     * kegagalan inisialisasi terjadi di dalam blok try [ask] dan berubah
     * menjadi pesan yang bisa dibaca petani.
     */
    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(Constants.GEMINI_MODEL)
    }

    override suspend fun ask(prompt: String): NetworkResult<String> =
        withContext(dispatchers.io) {
            var lastFailure: Throwable? = null

            // Satu kali coba ulang: kegagalan sesaat (jaringan goyah, TLS
            // handshake gagal) sangat umum di sinyal desa. Lebih dari sekali
            // hanya menambah waktu tunggu di layar tanpa menaikkan peluang.
            repeat(Constants.AI_RETRY_COUNT + 1) { attempt ->
                try {
                    val text = model.generateContent(prompt).text?.trim()

                    if (!text.isNullOrEmpty()) {
                        return@withContext NetworkResult.Success(text)
                    }

                    // text null/kosong berarti jawaban diblokir filter keamanan
                    // atau model tidak menghasilkan apa pun. Mengulang tidak
                    // akan menolong, jadi berhenti di sini — dan JANGAN
                    // mengembalikan Success bernilai kosong, yang akan tampil
                    // sebagai layar jawaban kosong tanpa penjelasan.
                    Timber.w("Gemini mengembalikan jawaban kosong")
                    return@withContext NetworkResult.Error(
                        context.getString(R.string.error_ai_blocked),
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastFailure = e
                    Timber.w(e, "Panggilan Gemini gagal (percobaan %d)", attempt + 1)
                }
            }

            NetworkResult.Error(messageFor(lastFailure), lastFailure)
        }

    /**
     * Kegagalan jaringan dibedakan dari kegagalan lain.
     *
     * Bagi petani, "tidak ada internet" dan "layanan bermasalah" menuntut
     * tindakan berbeda: yang pertama bisa ia perbaiki sendiri, yang kedua
     * hanya bisa ditunggu. Menyamakan keduanya membuat pesan jadi tidak
     * berguna — pelajaran yang sama dengan SpeechErrorMapper di Fase 3.
     */
    private fun messageFor(failure: Throwable?): String {
        val isNetwork = generateSequence(failure) { it.cause }.any { it is IOException }
        return if (isNetwork) {
            context.getString(R.string.error_ai_offline)
        } else {
            context.getString(R.string.error_ai_unavailable)
        }
    }
}
