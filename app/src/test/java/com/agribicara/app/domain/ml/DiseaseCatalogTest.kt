package com.agribicara.app.domain.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiseaseCatalogTest {

    @Test
    fun `label penyakit dikenal mengembalikan info Bahasa Indonesia`() {
        val info = DiseaseCatalog.infoFor("blast")
        assertNotNull(info)
        assertEquals("blast", info!!.label)
        assertEquals("Blas", info.displayName)
        assertTrue("Ciri tidak boleh kosong", info.summary.isNotBlank())
    }

    @Test
    fun `label tak dikenal mengembalikan null`() {
        assertNull(DiseaseCatalog.infoFor("penyakit_yang_tidak_ada"))
    }

    @Test
    fun `label normal dikenali sebagai sehat, bukan penyakit`() {
        assertTrue(DiseaseCatalog.isHealthy(DiseaseCatalog.NORMAL_LABEL))
        // "normal" bukan penyakit, jadi infoFor untuknya null — ditangani lewat isHealthy.
        assertNull(DiseaseCatalog.infoFor(DiseaseCatalog.NORMAL_LABEL))
    }

    @Test
    fun `label penyakit bukan sehat`() {
        assertFalse(DiseaseCatalog.isHealthy("blast"))
    }
}
