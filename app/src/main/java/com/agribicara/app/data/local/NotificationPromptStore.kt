package com.agribicara.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Mengingat bahwa izin notifikasi PERNAH diminta.
 *
 * Ada karena temuan F6 L5. Penandanya dulu `rememberSaveable` di dalam layar
 * Home, yang bertahan terhadap rotasi dan process death **tetapi tidak**
 * terhadap entri navigasi yang dibuang — kembali ke Home lewat jalur yang
 * membangun ulang entri itu membuat aplikasi meminta izin lagi kepada petani
 * yang sudah menolaknya.
 *
 * Dampaknya memang kecil: Android sendiri berhenti menampilkan dialog setelah
 * beberapa penolakan. Yang diperbaiki di sini hanya persistensi satu boolean —
 * ini BUKAN alur izin baru, dan jangan dijadikan begitu.
 *
 * **DataStore, bukan Room.** Room akan menuntut migrasi skema (v5 → v6) demi
 * satu boolean, sementara `androidx.datastore.preferences` sudah menjadi
 * dependency proyek sejak Fase 1 dan tidak menuntut apa pun. Ini pemakaian
 * pertamanya.
 */
@Singleton
class NotificationPromptStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * True bila dialog izin notifikasi sudah pernah ditampilkan.
     *
     * [tanpaGagalBaca] WAJIB ada dan bukan kehati-hatian berlebihan — lihat
     * KDoc-nya untuk apa yang dijaga dan kenapa ia berdiri sebagai operator
     * tersendiri alih-alih `catch` sebaris di sini.
     */
    val hasAsked: Flow<Boolean> = context.notificationPromptDataStore.data
        .tanpaGagalBaca()
        .map { it[KEY_ASKED] == true }

    suspend fun markAsked() {
        context.notificationPromptDataStore.edit { it[KEY_ASKED] = true }
    }

    private companion object {
        val KEY_ASKED = booleanPreferencesKey("notification_permission_asked")
    }
}

/**
 * Menjatuhkan kegagalan baca DataStore menjadi preferences kosong.
 *
 * **Yang dijaga.** `DataStore.data` melempar KE DALAM Flow bila berkasnya tidak
 * terbaca: `CorruptionException` saat berkas preferences rusak, `IOException`
 * biasa saat penyimpanan penuh. Tanpa penangkap, lemparan itu keluar dari
 * `viewModelScope` milik [com.agribicara.app.presentation.home.HomeViewModel]
 * tanpa satu pun penangkap dan MENJATUHKAN aplikasi di layar Home — layar
 * pertama yang dilihat petani — demi fitur yang isinya satu boolean.
 *
 * Jatuh ke "belum pernah diminta" adalah penilaian yang sama persis dengan
 * kegagalan [NotificationPromptStore.markAsked]: dialog bisa muncul sekali lagi
 * — mengganggu, bukan merusak. Yang BUKAN [IOException] tetap dilempar;
 * menelan segalanya akan menyembunyikan cacat pemrograman di balik fitur
 * sepele.
 *
 * **Kenapa operator tersendiri, bukan `catch` sebaris di `hasAsked`.**
 * `preferencesDataStore` terikat pada Context dan menulis berkas sungguhan,
 * sehingga apa pun yang menempel padanya tidak bisa dijalankan di JVM — dan
 * penjaga yang tidak pernah bisa memerah adalah penjaga palsu. Sebagai operator
 * murni atas `Flow<Preferences>`, kebijakannya diuji langsung oleh
 * `NotificationPromptStoreTest`. Preseden yang sama sudah dipakai
 * `SpeechErrorMapper` dan `TtsLanguageStatus`.
 */
internal fun Flow<Preferences>.tanpaGagalBaca(): Flow<Preferences> = catch { penyebab ->
    if (penyebab !is IOException) throw penyebab
    Timber.w(penyebab, "Penanda izin notifikasi gagal dibaca")
    emit(emptyPreferences())
}

/**
 * Delegate tingkat-berkas, sesuai anjuran resmi DataStore.
 *
 * WAJIB di luar kelas: `preferencesDataStore` membuat SATU instance per nama
 * berkas per proses, dan membuat instance kedua untuk berkas yang sama
 * melempar saat runtime. Menaruhnya di dalam kelas ber-`@Singleton` tampak
 * aman, tetapi test dan `@Singleton` yang dibuat ulang akan menabraknya.
 */
private val Context.notificationPromptDataStore by preferencesDataStore(
    name = "notification_prompt",
)
