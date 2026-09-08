package com.agribicara.app.data.ai

import android.content.Context
import com.agribicara.app.R
import com.agribicara.app.core.common.Constants
import com.agribicara.app.core.common.DispatcherProvider
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.di.IsDebugBuild
import com.agribicara.app.domain.repository.AiRepository
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
 * Kelas ini memegang kebijakannya — berapa kali mengulang, kapan berhenti,
 * pesan mana yang dibaca petani — sedangkan panggilan SDK-nya sendiri ada di
 * [FirebaseTextGenerator] di balik [AiTextGenerator]. Pemisahan itu dibuat
 * supaya bagian yang salahnya paling terasa bisa diuji tanpa FirebaseApp.
 *
 * Mengikuti gaya [com.agribicara.app.data.repository.WeatherRepositoryImpl]:
 * `withContext(dispatchers.io)`, pesan pengguna dari `context.getString`, dan
 * [CancellationException] SELALU dilempar ulang agar structured concurrency
 * tidak rusak diam-diam.
 */
@Singleton
class FirebaseAiRepository @Inject constructor(
    private val generator: AiTextGenerator,
    private val dispatchers: DispatcherProvider,
    @ApplicationContext private val context: Context,
    @IsDebugBuild private val isDebugBuild: Boolean,
) : AiRepository {

    override suspend fun ask(prompt: String): NetworkResult<String> =
        withContext(dispatchers.io) {
            var lastFailure: Throwable? = null

            // Satu kali coba ulang: kegagalan sesaat (jaringan goyah, TLS
            // handshake gagal) sangat umum di sinyal desa. Lebih dari sekali
            // hanya menambah waktu tunggu di layar tanpa menaikkan peluang.
            repeat(Constants.AI_RETRY_COUNT + 1) { attempt ->
                try {
                    val text = generator.generate(prompt)?.trim()

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
     * Tiga jenis kegagalan dibedakan karena menuntut tindakan berbeda.
     *
     * Bagi petani, "tidak ada internet", "sambungan lambat", dan "layanan
     * bermasalah" bukan hal yang sama: yang pertama bisa ia perbaiki sendiri,
     * yang kedua layak dicoba lagi sekarang, yang ketiga hanya bisa ditunggu.
     * Menyamakan ketiganya membuat pesan jadi tidak berguna — pelajaran yang
     * sama dengan SpeechErrorMapper di Fase 3, yang juga memisahkan timeout
     * dari tidak-tersambung.
     *
     * Penolakan App Check adalah PENGECUALIAN yang hanya berlaku di build
     * debug. Bagi petani ia memang "layanan bermasalah" dan tidak boleh
     * berbunyi lain: ia tidak punya Firebase Console, tidak tahu apa itu
     * token, dan menyebutnya hanya membuat pesan menakutkan tanpa memberi
     * jalan keluar. Bagi developer ia justru satu-satunya kegagalan di sini
     * yang penyebabnya SETELAN, bukan kode — dan menyamarkannya sebagai
     * gangguan server sudah pernah menghabiskan satu sesi penuh perburuan
     * pada fitur yang sebenarnya utuh.
     */
    private fun messageFor(failure: Throwable?): String {
        // .take() WAJIB sebelum .toList(). Sesudahnya, toList() sudah berputar
        // selamanya lebih dulu pada rantai siklik — dan kedua bentuknya nyaris
        // tidak terbedakan saat dibaca sekilas.
        val chain = generateSequence(failure) { it.cause }
            .take(Constants.AI_CAUSE_CHAIN_LIMIT)
            .toList()
        val messageRes = when {
            chain.any { it is AiTimeoutException } -> R.string.error_ai_timeout
            // Sebelum cabang IOException: penolakan App Check bisa datang
            // terbungkus kegagalan jaringan saat token gagal ditukar, dan
            // "tidak ada internet" adalah diagnosis yang menyesatkan.
            isDebugBuild && chain.any { it is AiAppCheckException } ->
                R.string.error_ai_app_check_debug
            chain.any { it is IOException } -> R.string.error_ai_offline
            else -> R.string.error_ai_unavailable
        }
        return context.getString(messageRes)
    }
}
