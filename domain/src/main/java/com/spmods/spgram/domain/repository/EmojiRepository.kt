package com.spmods.spgram.domain.repository

import kotlinx.coroutines.flow.Flow
import com.spmods.spgram.domain.models.RecentEmojiModel
import com.spmods.spgram.domain.models.StickerModel

interface EmojiRepository {
    val recentEmojis: Flow<List<RecentEmojiModel>>

    suspend fun getDefaultEmojis(): List<String>
    suspend fun searchEmojis(query: String): List<String>
    suspend fun searchCustomEmojis(query: String): List<StickerModel>
    suspend fun addRecentEmoji(recentEmoji: RecentEmojiModel)
    suspend fun clearRecentEmojis()
    suspend fun getMessageAvailableReactions(chatId: Long, messageId: Long): List<String>
}