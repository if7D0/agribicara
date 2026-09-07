package com.agribicara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Satu hasil deteksi penyakit tersimpan (Fase 4).
 *
 * Sama pola dengan [ChatMessageEntity]: id auto-generate karena deteksi yang
 * sama boleh berulang dan riwayat justru berguna bila entri lama tidak
 * tertimpa.
 *
 * [outcomeType] menyimpan nama [com.agribicara.app.domain.model.DetectionOutcomeType]
 * (DIAGNOSED/HEALTHY/UNSURE). [label] null untuk UNSURE — tidak ada label yang
 * cukup diyakini untuk disimpan.
 */
@Entity(tableName = "disease_detection")
data class DetectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val outcomeType: String,
    val label: String?,
    val confidence: Float,
    /** Epoch millis saat baris ditulis. */
    val createdAt: Long,
)
