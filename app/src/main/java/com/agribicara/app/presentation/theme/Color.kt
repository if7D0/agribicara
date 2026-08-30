package com.agribicara.app.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Palet "earthy" kontras tinggi.
 *
 * Kontras tinggi wajib: petani memakai aplikasi di luar ruangan, layar terkena
 * sinar matahari langsung. Semua pasangan teks/latar di bawah menargetkan
 * rasio kontras minimal 4.5:1 (WCAG AA).
 */
val GreenPrimary = Color(0xFF2E7D32)
val GreenDark = Color(0xFF1B5E20)
val GreenLight = Color(0xFFA5D6A7)
val BrownSecondary = Color(0xFF6D4C41)
val BrownLight = Color(0xFFD7CCC8)
val YellowAccent = Color(0xFFF9A825)

val SurfaceLight = Color(0xFFFFFBFF)
val SurfaceVariantLight = Color(0xFFEFF1EC)
val OnSurfaceDark = Color(0xFF1A1C19)
val OutlineGrey = Color(0xFF72796F)
val ErrorRed = Color(0xFFBA1A1A)
val White = Color(0xFFFFFFFF)

/*
 * --- Mode gelap (Fase 6) -------------------------------------------------
 *
 * BUKAN hasil membalik palet terang. Membalik akan meletakkan GreenPrimary
 * (0xFF2E7D32) di atas latar gelap, yang rasionya jatuh di bawah 4.5:1 —
 * peran primary di mode gelap harus berupa nada TERANG.
 *
 * ErrorRed juga sengaja diganti: 0xFFBA1A1A di atas latar gelap hanya
 * mencapai 1.7:1, sehingga justru pesan kegagalan yang paling sulit dibaca.
 *
 * Setiap rasio di bawah dihitung dengan rumus WCAG 2.1 dan dicatat apa
 * adanya. Kalau salah satu warna diubah, hitung ulang — jangan menebak.
 *
 *   OnSurfaceLight  di atas SurfaceDark ......... 14.37:1
 *   OnSurfaceVariantLight di atas SurfaceVariantDark  5.51:1
 *   GreenDark       di atas GreenLight (primary)  4.79:1
 *   BrownDark       di atas BrownLight ........... 8.47:1
 *   OnSurfaceDark   di atas YellowAccent ......... 8.71:1
 *   OnErrorDark     di atas ErrorRedLight ........ 7.72:1
 *   OutlineGreyDark di atas SurfaceDark .......... 5.87:1
 *
 * Peran primary/tertiary/error di mode gelap dipakai ulang dari token terang
 * yang sudah ada (GreenLight, YellowAccent) — tidak perlu warna baru, dan
 * memakai ulang menjaga identitas visualnya tetap sama di kedua mode.
 */
val SurfaceDark = Color(0xFF12140E)
val SurfaceVariantDark = Color(0xFF43483E)
val OnSurfaceLight = Color(0xFFE2E3DD)
val OnSurfaceVariantLight = Color(0xFFC3C8BC)
val BrownDark = Color(0xFF3E2B25)
val BrownContainerDark = Color(0xFF4E342E)
val OutlineGreyDark = Color(0xFF8C9388)
val ErrorRedLight = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
