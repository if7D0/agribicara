package com.agribicara.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Satu baris percakapan tersimpan (Fase 5).
 *
 * Berbeda dengan [WeatherCacheEntity] yang memakai primary key gabungan,
 * di sini id auto-generate: pertanyaan yang sama BOLEH muncul berkali-kali,
 * dan riwayat justru kehilangan gunanya bila entri lama tertimpa.
 *
 * [questionKey] diindeks karena setiap panggilan AI yang gagal akan mencarinya
 * untuk menemukan jawaban tersimpan — tanpa indeks, pencarian itu memindai
 * seluruh tabel tepat pada saat perangkat sedang paling lambat.
 */
@Entity(
    tableName = "chat_message",
    indices = [Index(value = ["questionKey"])],
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Nama [com.agribicara.app.domain.model.ChatRole]. */
    val role: String,
    val text: String,
    /** Pertanyaan yang sudah dinormalisasi; kunci pencarian cache jawaban. */
    val questionKey: String,
    /** Wilayah saat bertanya. Null bila petani melewati region picker. */
    val regionCode: String?,
    /** Epoch millis saat baris ditulis. */
    val createdAt: Long,
)
