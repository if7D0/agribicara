package com.agribicara.app.data.ai

import com.agribicara.app.core.common.Constants
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
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
            )
    }

    override suspend fun generate(prompt: String): String? = try {
        model.generateContent(prompt).text
    } catch (e: RequestTimeoutException) {
        // Diterjemahkan di sini supaya pemetaan pesan tidak perlu mengenal
        // tipe SDK. Sengaja TIDAK ditelan: pemanggil yang memutuskan apakah
        // masih pantas diulang.
        throw AiTimeoutException(e)
    }
}
