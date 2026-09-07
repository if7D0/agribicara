package com.agribicara.app.presentation.license

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agribicara.app.core.common.Constants
import com.agribicara.app.core.common.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

data class LicenseUiState(
    /**
     * Isi `ml/NOTICE` apa adanya. Kosong selama masih dimuat.
     *
     * Sengaja teks mentah, bukan dipecah menjadi field terstruktur: yang
     * diwajibkan lisensi Apache 2.0 adalah atribusinya disertakan utuh, dan
     * memformat ulangnya di sini akan menghidupkan lagi risiko penyimpangan
     * yang justru ditutup oleh berkas tunggal itu.
     */
    val noticeText: String = "",
    /**
     * True bila aset atribusi gagal dibaca.
     *
     * Dibedakan dari "sedang memuat" karena layar kosong tanpa penjelasan pada
     * layar LISENSI terlihat seperti aplikasi menyembunyikan atribusinya.
     */
    val hasFailed: Boolean = false,
) {
    val isLoading: Boolean get() = noticeText.isEmpty() && !hasFailed
}

/**
 * Memuat teks atribusi pihak ketiga untuk layar Lisensi.
 *
 * Membaca aset adalah I/O, jadi dilakukan di `dispatchers.io` — berkasnya kecil,
 * tetapi membaca berkas di main thread adalah kebiasaan yang cepat menular ke
 * tempat yang berkasnya tidak kecil.
 *
 * Kegagalan baca TIDAK dilempar. Aset ini dibundel di APK sehingga secara
 * praktis selalu ada; bila toh gagal, layar menampilkan pesan singkat dan
 * aplikasi tetap berjalan. Membuat aplikasi crash di layar lisensi adalah
 * hukuman yang jauh lebih besar daripada masalahnya.
 */
@HiltViewModel
class LicenseViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LicenseUiState())
    val uiState: StateFlow<LicenseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val text = withContext(dispatchers.io) {
                runCatching {
                    context.assets
                        .open(Constants.LICENSE_NOTICE_ASSET)
                        .bufferedReader()
                        .use { it.readText() }
                        .trim()
                }.getOrElse {
                    Timber.w(it, "Aset atribusi gagal dibaca")
                    null
                }
            }

            _uiState.update {
                if (text.isNullOrBlank()) {
                    it.copy(hasFailed = true)
                } else {
                    it.copy(noticeText = text, hasFailed = false)
                }
            }
        }
    }
}
