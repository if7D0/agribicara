package com.agribicara.app.data.ai

import com.agribicara.app.core.common.Constants
import com.google.firebase.ai.type.RequestOptions

/**
 * Opsi request untuk model Gemini, dipisahkan agar bisa diuji.
 *
 * Ada karena satu cacat yang pernah nyata: SDK Firebase memakai timeout bawaan
 * **180 detik** bila `requestOptions` tidak diberikan. Petani di desa dengan
 * sinyal buruk akan menatap layar menunggu selama tiga menit sebelum tahu
 * panggilannya gagal.
 *
 * Perbaikan itu sebelumnya hanya pernah dibuktikan dengan membaca disassembly
 * kelas rilis secara manual — sekali, oleh satu orang, dan tidak berulang.
 * Menghapus `requestOptions` dari [FirebaseTextGenerator] akan tetap compile,
 * tetap lolos lint, dan tetap lolos seluruh test. Fungsi ini memindahkan nilai
 * itu ke tempat yang bisa dipanggil test, sehingga penghapusannya memerahkan
 * sesuatu.
 *
 * `RequestOptions(timeoutInMillis = ...)` adalah konstruktor PUBLIK. Varian
 * `RequestOptions(timeout = ...)` yang menerima `Duration` bersifat `internal`
 * dan tidak akan compile dari modul ini — itu sudah pernah menggagalkan satu
 * percobaan perbaikan.
 */
internal fun aiRequestOptions(): RequestOptions =
    RequestOptions(timeoutInMillis = Constants.AI_TIMEOUT_MS)
