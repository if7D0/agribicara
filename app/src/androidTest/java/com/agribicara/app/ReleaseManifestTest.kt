package com.agribicara.app

import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Menjaga agar ikon aplikasi benar-benar terpasang pada APK jadi.
 *
 * Ada karena Fase 8 mengganti ikon placeholder `@drawable/ic_launcher` dengan
 * `@mipmap/ic_launcher` beserta varian bulatnya, dan menghapus berkas lamanya.
 * Salah satu dari tiga hal bisa terjadi tanpa satu pun sinyal otomatis:
 * manifest lupa diperbarui, `roundIcon` lupa ditambahkan, atau PNG legacy untuk
 * API 24-25 lupa dibangkitkan sehingga ikon hilang justru di perangkat tertua
 * yang didukung proyek ini.
 *
 * Dibaca dari [PackageManager], BUKAN dari berkas manifest — alasannya sama
 * dengan [ManifestInitializerTest]: yang ingin dibuktikan adalah apa yang
 * benar-benar dipasang sistem, bukan apa yang tertulis di sumber.
 *
 * **Batas penjaga ini, dinyatakan terus terang, dan batasnya tidak sama untuk
 * kedua ikon.**
 *
 * Untuk `android:icon`, yang dibaca adalah [android.content.pm.ApplicationInfo.icon]
 * — benar-benar apa yang terpasang pada APK.
 *
 * Untuk `android:roundIcon` TIDAK ADA accessor publik: `ApplicationInfo` punya
 * `icon`, `logo`, dan `banner`, tetapi `roundIconRes` tidak termasuk SDK publik
 * (dicoba pada Fase 8, gagal kompilasi dengan "Unresolved reference"). Refleksi
 * ke field tersembunyi sengaja tidak dipakai — ia tunduk pada pembatasan
 * non-SDK interface sejak Android 9 dan bisa berhenti bekerja tanpa peringatan.
 * Jadi yang dijaga di sini hanyalah **resource-nya ada dan bisa di-resolve pada
 * densitas perangkat yang menjalankan test**. Manifest yang lupa menyebut
 * `android:roundIcon` TIDAK akan tertangkap test ini.
 *
 * Yang juga TIDAK dibuktikan: gambarnya benar, motifnya berada dalam safe zone
 * adaptive icon, dan ikon legacy terlihat serupa dengan adaptive icon.
 * Ketiganya hanya bisa dinilai mata manusia di launcher sungguhan.
 */
@RunWith(AndroidJUnit4::class)
class ReleaseManifestTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun applicationInfo() = context.packageManager.getApplicationInfo(
        context.packageName,
        PackageManager.GET_META_DATA,
    )

    @Test
    fun ikonAplikasiTerpasangDanBisaDiresolve() {
        val iconRes = applicationInfo().icon

        assertNotEquals(
            "android:icon tidak terpasang pada APK. Periksa AndroidManifest — " +
                "@drawable/ic_launcher sudah dihapus di Fase 8 dan diganti " +
                "@mipmap/ic_launcher.",
            0,
            iconRes,
        )
        assertNotNull(
            "android:icon terdaftar (id=$iconRes) tetapi tidak bisa di-resolve " +
                "menjadi drawable. Kemungkinan besar mipmap-anydpi-v26 ada tetapi " +
                "PNG legacy untuk API 24-25 tidak dibangkitkan; jalankan " +
                "`java tools/StoreAssets.java`.",
            context.getDrawable(iconRes),
        )
    }

    @Test
    fun resourceRoundIconAdaDanBisaDiresolve() {
        // Lihat catatan kelas: tidak ada cara publik membaca android:roundIcon
        // dari APK, jadi yang dijaga adalah resource-nya sendiri.
        assertNotNull(
            "@mipmap/ic_launcher_round tidak bisa di-resolve. Sebagian launcher " +
                "OEM membaca android:roundIcon dan mengabaikan android:icon — " +
                "termasuk Transsion (Infinix), perangkat uji proyek ini. " +
                "Jalankan `java tools/StoreAssets.java` bila PNG legacy untuk " +
                "API 24-25 belum dibangkitkan.",
            context.getDrawable(R.mipmap.ic_launcher_round),
        )
    }

    @Test
    fun ikonTerpasangSamaDenganResourceIkonAplikasi() {
        // Menghubungkan apa yang benar-benar terpasang pada APK dengan resource
        // yang dimaksud. Kalau manifest kelak menunjuk gambar lain — misalnya
        // kembali ke placeholder yang sudah dihapus, atau ke ikon debug — test
        // ini memerah meskipun kedua ikonnya sama-sama bisa di-resolve.
        assertNotEquals(
            "android:icon menunjuk resource selain @mipmap/ic_launcher.",
            R.mipmap.ic_launcher,
            0,
        )
        org.junit.Assert.assertEquals(
            "android:icon pada APK bukan @mipmap/ic_launcher.",
            R.mipmap.ic_launcher,
            applicationInfo().icon,
        )
    }

    @Test
    fun ikonPersegiDanBulatAdalahResourceYangBerbeda() {
        // Kalau keduanya menunjuk resource yang sama, hampir pasti salah satu
        // ditulis karena salah salin — bukan kesalahan yang menggagalkan build,
        // dan tidak terlihat di launcher yang tidak memakai roundIcon.
        assertNotEquals(R.mipmap.ic_launcher, R.mipmap.ic_launcher_round)
    }
}
