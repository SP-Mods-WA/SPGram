package com.spmods.spgram.domain.repository

import com.spmods.spgram.domain.models.FoundStoryViewersModel
import com.spmods.spgram.domain.models.StoryModel
import com.spmods.spgram.domain.models.StoryPrivacy
import kotlinx.coroutines.flow.Flow

interface StoryRepository {

    /** Get stories posted to the chat page (profile stories) */
    suspend fun getChatPageStories(
        chatId: Long,
        fromStoryId: Int = 0,
        limit: Int = 20
    ): List<StoryModel>

    /** Get active stories for another user's chat (use for non-own profiles) */
    suspend fun getActiveStories(
        chatId: Long
    ): List<StoryModel>

    /**
     * Returns a Flow that emits updated active stories whenever TDLib fires
     * updateChatActiveStories for the given chatId.
     * Call getActiveStories(chatId) first to trigger TDLib to fetch from server.
     */
    fun observeActiveStories(chatId: Long): Flow<List<StoryModel>>

    /** Get a single story by its poster chat id and story id (e.g. to open a story reply preview from a chat). */
    suspend fun getStory(posterChatId: Long, storyId: Int): StoryModel?

    /** Post a photo story */
    suspend fun postPhotoStory(
        chatId: Long,
        photoPath: String,
        caption: String = "",
        activePeriodSeconds: Int = 86400,
        privacy: StoryPrivacy = StoryPrivacy.Everyone()
    ): StoryModel?

    /** Post a video story */
    suspend fun postVideoStory(
        chatId: Long,
        videoPath: String,
        thumbnailPath: String = "",
        caption: String = "",
        activePeriodSeconds: Int = 86400,
        privacy: StoryPrivacy = StoryPrivacy.Everyone()
    ): StoryModel?

    /** Delete a story */
    suspend fun deleteStory(chatId: Long, storyId: Int)

    /** Informs TDLib that a story is opened and is being viewed by the user — marks it as viewed. */
    suspend fun openStory(posterChatId: Long, storyId: Int)

    /** Informs TDLib that a story is closed by the user. Call when leaving the viewer. */
    suspend fun closeStory(posterChatId: Long, storyId: Int)

    /** Sets (or clears, when [emoji] is null) the current user's reaction on a story. */
    suspend fun setStoryReaction(posterChatId: Long, storyId: Int, emoji: String?)

    /**
     * Returns a Flow of chat IDs that currently have active stories.
     * Emits whenever TDLib fires UpdateChatActiveStories.
     */
    fun observeAllActiveStoryChats(): Flow<Map<Long, String>>

    /** Gets the list of users who viewed one of the current user's own stories. */
    suspend fun getStoryViewers(
        storyId: Int,
        offset: String = "",
        limit: Int = 50
    ): FoundStoryViewersModel

    /** Returns a t.me link for a story. Returns null on failure. */
    suspend fun getStoryLink(posterChatId: Long, storyId: Int): String?

    /** Reports a story to Telegram moderators. */
    suspend fun reportStory(posterChatId: Long, storyId: Int)

    /** Toggles story notifications for a chat (Do Not Notify About Stories). */
    suspend fun toggleStoryNotifications(chatId: Long, mute: Boolean)
}
