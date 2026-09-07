package com.agribicara.app.domain.ml

import com.agribicara.app.domain.model.DiseaseInfo

/**
 * Menerjemahkan label kelas model → nama + ciri Bahasa Indonesia.
 *
 * Ini SATU-SATUNYA tempat daftar kelas dituliskan sebagai teks tampilan. Logika
 * (DiseaseClassificationPolicy) tidak pernah meng-hardcode daftar kelas — ia
 * memakai [isHealthy] dan [infoFor], sehingga label yang tak dikenal katalog
 * (mis. model versi berikutnya menambah kelas) otomatis jatuh ke "belum yakin"
 * alih-alih membuat aplikasi salah menebak.
 *
 * Label mengikuti kelas Paddy Doctor (basis latih, lihat plan Fase 4). Ciri
 * sengaja hanya menyebut gejala yang terlihat — TANPA saran pestisida/dosis,
 * konsisten dengan sikap Fase 5.
 *
 * Objek murni tanpa Android, punya unit test (`DiseaseCatalogTest`).
 */
object DiseaseCatalog {

    /** Label kelas "sehat" pada model. Ditangani terpisah dari penyakit. */
    const val NORMAL_LABEL = "normal"

    private val diseases: Map<String, DiseaseInfo> = listOf(
        DiseaseInfo(
            label = "bacterial_leaf_blight",
            displayName = "Hawar Daun Bakteri (Kresek)",
            summary = "Daun mengering dari ujung dan tepi, warna kekuningan lalu " +
                "kecokelatan seperti terbakar.",
        ),
        DiseaseInfo(
            label = "bacterial_leaf_streak",
            displayName = "Bercak Garis Bakteri",
            summary = "Garis-garis basah tembus cahaya di antara tulang daun, " +
                "lama-lama menguning kecokelatan.",
        ),
        DiseaseInfo(
            label = "bacterial_panicle_blight",
            displayName = "Hawar Malai Bakteri",
            summary = "Malai dan gabah menguning hingga kecokelatan, bulir banyak " +
                "yang hampa.",
        ),
        DiseaseInfo(
            label = "blast",
            displayName = "Blas",
            summary = "Bercak seperti belah ketupat bermata abu-abu di daun; pada " +
                "leher malai batangnya bisa patah.",
        ),
        DiseaseInfo(
            label = "brown_spot",
            displayName = "Bercak Cokelat",
            summary = "Bintik-bintik bulat cokelat merata di helai daun, sering " +
                "muncul saat tanaman kekurangan hara.",
        ),
        DiseaseInfo(
            label = "dead_heart",
            displayName = "Sundep (Penggerek Batang)",
            summary = "Pucuk atau anakan muda mengering dan mudah dicabut karena " +
                "batangnya digerek dari dalam.",
        ),
        DiseaseInfo(
            label = "downy_mildew",
            displayName = "Bulai",
            summary = "Daun menguning bergaris memanjang dan tanaman kerdil; " +
                "kadang tampak lapisan putih di bawah daun.",
        ),
        DiseaseInfo(
            label = "hispa",
            displayName = "Hama Hispa",
            summary = "Garis-garis putih memanjang bekas kikisan pada permukaan " +
                "daun, daun tampak seperti tergores.",
        ),
        DiseaseInfo(
            label = "tungro",
            displayName = "Tungro",
            summary = "Daun muda menguning hingga jingga mulai dari ujung, tanaman " +
                "kerdil dan anakan berkurang.",
        ),
    ).associateBy { it.label }

    /** True bila label menandai daun sehat. */
    fun isHealthy(label: String): Boolean = label == NORMAL_LABEL

    /**
     * Info penyakit untuk [label], atau null bila label tak dikenal (termasuk
     * [NORMAL_LABEL], yang ditangani lewat [isHealthy], bukan sebagai penyakit).
     */
    fun infoFor(label: String): DiseaseInfo? = diseases[label]
}
