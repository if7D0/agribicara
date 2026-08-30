package com.agribicara.app.data.remote.wilayah.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Amplop response wilayah.id — bentuknya identik untuk keempat tingkat
 * (provinsi, kab/kota, kecamatan, kelurahan), jadi satu DTO melayani semuanya.
 *
 * ```
 * {"data":[{"code":"32.77.01.1002","name":"Cibeureum"}],
 *  "meta":{"administrative_area_level":4,"updated_at":"2025-07-04"}}
 * ```
 */
@Serializable
data class WilayahResponse(
    @SerialName("data") val items: List<WilayahItemDto> = emptyList(),
)

@Serializable
data class WilayahItemDto(
    val code: String? = null,
    val name: String? = null,
)
