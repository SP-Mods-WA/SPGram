package com.spmods.spgram.domain.models

data class StoryModel(
    val id: Int,
    val posterChatId: Long,
    val date: Int,
    val isPostedToChatPage: Boolean = false,
    val canBeDeleted: Boolean = false,
    val canBeForwarded: Boolean = false,
    val content: StoryContentModel,
    val caption: String = "",
    val isViewed: Boolean = false,
    val chosenReactionEmoji: String? = null,
    val viewCount: Int = 0,
    /** How long (seconds) this story is active. Default 86400 = 24 hours. */
    val activePeriodSeconds: Int = 86400,
) {
    /** Unix timestamp (seconds) when this story expires. */
    val expiresAtSeconds: Int get() = date + activePeriodSeconds
}

/** A single viewer of one of the current user's own stories, for the "viewed by" list. */
data class StoryViewerModel(
    val userId: Long,
    val name: String,
    val avatarPath: String? = null,
    val viewedAtSeconds: Int,
    val reactionEmoji: String? = null
)

sealed class StoryPrivacy {
    /** Visible to everyone. Optionally exclude specific user IDs. */
    data class Everyone(val exceptUserIds: List<Long> = emptyList()) : StoryPrivacy()

    /** Visible to contacts only. Optionally exclude specific contact IDs. */
    data class Contacts(val exceptUserIds: List<Long> = emptyList()) : StoryPrivacy()

    /** Visible to close friends only. */
    object CloseFriends : StoryPrivacy()

    /** Visible to selected users only. */
    data class SelectedUsers(val userIds: List<Long> = emptyList()) : StoryPrivacy()
}

data class FoundStoryViewersModel(
    val totalCount: Int,
    val viewers: List<StoryViewerModel>,
    val nextOffset: String
)

sealed class StoryContentModel {
    data class Photo(val filePath: String) : StoryContentModel()
    data class Video(val filePath: String, val thumbnailPath: String = "") : StoryContentModel()
    object Unsupported : StoryContentModel()
}
