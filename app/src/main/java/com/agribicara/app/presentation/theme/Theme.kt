package com.agribicara.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Dynamic color (Material You) sengaja TIDAK dipakai: warna yang diambil dari
 * wallpaper pengguna dapat merusak rasio kontras yang justru kritis untuk
 * pemakaian di bawah sinar matahari.
 *
 * Dark mode dikerjakan di Fase 6; warna sudah berupa token bernama sehingga
 * penambahan darkColorScheme nanti tidak perlu menyentuh composable mana pun.
 */
private val AgriLightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = White,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenDark,
    secondary = BrownSecondary,
    onSecondary = White,
    secondaryContainer = BrownLight,
    onSecondaryContainer = OnSurfaceDark,
    tertiary = YellowAccent,
    onTertiary = OnSurfaceDark,
    background = SurfaceLight,
    onBackground = OnSurfaceDark,
    surface = SurfaceLight,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceDark,
    outline = OutlineGrey,
    error = ErrorRed,
    onError = White,
)

@Composable
fun AgriBicaraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AgriLightColors,
        typography = AgriTypography,
        content = content,
    )
}
