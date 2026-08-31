package com.agribicara.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Dynamic color (Material You) sengaja TIDAK dipakai: warna yang diambil dari
 * wallpaper pengguna dapat merusak rasio kontras yang justru kritis untuk
 * pemakaian di bawah sinar matahari. Alasan ini tetap berlaku setelah mode
 * gelap ada.
 *
 * Mode gelap MENGIKUTI SETELAN SISTEM dan tidak punya tombol di dalam
 * aplikasi. Pengguna sasaran berliterasi digital rendah; satu layar setelan
 * baru berarti satu konsep baru yang harus dipelajari, demi preferensi yang
 * sudah mereka nyatakan sekali di tingkat sistem.
 *
 * Kedua skema di bawah WAJIB mengisi daftar peran yang sama persis. Peran
 * yang terlewat tidak membuat compile gagal — Material diam-diam memakai
 * bawaannya (ungu), dan itu baru terlihat sebagai warna asing di satu layar.
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
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerDark,
    onError = White,
)

/**
 * Peran primary dibalik perannya, bukan warnanya: [GreenLight] yang di mode
 * terang menjadi *container* kini menjadi *primary*, karena di atas latar
 * gelap yang dibutuhkan adalah nada terang. Rasio setiap pasangan dicatat di
 * [Color.kt].
 */
private val AgriDarkColors = darkColorScheme(
    primary = GreenLight,
    onPrimary = GreenDark,
    primaryContainer = GreenDark,
    onPrimaryContainer = GreenLight,
    secondary = BrownLight,
    onSecondary = BrownDark,
    secondaryContainer = BrownContainerDark,
    onSecondaryContainer = BrownLight,
    tertiary = YellowAccent,
    onTertiary = OnSurfaceDark,
    background = SurfaceDark,
    onBackground = OnSurfaceLight,
    surface = SurfaceDark,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineGreyDark,
    error = ErrorRedLight,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorContainerLight,
    onError = OnErrorDark,
)

@Composable
fun AgriBicaraTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isDarkTheme) AgriDarkColors else AgriLightColors,
        typography = AgriTypography,
        content = content,
    )
}
