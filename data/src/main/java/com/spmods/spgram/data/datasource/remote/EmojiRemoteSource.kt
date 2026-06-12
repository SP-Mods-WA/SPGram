package com.spmods.spgram.data.datasource.remote

import com.spmods.spgram.domain.models.StickerModel

interface EmojiRemoteSource {
    suspend fun getEmojiCategories(): List<String>
    suspend fun getMessageAvailableReactions(chatId: Long, messageId: Long): List<String>
    suspend fun searchEmojis(query: String): List<String>
    suspend fun searchCustomEmojis(query: String): List<StickerModel>

    /**
     * Fetches the animated select-animation sticker for the given emoji reaction.
     * Calls TdApi.GetEmojiReaction and returns the selectAnimation sticker
     * (the one that plays in the reaction picker row — same as original Telegram).
     */
    suspend fun getReactionSticker(emoji: String): StickerModel?
}
