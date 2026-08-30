package com.agribicara.app.presentation.theme

import androidx.compose.ui.unit.dp

/**
 * Token ukuran.
 *
 * Angka di sini menegakkan NFR aksesibilitas PRD — jangan memakai nilai dp
 * mentah di composable.
 */
object Dimens {
    /** Target sentuh minimum untuk SEMUA elemen interaktif. */
    val TouchTargetMin = 48.dp

    /** Tinggi CTA primer (mengikuti pola onboarding aplikasi produksi). */
    val ButtonHeight = 56.dp

    /** Tombol mic di Home — elemen paling menonjol di layar. */
    val MicButtonSize = 120.dp
    val MicIconSize = 56.dp
    val OnboardingIconSize = 120.dp

    val ScreenPadding = 20.dp
    val SpaceExtraSmall = 4.dp
    val SpaceSmall = 8.dp
    val SpaceMedium = 16.dp
    val SpaceLarge = 24.dp
    val SpaceExtraLarge = 32.dp

    val ProgressBarHeight = 6.dp
    val CardCornerRadius = 16.dp
}
