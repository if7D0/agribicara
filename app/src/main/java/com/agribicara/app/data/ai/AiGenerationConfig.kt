package com.agribicara.app.data.ai

import com.agribicara.app.core.common.Constants
import com.google.firebase.ai.type.GenerationConfig
import com.google.firebase.ai.type.generationConfig

/**
 * Konfigurasi generasi untuk model Gemini, dipisahkan agar bisa diuji.
 *
 * Bentuknya sengaja sama persis dengan [aiRequestOptions], termasuk `internal`,
 * dan ada karena alasan yang sejenis: sebuah pengaman yang bila terhapus tetap
 * compile, tetap lolos lint, dan tetap lolos seluruh test lain.
 *
 * Yang dijaga di sini adalah panjang jawaban. [Constants.AI_MAX_SENTENCES]
 * hanya DIMINTA lewat prompt, dan permintaan bisa diabaikan model. Ketika itu
 * terjadi, TTS membacakan jawaban panjang sampai habis tanpa tombol berhenti —
 * petani menunggu tanpa bisa menyela. `maxOutputTokens` memaksakannya di sisi
 * model, di mana prompt tidak lagi bisa dilanggar.
 *
 * `GenerationConfig(maxOutputTokens = ...)` **tidak bisa dipakai**:
 * konstruktornya `private`. Helper DSL `generationConfig { }` adalah satu-satunya
 * jalan dari modul ini — kendala yang sebentuk dengan varian `RequestOptions`
 * ber-`Duration` yang `internal` di [aiRequestOptions].
 */
internal fun aiGenerationConfig(): GenerationConfig = generationConfig {
    maxOutputTokens = Constants.AI_MAX_OUTPUT_TOKENS
}
