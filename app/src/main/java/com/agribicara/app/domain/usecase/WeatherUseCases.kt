package com.agribicara.app.domain.usecase

import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.domain.model.Forecast
import com.agribicara.app.domain.model.Region
import com.agribicara.app.domain.model.RegionLevel
import com.agribicara.app.domain.repository.RegionRepository
import com.agribicara.app.domain.repository.WeatherRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Use case sengaja tipis — hanya meneruskan ke repository.
 *
 * Nilainya bukan pada logika di dalamnya, melainkan pada batas yang
 * dibentuknya: ViewModel bergantung ke sini, bukan langsung ke repository,
 * sehingga Fase 5 (AI) bisa memakai ulang GetForecastUseCase yang sama untuk
 * grounding prompt tanpa menyeret detail data layer ke presentation.
 */
class GetForecastUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository,
) {
    suspend operator fun invoke(
        regionCode: String,
        regionName: String,
    ): NetworkResult<Forecast> = weatherRepository.getForecast(regionCode, regionName)
}

class GetRegionsUseCase @Inject constructor(
    private val regionRepository: RegionRepository,
) {
    suspend operator fun invoke(
        level: RegionLevel,
        parentCode: String?,
        forceRefresh: Boolean = false,
    ): NetworkResult<List<Region>> = regionRepository.getRegions(level, parentCode, forceRefresh)
}

class SaveSelectedRegionUseCase @Inject constructor(
    private val regionRepository: RegionRepository,
) {
    suspend operator fun invoke(region: Region) = regionRepository.saveSelectedRegion(region)
}

class ObserveSelectedRegionUseCase @Inject constructor(
    private val regionRepository: RegionRepository,
) {
    operator fun invoke(): Flow<Region?> = regionRepository.observeSelectedRegion()
}
