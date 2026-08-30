package com.agribicara.app.data.repository

import android.content.Context
import com.agribicara.app.R
import com.agribicara.app.core.common.Constants
import com.agribicara.app.core.common.DispatcherProvider
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.data.local.dao.RegionCacheDao
import com.agribicara.app.data.local.dao.UserPreferenceDao
import com.agribicara.app.data.local.entity.RegionCacheEntity
import com.agribicara.app.data.remote.wilayah.WilayahApiService
import com.agribicara.app.data.remote.wilayah.dto.WilayahResponse
import com.agribicara.app.domain.model.Region
import com.agribicara.app.domain.model.RegionLevel
import com.agribicara.app.domain.repository.RegionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.Response
import timber.log.Timber

/**
 * Daftar wilayah dari wilayah.id, dengan cache Room.
 *
 * Cache dibaca lebih dulu: setelah satu tingkat pernah dibuka, tingkat itu
 * tetap bisa ditelusuri tanpa internet. Tanpa cache, region picker akan mati
 * total begitu wilayah.id tidak bisa dihubungi — dan pengguna tidak akan bisa
 * mengganti wilayahnya sama sekali.
 */
@Singleton
class RegionRepositoryImpl @Inject constructor(
    private val wilayahApi: WilayahApiService,
    private val regionCacheDao: RegionCacheDao,
    private val userPreferenceDao: UserPreferenceDao,
    private val dispatchers: DispatcherProvider,
    @ApplicationContext private val context: Context,
) : RegionRepository {

    override suspend fun getRegions(
        level: RegionLevel,
        parentCode: String?,
        forceRefresh: Boolean,
    ): NetworkResult<List<Region>> = withContext(dispatchers.io) {
        if (!forceRefresh) {
            val cached = readCache(level, parentCode)
            if (cached.isNotEmpty()) return@withContext NetworkResult.Success(cached)
        }

        val fetched = fetchFromNetwork(level, parentCode)
        if (fetched != null) {
            writeCache(fetched, parentCode)
            return@withContext NetworkResult.Success(fetched)
        }

        // Jaringan gagal saat refresh paksa: lebih baik menampilkan cache lama
        // daripada mengosongkan layar yang tadinya sudah terisi.
        val fallback = readCache(level, parentCode)
        if (fallback.isNotEmpty()) {
            NetworkResult.Success(fallback)
        } else {
            NetworkResult.Error(context.getString(R.string.error_region_load))
        }
    }

    override fun observeSelectedRegion(): Flow<Region?> =
        userPreferenceDao.observe(Constants.USER_PREFERENCE_ID).map { preference ->
            val code = preference?.regionCode ?: return@map null
            val name = preference.regionName ?: return@map null
            Region(code = code, name = name, level = RegionLevel.VILLAGE)
        }

    override suspend fun saveSelectedRegion(region: Region) {
        userPreferenceDao.updateRegion(code = region.code, name = region.name)
    }

    /** null berarti gagal; daftar kosong dari server juga diperlakukan gagal. */
    private suspend fun fetchFromNetwork(
        level: RegionLevel,
        parentCode: String?,
    ): List<Region>? = try {
        val response = requestFor(level, parentCode)
        if (!response.isSuccessful) {
            Timber.w("wilayah.id membalas HTTP %d untuk %s", response.code(), level)
            null
        } else {
            response.body()?.items.orEmpty()
                .mapNotNull { item ->
                    val code = item.code?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val name = item.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    Region(code = code, name = name, level = level)
                }
                .takeIf { it.isNotEmpty() }
        }
    } catch (e: CancellationException) {
        // Pembatalan coroutine WAJIB diteruskan, bukan ditelan sebagai kegagalan.
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Gagal memuat daftar wilayah %s", level)
        null
    }

    private suspend fun requestFor(
        level: RegionLevel,
        parentCode: String?,
    ): Response<WilayahResponse> = when (level) {
        RegionLevel.PROVINCE -> wilayahApi.getProvinces()
        RegionLevel.REGENCY -> wilayahApi.getRegencies(requireParent(parentCode, level))
        RegionLevel.DISTRICT -> wilayahApi.getDistricts(requireParent(parentCode, level))
        RegionLevel.VILLAGE -> wilayahApi.getVillages(requireParent(parentCode, level))
    }

    private fun requireParent(parentCode: String?, level: RegionLevel): String =
        requireNotNull(parentCode) { "parentCode wajib diisi untuk tingkat $level" }

    private suspend fun readCache(level: RegionLevel, parentCode: String?): List<Region> = try {
        regionCacheDao.getByLevel(level.name, parentCode).map { entity ->
            Region(code = entity.code, name = entity.name, level = level)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Gagal membaca cache wilayah")
        emptyList()
    }

    private suspend fun writeCache(regions: List<Region>, parentCode: String?) {
        try {
            regionCacheDao.upsertAll(
                regions.map { region ->
                    RegionCacheEntity(
                        code = region.code,
                        name = region.name,
                        level = region.level.name,
                        parentCode = parentCode,
                    )
                },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Gagal menyimpan cache wilayah")
        }
    }
}
