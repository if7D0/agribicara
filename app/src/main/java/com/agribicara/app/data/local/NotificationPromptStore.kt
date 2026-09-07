package com.agribicara.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

    /** True bila dialog izin notifikasi sudah pernah ditampilkan. */
    val hasAsked: Flow<Boolean> =
        context.notificationPromptDataStore.data.map { it[KEY_ASKED] == true }

    suspend fun markAsked() {
        context.notificationPromptDataStore.edit { it[KEY_ASKED] = true }
    }

    private companion object {
        val KEY_ASKED = booleanPreferencesKey("notification_permission_asked")
    }
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
