package com.spmods.spgram.data.repository

import org.drinkless.tdlib.TdApi
import com.spmods.spgram.data.core.coRunCatching
import com.spmods.spgram.data.gateway.TelegramGateway
import com.spmods.spgram.data.infra.FileDownloadQueue
import com.spmods.spgram.data.infra.FileObserverHub
import com.spmods.spgram.data.mapper.isValidFilePath
import com.spmods.spgram.domain.models.StoryContentModel
import com.spmods.spgram.domain.models.StoryModel
import com.spmods.spgram.domain.repository.StoryRepository
import kotlinx.coroutines.withTimeoutOrNull

class StoryRepositoryImpl(
    private val gateway: TelegramGateway,
    private val fileQueue: FileDownloadQueue,
    private val fileObserverHub: FileObserverHub
) : StoryRepository {

    override suspend fun getChatPageStories(
        chatId: Long,
        fromStoryId: Int,
        limit: Int
    ): List<StoryModel> {
        val req = TdApi.GetChatPostedToChatPageStories().apply {
            this.chatId = chatId
            this.fromStoryId = fromStoryId
            this.limit = limit
        }
        val result = coRunCatching {
            gateway.execute(req) as? TdApi.Stories
        }.getOrNull() ?: return emptyList()

        return result.stories.mapNotNull { it.toModel() }
    }

    override suspend fun postPhotoStory(
        chatId: Long,
        photoPath: String,
        caption: String,
        activePeriodSeconds: Int
    ): StoryModel? {
        val content = TdApi.InputStoryContentPhoto(
            TdApi.InputFileLocal(photoPath),
            intArrayOf()
        )
        return postStory(chatId, content, caption, activePeriodSeconds)
    }

    override suspend fun postVideoStory(
        chatId: Long,
        videoPath: String,
        thumbnailPath: String,
        caption: String,
        activePeriodSeconds: Int
    ): StoryModel? {
        val thumbnail = if (thumbnailPath.isNotEmpty()) {
            TdApi.InputThumbnail(TdApi.InputFileLocal(thumbnailPath), 720, 1280)
        } else null

        val content = TdApi.InputStoryContentVideo(
            TdApi.InputFileLocal(videoPath),
            intArrayOf(),
            0.0,
            0.0,
            false,
            thumbnail
        )
        return postStory(chatId, content, caption, activePeriodSeconds)
    }

    override suspend fun deleteStory(chatId: Long, storyId: Int) {
        coRunCatching {
            gateway.execute(TdApi.DeleteStory(chatId, storyId))
        }
    }

    // -- private helpers --

    private suspend fun postStory(
        chatId: Long,
        content: TdApi.InputStoryContent,
        caption: String,
        activePeriodSeconds: Int
    ): StoryModel? {
        val req = TdApi.PostStory().apply {
            this.chatId = chatId
            this.content = content
            this.areas = null
            this.caption = TdApi.FormattedText(caption, emptyArray())
            this.privacySettings = TdApi.StoryPrivacySettingsEveryone()
            this.albumIds = intArrayOf()
            this.activePeriod = activePeriodSeconds
            this.fromStoryFullId = null
            this.isPostedToChatPage = true
            this.protectContent = false
        }
        val result = coRunCatching {
            gateway.execute(req) as? TdApi.Story
        }.getOrNull() ?: return null
        return result.toModel()
    }

    private suspend fun TdApi.Story.toModel(): StoryModel? {
        val contentModel = resolveContent(content) ?: return null
        return StoryModel(
            id = id,
            posterChatId = posterChatId,
            date = date,
            isPostedToChatPage = isPostedToChatPage,
            canBeDeleted = canBeDeleted,
            canBeForwarded = canBeForwarded,
            content = contentModel,
            caption = caption?.text.orEmpty()
        )
    }

    private suspend fun resolveContent(content: TdApi.StoryContent): StoryContentModel? {
        return when (content) {
            is TdApi.StoryContentPhoto -> {
                val best = content.photo.sizes.maxByOrNull { it.width * it.height }?.photo
                    ?: return StoryContentModel.Unsupported
                val path = resolveFilePath(best) ?: return null
                StoryContentModel.Photo(path)
            }
            is TdApi.StoryContentVideo -> {
                val videoFile = content.video.video
                val thumbFile = content.video.thumbnail?.file
                val videoPath = resolveFilePath(videoFile) ?: return null
                val thumbPath = thumbFile?.let { resolveFilePath(it) }.orEmpty()
                StoryContentModel.Video(videoPath, thumbPath)
            }
            else -> StoryContentModel.Unsupported
        }
    }

    private suspend fun resolveFilePath(file: TdApi.File): String? {
        val direct = file.local.path.takeIf { isValidFilePath(it) }
        if (direct != null) return direct

        val fileId = file.id.takeIf { it != 0 } ?: return null
        fileQueue.enqueue(
            fileId = fileId,
            priority = STORY_DOWNLOAD_PRIORITY,
            type = FileDownloadQueue.DownloadType.DEFAULT,
            synchronous = false
        )
        withTimeoutOrNull(FILE_DOWNLOAD_TIMEOUT_MS) {
            coRunCatching { fileQueue.waitForDownload(fileId).await() }
        }
        val updated = fileObserverHub.getCachedFile(fileId)
            ?: coRunCatching { gateway.execute(TdApi.GetFile(fileId)) }.getOrNull()
            ?: return null
        return if (updated.local.isDownloadingCompleted) {
            updated.local.path.takeIf { isValidFilePath(it) }
        } else null
    }

    companion object {
        private const val STORY_DOWNLOAD_PRIORITY = 28
        private const val FILE_DOWNLOAD_TIMEOUT_MS = 20_000L
    }
}
