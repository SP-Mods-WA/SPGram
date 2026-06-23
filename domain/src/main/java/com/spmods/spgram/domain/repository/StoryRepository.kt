package com.spmods.spgram.domain.repository

import com.spmods.spgram.domain.models.StoryModel

interface StoryRepository {

    /** Get stories posted to the chat page (profile stories) */
    suspend fun getChatPageStories(
        chatId: Long,
        fromStoryId: Int = 0,
        limit: Int = 20
    ): List<StoryModel>

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
