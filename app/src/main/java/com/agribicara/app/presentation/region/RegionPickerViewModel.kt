package com.agribicara.app.presentation.region

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.domain.model.Region
import com.agribicara.app.domain.model.RegionLevel
import com.agribicara.app.domain.usecase.GetRegionsUseCase
import com.agribicara.app.domain.usecase.SaveSelectedRegionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class RegionPickerUiState(
    val level: RegionLevel = RegionLevel.PROVINCE,
    val items: List<Region> = emptyList(),
    /** Jejak pilihan dari provinsi ke bawah; dipakai untuk naik lagi saat back. */
    val breadcrumb: List<Region> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** Menjadi true tepat sekali, saat kelurahan berhasil disimpan. */
    val isCompleted: Boolean = false,
) {
    /** Item yang lolos filter pencarian. Daftar kelurahan bisa panjang. */
    val visibleItems: List<Region>
        get() = if (query.isBlank()) {
            items
        } else {
            items.filter { it.name.contains(query.trim(), ignoreCase = true) }
        }
}

/**
 * Region picker bertingkat: Provinsi -> Kab/Kota -> Kecamatan -> Kelurahan.
 *
 * Picker manual ini WAJIB ada, bukan pilihan gaya: BMKG hanya menerima kode
 * `adm4` (kelurahan) dan tidak menyediakan reverse-geocode dari koordinat,
 * sehingga GPS saja tidak akan pernah cukup untuk menentukan wilayah.
 *
 * Seluruh perubahan state memakai [MutableStateFlow.update] — bukan
 * `value = value.copy(...)` — karena pemuatan daftar, penyimpanan, dan aksi
 * pengguna bisa menulis dari coroutine berbeda, dan pola baca-lalu-tulis akan
 * diam-diam membuang perubahan field lain.
 */
@HiltViewModel
class RegionPickerViewModel @Inject constructor(
    private val getRegions: GetRegionsUseCase,
    private val saveSelectedRegion: SaveSelectedRegionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegionPickerUiState())
    val uiState: StateFlow<RegionPickerUiState> = _uiState.asStateFlow()

    /**
     * Pemuatan yang sedang berjalan.
     *
     * Wajib dibatalkan sebelum memulai yang baru: tanpa ini, respons tingkat
     * yang SUDAH ditinggalkan pengguna bisa datang belakangan dan menimpa
     * daftar tingkat yang sedang tampil — pengguna lalu menelusuri kabupaten
     * milik provinsi yang salah tanpa tanda apa pun.
     */
    private var loadJob: Job? = null

    init {
        load(RegionLevel.PROVINCE, parentCode = null)
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    /**
     * Memilih satu item.
     *
     * Pada tingkat kelurahan ini berarti menyimpan dan menutup layar; pada
     * tingkat lain berarti turun satu tingkat.
     */
    fun onSelect(region: Region) {
        if (_uiState.value.isSaving) return

        val nextLevel = region.level.next()
        if (nextLevel == null) {
            persist(region)
            return
        }

        _uiState.update { it.copy(breadcrumb = it.breadcrumb + region, query = "") }
        load(nextLevel, parentCode = region.code)
    }

    /**
     * Naik satu tingkat.
     *
     * @return true bila masih ada tingkat di atas, false bila sudah di
     * provinsi — pemanggil memakai ini untuk memutuskan menutup layar.
     */
    fun onBack(): Boolean {
        val state = _uiState.value
        if (state.isSaving || state.breadcrumb.isEmpty()) return false

        val level = state.level.previous() ?: return false
        val parent = state.breadcrumb.dropLast(1)
        _uiState.update { it.copy(breadcrumb = parent, query = "") }
        load(level, parentCode = parent.lastOrNull()?.code)
        return true
    }

    /**
     * Memuat ulang tingkat yang sedang tampil, MELEWATI cache.
     *
     * Refresh paksa penting karena response terpotong (mis. 2 dari 40
     * kelurahan) tetap tersimpan sebagai cache yang sah; membaca ulang cache
     * yang sama tidak akan pernah memperbaikinya.
     */
    fun retry() {
        val state = _uiState.value
        load(state.level, parentCode = state.breadcrumb.lastOrNull()?.code, forceRefresh = true)
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun load(level: RegionLevel, parentCode: String?, forceRefresh: Boolean = false) {
        loadJob?.cancel()
        _uiState.update {
            it.copy(level = level, isLoading = true, errorMessage = null, items = emptyList())
        }
        loadJob = viewModelScope.launch {
            when (val result = getRegions(level, parentCode, forceRefresh)) {
                is NetworkResult.Success ->
                    _uiState.update { it.copy(items = result.data, isLoading = false) }

                is NetworkResult.Error -> {
                    Timber.w("Gagal memuat wilayah tingkat %s", level)
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }

                NetworkResult.Loading -> Unit
            }
        }
    }

    /**
     * Navigasi TIDAK lewat callback dari sini — lihat alasannya di
     * OnboardingViewModel: lambda UI menahan NavHostController dan akan
     * membocorkan controller milik Activity yang sudah hancur saat rotasi.
     * Layar mengamati [RegionPickerUiState.isCompleted].
     */
    private fun persist(region: Region) {
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                saveSelectedRegion(region)
                _uiState.update { it.copy(isSaving = false, isCompleted = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Gagal menyimpan wilayah pilihan")
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Gagal menyimpan. Coba lagi.")
                }
            }
        }
    }
}
