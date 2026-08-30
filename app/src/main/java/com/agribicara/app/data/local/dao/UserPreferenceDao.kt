package com.agribicara.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.agribicara.app.core.common.Constants
import com.agribicara.app.data.local.entity.UserPreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferenceDao {

    @Query("SELECT * FROM user_preference WHERE id = :id LIMIT 1")
    fun observe(id: Int): Flow<UserPreferenceEntity?>

    @Query("SELECT * FROM user_preference WHERE id = :id LIMIT 1")
    suspend fun get(id: Int): UserPreferenceEntity?

    @Upsert
    suspend fun upsert(entity: UserPreferenceEntity)

    @Query("UPDATE user_preference SET isOnboardingCompleted = :completed WHERE id = :id")
    suspend fun updateOnboardingCompleted(id: Int, completed: Boolean): Int

    /**
     * Mengganti wilayah SEKALIGUS mengosongkan koordinat.
     *
     * Koordinat milik wilayah lama WAJIB ikut dibuang: kalau tidak, saat BMKG
     * gagal untuk wilayah baru, fallback Open-Meteo akan mengambil cuaca
     * lokasi LAMA dan menampilkannya dengan nama wilayah BARU — salah data
     * yang tidak terlihat sama sekali oleh pengguna.
     */
    @Query(
        "UPDATE user_preference SET regionCode = :code, regionName = :name, " +
            "latitude = NULL, longitude = NULL WHERE id = :id",
    )
    suspend fun updateRegionColumns(id: Int, code: String, name: String): Int

    /**
     * Menyimpan koordinat kelurahan yang dipelajari dari response BMKG.
     *
     * Sengaja tidak menyentuh kolom lain: dipanggil dari jalur pengambilan
     * cuaca, yang bisa berjalan bersamaan dengan penulisan lain.
     */
    @Query("UPDATE user_preference SET latitude = :lat, longitude = :lon WHERE id = :id")
    suspend fun updateCoordinates(id: Int, lat: Double, lon: Double): Int

    /**
     * Menandai onboarding selesai secara atomik.
     *
     * Sengaja BUKAN read-modify-write di ViewModel: pola itu bisa menimpa field
     * lain (mis. regionCode yang ditulis region picker Fase 2) dengan salinan
     * basi bila ada penulis lain di sela baca dan tulis. UPDATE bertarget hanya
     * menyentuh satu kolom; INSERT dipakai hanya bila barisnya memang belum ada.
     */
    @Transaction
    suspend fun markOnboardingCompleted(id: Int = Constants.USER_PREFERENCE_ID) {
        val updatedRows = updateOnboardingCompleted(id, true)
        if (updatedRows == 0) {
            upsert(UserPreferenceEntity(id = id, isOnboardingCompleted = true))
        }
    }

    /**
     * Menyimpan wilayah pilihan pengguna secara atomik.
     *
     * Alasannya sama dengan [markOnboardingCompleted] dan berlaku dua arah:
     * read-modify-write di sini bisa mengembalikan `isOnboardingCompleted`
     * ke false bila onboarding selesai di sela baca dan tulis. UPDATE
     * bertarget hanya menyentuh dua kolom wilayah.
     */
    @Transaction
    suspend fun updateRegion(
        code: String,
        name: String,
        id: Int = Constants.USER_PREFERENCE_ID,
    ) {
        val updatedRows = updateRegionColumns(id, code, name)
        if (updatedRows == 0) {
            upsert(UserPreferenceEntity(id = id, regionCode = code, regionName = name))
        }
    }
}
