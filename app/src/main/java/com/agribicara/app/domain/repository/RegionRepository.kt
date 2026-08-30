package com.agribicara.app.domain.repository

import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.domain.model.Region
import com.agribicara.app.domain.model.RegionLevel
import kotlinx.coroutines.flow.Flow

/**
 * Daftar wilayah administratif dan wilayah yang sedang dipilih pengguna.
 */
interface RegionRepository {

    /**
     * Anak-anak dari [parentCode] pada [level].
     *
     * [parentCode] null hanya sah untuk [RegionLevel.PROVINCE]. Cache Room
     * dibaca lebih dulu supaya picker tetap bisa dipakai tanpa internet
     * setelah tingkat itu pernah dibuka sekali.
     *
     * [forceRefresh] melewati cache dan mengambil ulang dari jaringan. Wajib
     * ada karena response yang terpotong (mis. 2 dari 40 kelurahan) tetap
     * tersimpan sebagai cache yang sah; tanpa jalan paksa, satu-satunya cara
     * pengguna keluar dari daftar yang cacat adalah menghapus data aplikasi.
     */
    suspend fun getRegions(
        level: RegionLevel,
        parentCode: String?,
        forceRefresh: Boolean = false,
    ): NetworkResult<List<Region>>

    /** Wilayah pilihan pengguna, atau null bila belum memilih. */
    fun observeSelectedRegion(): Flow<Region?>

    suspend fun saveSelectedRegion(region: Region)
}
