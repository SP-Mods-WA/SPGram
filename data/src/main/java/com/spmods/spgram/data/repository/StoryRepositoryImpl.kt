package com.spmods.spgram.data.repository

import org.drinkless.tdlib.TdApi
import com.spmods.spgram.data.core.coRunCatching
import com.spmods.spgram.data.gateway.TelegramGateway
import com.spmods.spgram.data.infra.FileDownloadQueue
import com.spmods.spgram.data.infra.FileObserverHub
import com.spmods.spgram.data.mapper.isValidFilePath
import com.spmods.spgram.domain.models.FoundStoryViewersModel
import com.spmods.spgram.domain.models.StoryContentModel
import com.spmods.spgram.domain.models.StoryModel
import com.spmods.spgram.domain.models.StoryPrivacy
import com.spmods.spgram.domain.models.StoryViewerModel
import com.spmods.spgram.domain.repository.StoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class StoryRepositoryImpl(
    private val gateway: TelegramGateway,
    private val fileQueue: FileDownloadQueue,
    private val fileObserverHub: FileObserverHub
) : StoryRepository {

    override suspend fun getActiveStories(chatId: Long): List<StoryModel> {
        // Step 1: get active (unexpired) stories via GetChatActiveStories
        val activeResult = coRunCatching {
            gateway.execute(TdApi.GetChatActiveStories(chatId)) as? TdApi.ChatActiveStories
        }.getOrNull()

        val posterChatId = activeResult?.chatId?.takeIf { it != 0L } ?: chatId
        val maxReadStoryId = activeResult?.maxReadStoryId ?: 0

        val activeStories = activeResult?.stories?.mapNotNull { storyInfo ->
            coRunCatching {
                gateway.execute(TdApi.GetStory(posterChatId, storyInfo.storyId, false)) as? TdApi.Story
            }.getOrNull()?.toModel(maxReadStoryId = maxReadStoryId)
        } ?: emptyList()

        // Step 2: also fetch stories pinned to profile page (posted to chat page)
        // Official Telegram shows both active + pinned profile stories on profile page
        val pageStories = getChatPageStories(posterChatId)

        // Merge both lists, deduplicate by story id
        val seen = mutableSetOf<Int>()
        return (activeStories + pageStories).filter { seen.add(it.id) }
    }

    override fun observeActiveStories(chatId: Long): Flow<List<StoryModel>> = channelFlow {
        // Listen for TDLib updateChatActiveStories in background coroutine
        launch {
            gateway.updates
                .filterIsInstance<TdApi.UpdateChatActiveStories>()
                .collect { update ->
                    if (update.activeStories.chatId == chatId) {
                        val posterChatId = update.activeStories.chatId.takeIf { it != 0L } ?: chatId
                        val maxReadStoryId = update.activeStories.maxReadStoryId
                        val stories = update.activeStories.stories.mapNotNull { storyInfo ->
                            coRunCatching {
                                gateway.execute(TdApi.GetStory(posterChatId, storyInfo.storyId, false)) as? TdApi.Story
                            }.getOrNull()?.toModel(maxReadStoryId = maxReadStoryId)
                        }
                        val pageStories = getChatPageStories(posterChatId)
                        val seen = mutableSetOf<Int>()
                        send((stories + pageStories).filter { seen.add(it.id) })
                    }
                }
        }

        // Re-emit stories when any story file finishes downloading (thumbnails update)
        launch {
            fileObserverHub.fileStates
                .filter { it.isDownloaded }
                .collect {
                    val refreshed = getActiveStories(chatId)
                    if (refreshed.isNotEmpty()) send(refreshed)
                }
        }

        // Trigger TDLib to fetch from server — emit immediately (paths may be empty initially)
        val initial = getActiveStories(chatId)
        send(initial)

        awaitClose()
    }

    override fun observeAllActiveStoryChats(): kotlinx.coroutines.flow.Flow<Map<Long, String>> =
        gateway.updates
            .filterIsInstance<TdApi.UpdateChatActiveStories>()
            .runningFold(mutableMapOf<Long, String>()) { acc, update ->
                val chatId = update.activeStories.chatId
                val newAcc = acc.toMutableMap()
                when {
                    update.activeStories.stories.isEmpty() -> newAcc.remove(chatId)
                    update.activeStories.maxReadStoryId >= (update.activeStories.stories.maxOfOrNull { it.storyId } ?: 0) ->
                        newAcc[chatId] = "READ"
                    else -> newAcc[chatId] = "UNREAD"
                }
                newAcc
            }

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

    override suspend fun getStory(posterChatId: Long, storyId: Int): StoryModel? {
        val story = coRunCatching {
            gateway.execute(TdApi.GetStory(posterChatId, storyId, false)) as? TdApi.Story
        }.getOrNull() ?: return null
        return story.toModel(waitForDownload = true)
    }

    override suspend fun postPhotoStory(
        chatId: Long,
        photoPath: String,
        caption: String,
        activePeriodSeconds: Int,
        privacy: StoryPrivacy
    ): StoryModel? {
        val content = TdApi.InputStoryContentPhoto(
            TdApi.InputFileLocal(photoPath),
            intArrayOf()
        )
        return postStory(chatId, content, caption, activePeriodSeconds, privacy)
    }

    override suspend fun postVideoStory(
        chatId: Long,
        videoPath: String,
        thumbnailPath: String,
        caption: String,
        activePeriodSeconds: Int,
        privacy: StoryPrivacy
    ): StoryModel? {
        val content = TdApi.InputStoryContentVideo(
            TdApi.InputFileLocal(videoPath),
            intArrayOf(),
            0.0,
            0.0,
            false
        )
        return postStory(chatId, content, caption, activePeriodSeconds, privacy)
    }

    override suspend fun deleteStory(chatId: Long, storyId: Int) {
        coRunCatching {
            gateway.execute(TdApi.DeleteStory(chatId, storyId))
        }
    }

    override suspend fun openStory(posterChatId: Long, storyId: Int) {
        coRunCatching {
            gateway.execute(TdApi.OpenStory(posterChatId, storyId))
        }
    }

    override suspend fun closeStory(posterChatId: Long, storyId: Int) {
        coRunCatching {
            gateway.execute(TdApi.CloseStory(posterChatId, storyId))
        }
    }

    override suspend fun setStoryReaction(posterChatId: Long, storyId: Int, emoji: String?) {
        coRunCatching {
            val reactionType = emoji?.let { TdApi.ReactionTypeEmoji(it) }
            gateway.execute(TdApi.SetStoryReaction(posterChatId, storyId, reactionType, true))
        }
    }

    override suspend fun getStoryViewers(
        storyId: Int,
        offset: String,
        limit: Int
    ): FoundStoryViewersModel {
        val req = TdApi.GetStoryInteractions().apply {
            this.storyId = storyId
            this.query = ""
            this.onlyContacts = false
            this.preferForwards = false
            this.preferWithReaction = false
            this.offset = offset
            this.limit = limit
        }
        val result = coRunCatching {
            gateway.execute(req) as? TdApi.StoryInteractions
        }.getOrNull() ?: return FoundStoryViewersModel(0, emptyList(), "")

        val viewers = result.interactions.mapNotNull { interaction ->
            val userId = (interaction.actorId as? TdApi.MessageSenderUser)?.userId ?: return@mapNotNull null
            val user = coRunCatching {
                gateway.execute(TdApi.GetUser(userId)) as? TdApi.User
            }.getOrNull() ?: return@mapNotNull null
            val reactionType = (interaction.type as? TdApi.StoryInteractionTypeView)?.chosenReactionType
            StoryViewerModel(
                userId = userId,
                name = listOfNotNull(user.firstName.ifBlank { null }, user.lastName.ifBlank { null })
                    .joinToString(" ")
                    .ifBlank { "Unknown" },
                avatarPath = user.profilePhoto?.small?.local?.path?.takeIf { isValidFilePath(it) },
                viewedAtSeconds = interaction.interactionDate,
                reactionEmoji = (reactionType as? TdApi.ReactionTypeEmoji)?.emoji
            )
        }
        return FoundStoryViewersModel(
            totalCount = result.totalCount,
            viewers = viewers,
            nextOffset = result.nextOffset
        )
    }

    // -- private helpers --

    private suspend fun postStory(
        chatId: Long,
        content: TdApi.InputStoryContent,
        caption: String,
        activePeriodSeconds: Int,
        privacy: StoryPrivacy
    ): StoryModel? {
        val req = TdApi.PostStory().apply {
            this.chatId = chatId
            this.content = content
            this.areas = null
            this.caption = TdApi.FormattedText(caption, emptyArray())
            this.privacySettings = when (privacy) {
                is StoryPrivacy.Everyone      -> TdApi.StoryPrivacySettingsEveryone(
                    privacy.exceptUserIds.toLongArray()
                )
                is StoryPrivacy.Contacts      -> TdApi.StoryPrivacySettingsContacts(
                    privacy.exceptUserIds.toLongArray()
                )
                is StoryPrivacy.CloseFriends  -> TdApi.StoryPrivacySettingsCloseFriends()
                is StoryPrivacy.SelectedUsers -> TdApi.StoryPrivacySettingsSelectedUsers(
                    privacy.userIds.toLongArray()
                )
            }
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

    private suspend fun TdApi.Story.toModel(waitForDownload: Boolean = false, maxReadStoryId: Int = Int.MAX_VALUE): StoryModel? {
        // resolveContent never returns null now (uses fast path), only Unsupported for unknown types
        val contentModel = resolveContent(content, waitForDownload) ?: StoryContentModel.Unsupported
        return StoryModel(
            id = id,
            posterChatId = posterChatId,
            date = date,
            isPostedToChatPage = isPostedToChatPage,
            canBeDeleted = canBeDeleted,
            canBeForwarded = canBeForwarded,
            content = contentModel,
            caption = caption?.text.orEmpty(),
            isViewed = id <= maxReadStoryId,
            chosenReactionEmoji = (chosenReactionType as? TdApi.ReactionTypeEmoji)?.emoji,
            viewCount = interactionInfo?.viewCount ?: 0
        )
    }

    private suspend fun resolveContent(content: TdApi.StoryContent, waitForDownload: Boolean = false): StoryContentModel? {
        return when (content) {
            is TdApi.StoryContentPhoto -> {
                val best = content.photo.sizes.maxByOrNull { it.width * it.height }?.photo
                    ?: return StoryContentModel.Unsupported
                val path = if (waitForDownload) {
                    resolveFilePath(best).orEmpty()
                } else {
                    resolveFilePathFast(best)
                }
                StoryContentModel.Photo(path)
            }
            is TdApi.StoryContentVideo -> {
                val videoFile = content.video.video
                val thumbFile = content.video.thumbnail?.file
                val videoPath = if (waitForDownload) {
                    resolveFilePath(videoFile).orEmpty()
                } else {
                    resolveFilePathFast(videoFile)
                }
                val thumbPath = thumbFile?.let {
                    if (waitForDownload) resolveFilePath(it).orEmpty() else resolveFilePathFast(it)
                }.orEmpty()
                StoryContentModel.Video(videoPath, thumbPath)
            }
            else -> StoryContentModel.Unsupported
        }
    }

    // Returns immediately with cached path (or empty string) and enqueues download in background
    private fun resolveFilePathFast(file: TdApi.File): String {
        val direct = file.local.path.takeIf { isValidFilePath(it) }
        if (direct != null) return direct
        val fileId = file.id.takeIf { it != 0 } ?: return ""
        fileQueue.enqueue(
            fileId = fileId,
            priority = STORY_DOWNLOAD_PRIORITY,
            type = FileDownloadQueue.DownloadType.DEFAULT,
            synchronous = false
        )
        return fileObserverHub.getCachedPath(fileId) ?: ""
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
        private const val STORY_DOWNLOAD_PRIORITY = 32
        private const val FILE_DOWNLOAD_TIMEOUT_MS = 20_000L
    }
}
