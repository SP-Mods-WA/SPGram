package com.spmods.spgram.domain.repository

import com.spmods.spgram.domain.models.StoryModel
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
        activePeriodSeconds: Int = 86400
    ): StoryModel?

    /** Post a video story */
    suspend fun postVideoStory(
        chatId: Long,
        videoPath: String,
        thumbnailPath: String = "",
        caption: String = "",
        activePeriodSeconds: Int = 86400
    ): StoryModel?

    /** Delete a story */
    suspend fun deleteStory(chatId: Long, storyId: Int)
}
