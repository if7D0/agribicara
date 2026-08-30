package com.agribicara.app.domain.model

/**
 * Satu wilayah administratif.
 *
 * [code] berformat titik ("32", "32.77", "32.77.01", "32.77.01.1002").
 * Hanya kode tingkat [RegionLevel.VILLAGE] yang bisa dipakai memanggil BMKG.
 */
data class Region(
    val code: String,
    val name: String,
    val level: RegionLevel,
)

/**
 * Tingkat wilayah, berurutan dari terluas ke tersempit.
 *
 * Region picker menuruni keempatnya. BMKG hanya menerima [VILLAGE] — query
 * tingkat kecamatan ditolak (301), dan tidak ada reverse-geocode dari
 * koordinat, itulah sebabnya picker manual wajib ada.
 */
enum class RegionLevel {
    PROVINCE,
    REGENCY,
    DISTRICT,
    VILLAGE,
    ;

    /** Tingkat berikutnya yang lebih sempit, atau null bila sudah paling dalam. */
    fun next(): RegionLevel? = when (this) {
        PROVINCE -> REGENCY
        REGENCY -> DISTRICT
        DISTRICT -> VILLAGE
        VILLAGE -> null
    }

    /** Tingkat sebelumnya yang lebih luas, atau null bila sudah paling luar. */
    fun previous(): RegionLevel? = when (this) {
        PROVINCE -> null
        REGENCY -> PROVINCE
        DISTRICT -> REGENCY
        VILLAGE -> DISTRICT
    }
}
