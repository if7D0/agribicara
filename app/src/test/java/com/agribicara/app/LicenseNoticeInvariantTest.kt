package com.agribicara.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aset atribusi yang dikirim ke petani HARUS identik dengan `ml/NOTICE`.
 *
 * Dataset Paddy Doctor berlisensi Apache 2.0 dan mewajibkan atribusinya ikut
 * disertakan pada distribusi aplikasi. Sumber kebenarannya adalah `ml/NOTICE`,
 * yang tinggal bersama resep pelatihan — di situlah orang akan memperbaruinya
 * kalau dataset atau kutipannya berubah.
 *
 * Aplikasi tidak bisa membaca berkas itu saat runtime, jadi salinannya dibundel
 * sebagai aset. Salinan berarti dua sumber kebenaran, dan proyek ini sudah
 * pernah membayar mahal untuk itu: notebook Colab pernah memuat salinan inline
 * logika latih, resepnya diperbaiki di satu tempat, dan Colab menjalankan
 * salinan yang lain selama berhari-hari tanpa ada yang menyadari.
 *
 * Test ini membuat penyimpangan itu MUSTAHIL lolos diam-diam. Ia sengaja
 * membandingkan isi apa adanya, bukan sekadar "mengandung nama dataset":
 * kegagalan yang ingin dicegah justru yang halus — kutipan penulis diperbarui
 * di `ml/NOTICE` sementara APK terus mengirim versi lama.
 *
 * Kalau test ini merah, JANGAN mengubah berkas ekspektasinya. Jalankan:
 * `cp ml/NOTICE app/src/main/assets/paddy_doctor_notice.txt`
 */
class LicenseNoticeInvariantTest {

    /**
     * Unit test JVM berjalan dengan direktori kerja pada modul `app/`, sehingga
     * `ml/` berada satu tingkat di atas. Diverifikasi lewat assertion di bawah,
     * bukan diasumsikan — path yang salah akan membuat test ini lulus palsu
     * seandainya kegagalan bacanya ditelan.
     */
    private val sumberKebenaran = File("../ml/NOTICE")
    private val asetTerbundel = File("src/main/assets/paddy_doctor_notice.txt")

    @Test
    fun `kedua berkas benar-benar ada di tempat yang diharapkan`() {
        // Tanpa ini, salah path akan tampak seperti "isi sama-sama kosong".
        assertTrue(
            "Sumber kebenaran tidak ditemukan: ${sumberKebenaran.absolutePath}",
            sumberKebenaran.isFile,
        )
        assertTrue(
            "Aset terbundel tidak ditemukan: ${asetTerbundel.absolutePath}",
            asetTerbundel.isFile,
        )
    }

    @Test
    fun `aset atribusi identik dengan ml NOTICE`() {
        // Akhiran baris dinormalkan: repo ini dikerjakan di Windows dengan
        // autocrlf, dan CRLF versus LF bukan penyimpangan atribusi yang
        // pantas memerahkan build.
        assertEquals(
            "Aset atribusi menyimpang dari ml/NOTICE. " +
                "Perbaiki dengan menyalin ulang, JANGAN menyunting salah satunya terpisah.",
            sumberKebenaran.readText().replace("\r\n", "\n"),
            asetTerbundel.readText().replace("\r\n", "\n"),
        )
    }

    @Test
    fun `atribusi menyebut dataset dan lisensinya`() {
        // Penjaga terakhir kalau suatu saat seseorang mengosongkan KEDUA
        // berkas sekaligus — test kesamaan di atas akan tetap hijau.
        val isi = asetTerbundel.readText()

        assertTrue("Nama dataset hilang", isi.contains("Paddy Doctor"))
        assertTrue("Lisensi tidak disebut", isi.contains("Apache License"))
    }
}
