package com.agribicara.app.data.repository

import android.content.Context
import com.agribicara.app.core.common.DispatcherProvider
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.data.local.dao.RegionCacheDao
import com.agribicara.app.data.local.dao.UserPreferenceDao
import com.agribicara.app.data.local.entity.RegionCacheEntity
import com.agribicara.app.data.remote.wilayah.WilayahApiService
import com.agribicara.app.data.remote.wilayah.dto.WilayahItemDto
import com.agribicara.app.data.remote.wilayah.dto.WilayahResponse
import com.agribicara.app.domain.model.Region
import com.agribicara.app.domain.model.RegionLevel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class RegionRepositoryImplTest {

    private val wilayahApi = mockk<WilayahApiService>()
    private val regionCacheDao = mockk<RegionCacheDao>(relaxed = true)
    private val userPreferenceDao = mockk<UserPreferenceDao>(relaxed = true)
    private val context = mockk<Context>()

    private val dispatchers = object : DispatcherProvider {
        private val test = UnconfinedTestDispatcher()
        override val io = test
        override val default = test
        override val main = test
    }

    private lateinit var repository: RegionRepositoryImpl

    @Before
    fun setUp() {
        every { context.getString(any()) } returns "pesan error"
        repository = RegionRepositoryImpl(
            wilayahApi = wilayahApi,
            regionCacheDao = regionCacheDao,
            userPreferenceDao = userPreferenceDao,
            dispatchers = dispatchers,
            context = context,
        )
    }

    private fun provincesResponse() = Response.success(
        WilayahResponse(
            items = listOf(
                WilayahItemDto(code = "32", name = "Jawa Barat"),
                WilayahItemDto(code = "33", name = "Jawa Tengah"),
            ),
        ),
    )

    private fun cachedProvinces() = listOf(
        RegionCacheEntity("32", "Jawa Barat (cache)", "PROVINCE", null),
    )

    private fun <T> serverError(): Response<T> = Response.error(
        500,
        """{"error":"boom"}""".toResponseBody("application/json".toMediaType()),
    )

    @Test
    fun `cache dipakai lebih dulu tanpa menyentuh jaringan`() = runTest {
        coEvery { regionCacheDao.getByLevel("PROVINCE", null) } returns cachedProvinces()

        val result = repository.getRegions(RegionLevel.PROVINCE, null)

        assertTrue(result is NetworkResult.Success)
        assertEquals("Jawa Barat (cache)", (result as NetworkResult.Success).data[0].name)
        coVerify(exactly = 0) { wilayahApi.getProvinces() }
    }

    @Test
    fun `cache kosong memicu panggilan jaringan dan hasilnya disimpan`() = runTest {
        coEvery { regionCacheDao.getByLevel("PROVINCE", null) } returns emptyList()
        coEvery { wilayahApi.getProvinces() } returns provincesResponse()

        val result = repository.getRegions(RegionLevel.PROVINCE, null)

        assertEquals(2, (result as NetworkResult.Success).data.size)
        coVerify { regionCacheDao.upsertAll(any()) }
    }

    @Test
    fun `forceRefresh melewati cache dan mengambil ulang dari jaringan`() = runTest {
        // Inilah satu-satunya jalan keluar dari cache yang terpotong.
        coEvery { regionCacheDao.getByLevel("PROVINCE", null) } returns cachedProvinces()
        coEvery { wilayahApi.getProvinces() } returns provincesResponse()

        val result = repository.getRegions(RegionLevel.PROVINCE, null, forceRefresh = true)

        assertEquals(2, (result as NetworkResult.Success).data.size)
        assertEquals("Jawa Barat", result.data[0].name)
        coVerify { wilayahApi.getProvinces() }
    }

    @Test
    fun `forceRefresh yang gagal jatuh kembali ke cache lama`() = runTest {
        // Lebih baik daftar lama daripada layar kosong.
        coEvery { regionCacheDao.getByLevel("PROVINCE", null) } returns cachedProvinces()
        coEvery { wilayahApi.getProvinces() } throws IOException("mati")

        val result = repository.getRegions(RegionLevel.PROVINCE, null, forceRefresh = true)

        assertTrue(result is NetworkResult.Success)
        assertEquals("Jawa Barat (cache)", (result as NetworkResult.Success).data[0].name)
    }

    @Test
    fun `jaringan gagal dan cache kosong menghasilkan Error`() = runTest {
        coEvery { regionCacheDao.getByLevel("PROVINCE", null) } returns emptyList()
        coEvery { wilayahApi.getProvinces() } throws IOException("mati")

        assertTrue(repository.getRegions(RegionLevel.PROVINCE, null) is NetworkResult.Error)
    }

    @Test
    fun `HTTP error tidak disimpan sebagai cache`() = runTest {
        coEvery { regionCacheDao.getByLevel("PROVINCE", null) } returns emptyList()
        coEvery { wilayahApi.getProvinces() } returns serverError()

        val result = repository.getRegions(RegionLevel.PROVINCE, null)

        assertTrue(result is NetworkResult.Error)
        coVerify(exactly = 0) { regionCacheDao.upsertAll(any()) }
    }

    @Test
    fun `daftar kosong dari server diperlakukan gagal bukan sukses kosong`() = runTest {
        // Menyimpan daftar kosong sebagai cache akan mengunci picker selamanya.
        coEvery { regionCacheDao.getByLevel("PROVINCE", null) } returns emptyList()
        coEvery { wilayahApi.getProvinces() } returns Response.success(WilayahResponse())

        val result = repository.getRegions(RegionLevel.PROVINCE, null)

        assertTrue(result is NetworkResult.Error)
        coVerify(exactly = 0) { regionCacheDao.upsertAll(any()) }
    }

    @Test
    fun `item tanpa kode atau nama dibuang tanpa menjatuhkan sisanya`() = runTest {
        coEvery { regionCacheDao.getByLevel("PROVINCE", null) } returns emptyList()
        coEvery { wilayahApi.getProvinces() } returns Response.success(
            WilayahResponse(
                items = listOf(
                    WilayahItemDto(code = null, name = "Tanpa kode"),
                    WilayahItemDto(code = "33", name = "  "),
                    WilayahItemDto(code = "32", name = "Jawa Barat"),
                ),
            ),
        )

        val result = repository.getRegions(RegionLevel.PROVINCE, null)

        assertEquals(listOf("Jawa Barat"), (result as NetworkResult.Success).data.map { it.name })
    }

    @Test
    fun `setiap tingkat memanggil endpoint yang sesuai`() = runTest {
        coEvery { regionCacheDao.getByLevel(any(), any()) } returns emptyList()
        coEvery { wilayahApi.getRegencies("32") } returns provincesResponse()
        coEvery { wilayahApi.getDistricts("32.77") } returns provincesResponse()
        coEvery { wilayahApi.getVillages("32.77.01") } returns provincesResponse()

        repository.getRegions(RegionLevel.REGENCY, "32")
        repository.getRegions(RegionLevel.DISTRICT, "32.77")
        repository.getRegions(RegionLevel.VILLAGE, "32.77.01")

        coVerify { wilayahApi.getRegencies("32") }
        coVerify { wilayahApi.getDistricts("32.77") }
        coVerify { wilayahApi.getVillages("32.77.01") }
    }

    @Test
    fun `menyimpan wilayah pilihan memakai DAO atomik`() = runTest {
        repository.saveSelectedRegion(
            Region("32.77.01.1002", "Cibeureum", RegionLevel.VILLAGE),
        )

        // updateRegion sekaligus mengosongkan koordinat wilayah lama.
        coVerify { userPreferenceDao.updateRegion("32.77.01.1002", "Cibeureum", any()) }
    }
}
