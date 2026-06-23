package com.spmods.spgram.domain.models

data class StoryModel(
    val id: Int,
    val posterChatId: Long,
    val date: Int,
    val isPostedToChatPage: Boolean = false,
    val canBeDeleted: Boolean = false,
    val canBeForwarded: Boolean = false,
    val content: StoryContentModel,
    val caption: String = ""
)

sealed class StoryContentModel {
    data class Photo(val filePath: String) : StoryContentModel()
    data class Video(val filePath: String, val thumbnailPath: String = "") : StoryContentModel()
    object Unsupported : StoryContentModel()
}
