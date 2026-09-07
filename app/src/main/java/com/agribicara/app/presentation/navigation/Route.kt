package com.agribicara.app.presentation.navigation

/**
 * Route berbasis konstanta String.
 *
 * KEPUTUSAN: plan Fase 1 menawarkan dua opsi (String vs route type-safe
 * @Serializable). Dipilih String dan tetap dipertahankan di Fase 2 meskipun
 * kotlinx-serialization sekarang sudah ada di project — mencampur dua gaya
 * route lebih mahal daripada keuntungan yang didapat. Konsisten: jangan
 * campur kedua gaya.
 *
 * Route berargumen memakai pola "weather/{regionCode}" bila nanti dibutuhkan;
 * saat ini layar cuaca membaca wilayah dari preferensi sehingga tidak perlu
 * argumen.
 */
object Route {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val WEATHER = "weather"
    const val REGION_PICKER = "region_picker"
    const val VOICE = "voice"
    const val DETECTION = "detection"
    const val LICENSE = "license"
}
