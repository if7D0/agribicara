package com.agribicara.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.agribicara.app.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Insert
    suspend fun insert(entity: ChatMessageEntity): Long

    @Query("SELECT * FROM chat_message ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ChatMessageEntity>>

    /**
     * Jawaban tersimpan untuk pertanyaan yang setara.
     *
     * Wilayah IKUT dicocokkan, termasuk kasus keduanya null: jawaban yang
     * di-grounding cuaca desa lain akan menyesatkan bila dipakai ulang.
     * `notBefore` menyaring jawaban basi — cuaca berubah, jadi jawaban lama
     * bisa berbahaya, bukan sekadar usang.
     */
    @Query(
        """
        SELECT text FROM chat_message
        WHERE role = 'ASSISTANT'
          AND questionKey = :questionKey
          AND createdAt >= :notBefore
          AND ((regionCode IS NULL AND :regionCode IS NULL) OR regionCode = :regionCode)
        ORDER BY createdAt DESC
        LIMIT 1
        """,
    )
    suspend fun findAnswer(questionKey: String, regionCode: String?, notBefore: Long): String?

    /** Membuang riwayat lama agar tabel tidak tumbuh tanpa batas. */
    @Query(
        """
        DELETE FROM chat_message WHERE id NOT IN (
            SELECT id FROM chat_message ORDER BY createdAt DESC LIMIT :keep
        )
        """,
    )
    suspend fun trimTo(keep: Int)
}
