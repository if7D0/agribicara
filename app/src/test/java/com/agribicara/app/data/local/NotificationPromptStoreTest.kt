package com.agribicara.app.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import java.io.IOException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Memaku penjaga kegagalan baca DataStore.
 *
 * Ada karena temuan H1 tinjauan Fase 9. `DataStore.data` melempar KE DALAM Flow
 * bila berkasnya tidak terbaca, dan tanpa penangkap lemparan itu keluar dari
 * `viewModelScope` milik `HomeViewModel` lalu MENJATUHKAN aplikasi di layar
 * Home — layar pertama yang dilihat petani — demi fitur yang isinya satu
 * boolean.
 *
 * Yang diuji adalah [tanpaGagalBaca] sebagai operator murni, bukan
 * [NotificationPromptStore] utuh: `preferencesDataStore` terikat pada Context
 * dan menulis berkas sungguhan, sehingga tidak bisa dijalankan di JVM. Itu
 * persis alasan kebijakannya diangkat keluar — penjaga yang tidak pernah bisa
 * memerah adalah penjaga palsu.
 *
 * **Batas penjaga ini, dinyatakan terus terang.** Yang dibuktikan adalah
 * kebijakannya benar. Yang TIDAK dibuktikan adalah `hasAsked` benar-benar
 * memasang operator ini pada `data`-nya; menghapus `.tanpaGagalBaca()` di sana
 * masih akan meloloskan seluruh test di berkas ini. Sama seperti
 * `AiGenerationConfigTest` terhadap `FirebaseTextGenerator`.
 */
class NotificationPromptStoreTest {

    private val kunci = booleanPreferencesKey("notification_permission_asked")

    @Test
    fun `nilai yang terbaca normal diteruskan apa adanya`() = runTest {
        val isi = mutablePreferencesOf(kunci to true)

        val hasil = flowOf(isi).tanpaGagalBaca().toList()

        assertEquals(listOf(isi), hasil)
    }

    @Test
    fun `berkas rusak jatuh ke preferences kosong, bukan lemparan`() = runTest {
        // CorruptionException DataStore adalah turunan IOException; IOException
        // biasa (penyimpanan penuh) menempuh jalur yang sama.
        val rusak = flow<androidx.datastore.preferences.core.Preferences> {
            throw IOException("berkas preferences rusak")
        }

        val hasil = rusak.tanpaGagalBaca().toList()

        // Kosong berarti "belum pernah diminta": dialog bisa muncul sekali lagi
        // — mengganggu, bukan merusak. Yang penting Flow-nya SELESAI, bukan
        // melempar keluar dan menjatuhkan layar Home.
        assertEquals(listOf(emptyPreferences()), hasil)
    }

    @Test
    fun `nilai yang sempat terbaca sebelum kerusakan tidak hilang`() = runTest {
        val sebelum = mutablePreferencesOf(kunci to true)
        val setengahJalan = flow {
            emit(sebelum)
            throw IOException("penyimpanan penuh di tengah jalan")
        }

        val hasil = setengahJalan.tanpaGagalBaca().toList()

        assertEquals(listOf(sebelum, emptyPreferences()), hasil)
    }

    @Test(expected = IllegalStateException::class)
    fun `kegagalan yang BUKAN IO tetap dilempar`() = runTest {
        // Tripwire arah sebaliknya. Menelan segalanya akan menyembunyikan cacat
        // pemrograman di balik fitur sepele, dan tanpa test ini `catch { }`
        // yang terlalu longgar tidak akan tertangkap oleh apa pun.
        val cacat = flow<androidx.datastore.preferences.core.Preferences> {
            throw IllegalStateException("cacat pemrograman, bukan berkas rusak")
        }

        cacat.tanpaGagalBaca().toList()
    }
}
