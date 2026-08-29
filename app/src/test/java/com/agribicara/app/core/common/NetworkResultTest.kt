package com.agribicara.app.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkResultTest {

    @Test
    fun `map mengubah data saat Success`() {
        // Arrange
        val result: NetworkResult<Int> = NetworkResult.Success(2)

        // Act
        val mapped = result.map { it * 3 }

        // Assert
        assertEquals(NetworkResult.Success(6), mapped)
    }

    @Test
    fun `map tidak mengubah apa pun saat Error`() {
        // Arrange
        val cause = IllegalStateException("boom")
        val result: NetworkResult<Int> = NetworkResult.Error("Gagal memuat data", cause)

        // Act
        val mapped = result.map { it * 3 }

        // Assert
        assertEquals(result, mapped)
    }

    @Test
    fun `map tidak mengubah apa pun saat Loading`() {
        // Arrange
        val result: NetworkResult<Int> = NetworkResult.Loading

        // Act
        val mapped = result.map { it * 3 }

        // Assert
        assertEquals(NetworkResult.Loading, mapped)
    }

    @Test
    fun `dataOrNull mengembalikan data saat Success`() {
        // Arrange
        val result: NetworkResult<String> = NetworkResult.Success("cerah")

        // Act & Assert
        assertEquals("cerah", result.dataOrNull())
    }

    @Test
    fun `dataOrNull mengembalikan null saat Error`() {
        // Arrange
        val result: NetworkResult<String> = NetworkResult.Error("Tidak ada koneksi")

        // Act & Assert
        assertNull(result.dataOrNull())
    }

    @Test
    fun `pesan Error dapat dibaca pengguna dan cause boleh kosong`() {
        // Arrange: pesan wajib Bahasa Indonesia sederhana, cause opsional
        val result = NetworkResult.Error("Tidak ada koneksi internet")

        // Assert
        assertEquals("Tidak ada koneksi internet", result.message)
        assertNull(result.cause)
    }
}
