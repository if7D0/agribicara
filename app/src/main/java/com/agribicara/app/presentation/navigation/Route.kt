package com.agribicara.app.presentation.navigation

/**
 * Route berbasis konstanta String.
 *
 * KEPUTUSAN: plan menawarkan dua opsi (String vs route type-safe @Serializable).
 * Dipilih String karena Fase 1 hanya punya dua layar tanpa argumen, dan opsi ini
 * tidak menambah dependency kotlinx-serialization. Argumen di fase berikutnya
 * memakai pola "weather/{regionCode}". Konsisten — jangan campur kedua gaya.
 */
object Route {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
}
