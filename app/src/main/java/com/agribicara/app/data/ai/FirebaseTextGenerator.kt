package com.agribicara.app.data.ai

import com.agribicara.app.core.common.Constants
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FirebaseAIException
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.RequestTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Satu-satunya kode yang menyentuh SDK Firebase AI.
 *
 * Firebase dipakai, BUKAN REST langsung ke Gemini: kunci API tidak bisa
 * disembunyikan di dalam APK, dan pola itu sedang dieksploitasi di lapangan.
 * Lewat jalur ini kunci tetap di server Firebase dan aksesnya dijaga App Check.
 *
 * Kelas ini sengaja dijaga sedangkal mungkin — membangun model, memanggilnya,
 * menerjemahkan satu jenis kegagalan. Semua keputusan lain ada di
 * [FirebaseAiRepository] yang bisa diuji.
 *
 * CATATAN: jangan pakai paket `com.google.firebase.*.ktx.*`. Sejak BoM 34.x
 * isinya sudah dilebur ke paket utama dan tidak akan resolve, walaupun hampir
 * semua contoh kode Firebase di internet masih memakainya.
 */
@Singleton
class FirebaseTextGenerator @Inject constructor() : AiTextGenerator {

    /**
     * Dibuat malas, bukan di constructor.
     *
     * Constructor kelas ini dipanggil Hilt saat graf dibangun. Bila Firebase
     * belum siap pada saat itu, kegagalannya akan muncul sebagai crash saat
     * injeksi — jauh dari penyebab sebenarnya. Dengan `lazy`, kegagalan
     * inisialisasi terjadi di dalam panggilan [generate], tertangkap
     * [FirebaseAiRepository], dan berubah menjadi pesan yang bisa dibaca
     * petani.
     */
    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = Constants.GEMINI_MODEL,
                // Tanpa ini SDK memakai bawaannya, 180 detik. Lihat
                // Constants.AI_TIMEOUT_MS untuk alasan lengkapnya.
                requestOptions = aiRequestOptions(),
                // Batas 3 kalimat hanya DIMINTA lewat prompt dan bisa
                // diabaikan. Lihat Constants.AI_MAX_OUTPUT_TOKENS.
                generationConfig = aiGenerationConfig(),
            )
    }

    override suspend fun generate(prompt: String): String? = try {
        model.generateContent(prompt).text
    } catch (e: RequestTimeoutException) {
        // Diterjemahkan di sini supaya pemetaan pesan tidak perlu mengenal
        // tipe SDK. Sengaja TIDAK ditelan: pemanggil yang memutuskan apakah
        // masih pantas diulang.
        //
        // WAJIB sebelum FirebaseAIException: RequestTimeoutException adalah
        // turunannya, dan blok catch Kotlin diperiksa berurutan.
        throw AiTimeoutException(e)
    } catch (e: FirebaseAIException) {
        if (isAppCheckRejection(e)) throw AiAppCheckException(e)
        throw e
    }
}

/**
 * Menebak apakah [failure] adalah penolakan App Check, dari teks pesannya.
 *
 * RAPUH DAN MEMANG DISENGAJA. SDK 17.16.0 tidak punya tipe exception khusus
 * App Check — penolakannya datang sebagai `ServerException` biasa dengan pesan
 * "Firebase App Check token is invalid.", persis sama bentuknya dengan
 * kegagalan server lain. Teks itulah satu-satunya pembeda yang ada.
 *
 * Kalau pesan SDK berubah, kecocokan ini meleset dan perilakunya kembali PERSIS
 * seperti sebelum fungsi ini ada: pesan "layanan bermasalah" yang generik. Jadi
 * salah tebak menurunkan kualitas diagnosis, tidak pernah merusak apa pun — dan
 * itu syarat yang membuat pencocokan serapuh ini masih pantas dipakai.
 *
 * Diangkat keluar dari [FirebaseTextGenerator] supaya bisa diuji: kelas itu
 * menuntut FirebaseApp yang hidup, sedangkan fungsi ini hanya butuh sebuah
 * [Throwable]. Alasan yang sama dengan [AiTextGenerator] itu sendiri.
 *
 * Menerima [Throwable], bukan `FirebaseAIException`, justru supaya bisa diuji:
 * konstruktor `ServerException` bersifat `internal` di Kotlin sehingga tipe
 * aslinya TIDAK bisa dibuat dari test — kendala yang sama persis dengan
 * `RequestTimeoutException` dan alasan lahirnya [AiTimeoutException]. Tipe
 * exception memang tidak pernah ikut menentukan hasil di sini; yang dibaca
 * hanya rantai `message`. Penyempitan ke `FirebaseAIException` dilakukan di
 * tempat pemanggilan, bukan di dalam fungsi ini.
 */
internal fun isAppCheckRejection(failure: Throwable): Boolean =
    generateSequence(failure) { it.cause }
        .any { it.message?.contains("app check", ignoreCase = true) == true }
