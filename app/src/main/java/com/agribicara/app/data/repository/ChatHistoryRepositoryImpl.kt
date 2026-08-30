package com.agribicara.app.data.repository

import com.agribicara.app.core.common.Constants
import com.agribicara.app.core.common.DispatcherProvider
import com.agribicara.app.data.local.dao.ChatMessageDao
import com.agribicara.app.data.local.entity.ChatMessageEntity
import com.agribicara.app.domain.model.ChatMessage
import com.agribicara.app.domain.model.ChatRole
import com.agribicara.app.domain.repository.ChatHistoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Riwayat percakapan di Room.
 *
 * Kegagalan menulis SENGAJA ditelan dan hanya dicatat: jawaban yang sudah
 * berhasil didapat tidak boleh hilang dari layar hanya karena penyimpanan
 * riwayat bermasalah. Riwayat adalah kenyamanan; jawabannya yang utama.
 */
@Singleton
class ChatHistoryRepositoryImpl @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val dispatchers: DispatcherProvider,
) : ChatHistoryRepository {

    override suspend fun append(message: ChatMessage) {
        withContext(dispatchers.io) {
            runSafely("simpan riwayat") {
                chatMessageDao.insert(message.toEntity())
                // Dipangkas setiap kali menulis, bukan lewat pekerjaan
                // terjadwal: perangkat murah tidak selalu sempat menjalankan
                // WorkManager, dan tabel yang tumbuh tanpa batas memperlambat
                // pencarian cache justru di perangkat yang paling lemah.
                chatMessageDao.trimTo(Constants.CHAT_HISTORY_LIMIT)
            }
        }
    }

    override suspend fun findCachedAnswer(
        questionKey: String,
        regionCode: String?,
        notBefore: Long,
    ): String? = withContext(dispatchers.io) {
        runSafely("cari jawaban tersimpan") {
            chatMessageDao.findAnswer(questionKey, regionCode, notBefore)
        }
    }

    override fun observeHistory(limit: Int): Flow<List<ChatMessage>> =
        chatMessageDao.observeRecent(limit).map { entities ->
            entities.mapNotNull { it.toDomain() }
        }

    /**
     * [CancellationException] dilempar ulang, sisanya dicatat lalu null.
     *
     * Pola yang sama dengan `callOrNull` di [WeatherRepositoryImpl]: menelan
     * pembatalan coroutine akan merusak structured concurrency tanpa gejala
     * yang terlihat.
     */
    private inline fun <T : Any> runSafely(what: String, block: () -> T?): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Gagal: %s", what)
        null
    }

    private fun ChatMessage.toEntity() = ChatMessageEntity(
        role = role.name,
        text = text,
        questionKey = questionKey,
        regionCode = regionCode,
        createdAt = createdAt,
    )

    /** Baris dengan peran tak dikenal (dari versi app lain) dibuang, bukan crash. */
    private fun ChatMessageEntity.toDomain(): ChatMessage? {
        val parsedRole = ChatRole.entries.firstOrNull { it.name == role }
        if (parsedRole == null) {
            Timber.w("Baris riwayat dengan peran tidak dikenal: %s", role)
            return null
        }
        return ChatMessage(
            id = id,
            role = parsedRole,
            text = text,
            questionKey = questionKey,
            regionCode = regionCode,
            createdAt = createdAt,
        )
    }
}
