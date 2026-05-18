package com.spmods.spgram.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import com.spmods.spgram.domain.models.AttachMenuBotModel
import com.spmods.spgram.domain.models.ChatViewportCacheEntry
import com.spmods.spgram.domain.models.FolderModel
import com.spmods.spgram.domain.models.GifModel
import com.spmods.spgram.domain.models.RecentEmojiModel
import com.spmods.spgram.domain.models.StickerSetModel

interface CacheProvider {
    val recentEmojis: Flow<List<RecentEmojiModel>>
    val searchHistory: Flow<List<Long>>
    val chatFolders: StateFlow<List<FolderModel>>
    val attachBots: StateFlow<List<AttachMenuBotModel>>
    val cachedSimCountryIso: StateFlow<String?>
    val savedGifs: StateFlow<List<GifModel>>
    val installedStickerSets: StateFlow<List<StickerSetModel>>
    val customEmojiStickerSets: StateFlow<List<StickerSetModel>>

    fun addRecentEmoji(recentEmoji: RecentEmojiModel)
    fun clearRecentEmojis()
    fun setRecentEmojis(emojis: List<RecentEmojiModel>)

    fun addSearchChatId(chatId: Long)
    fun removeSearchChatId(chatId: Long)
    fun clearSearchHistory()
    fun setSearchHistory(history: List<Long>)

    fun setChatFolders(folders: List<FolderModel>)
    fun setAttachBots(bots: List<AttachMenuBotModel>)
    fun setCachedSimCountryIso(iso: String?)

    fun saveChatScrollPosition(chatId: Long, messageId: Long)
    fun getChatScrollPosition(chatId: Long): Long

    fun saveChatViewport(chatId: Long, threadId: Long?, viewport: ChatViewportCacheEntry)
    fun getChatViewport(chatId: Long, threadId: Long?): ChatViewportCacheEntry?

    fun setSavedGifs(gifs: List<GifModel>)

    fun setInstalledStickerSets(sets: List<StickerSetModel>)
    fun setCustomEmojiStickerSets(sets: List<StickerSetModel>)

    fun clearAll()
}
