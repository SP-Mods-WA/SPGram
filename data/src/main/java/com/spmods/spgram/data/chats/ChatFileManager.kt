package com.spmods.spgram.data.chats

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import com.spmods.spgram.core.DispatcherProvider
import com.spmods.spgram.data.core.coRunCatching
import com.spmods.spgram.data.gateway.TelegramGateway
import com.spmods.spgram.data.infra.FileDownloadQueue
import com.spmods.spgram.data.infra.FileUpdateHandler
import java.util.*
import java.util.concurrent.ConcurrentHashMap


class ChatFileManager(
    private val gateway: TelegramGateway,
    private val dispatchers: DispatcherProvider,
    private val fileQueue: FileDownloadQueue,
    private val fileUpdateHandler: FileUpdateHandler,
    private val scope: CoroutineScope,
    private val onUpdate: () -> Unit
) {
    private val downloadingFiles: MutableSet<Int> = Collections.newSetFromMap(ConcurrentHashMap())
    private val loadingEmojis: MutableSet<Long> = Collections.newSetFromMap(ConcurrentHashMap())
    private val filePaths = ConcurrentHashMap<Int, String>()
    private val chatPhotoIds = ConcurrentHashMap<Int, Long>()
    private val trackedFileIds = Collections.newSetFromMap(ConcurrentHashMap<Int, Boolean>())

    fun getFilePath(fileId: Int): String? = filePaths[fileId]
    fun getEmojiPath(emojiId: Long): String? = fileUpdateHandler.customEmojiPaths[emojiId]
    fun getChatIdByPhotoId(fileId: Int): Long? = chatPhotoIds[fileId]

    // Holds the chat ID(s) for files registered via FileMessageRegistry (normal
    // message photos/videos), discovered the first time we see a matching UpdateFile.
    // ✅ FIX: previously handleFileUpdate() only returned true (i.e. only triggered a
    // UI refresh) for custom emoji, chat-list photos, and explicitly "tracked" files.
    // Regular chat message photos/videos are registered in FileMessageRegistry
    // (via registerFileForMessage / TdFileHelper.registerCachedFile) but that registry
    // was never consulted here, so completing a photo/video download in an open chat
    // never triggered onTriggerUpdate(chatId) — the bubble stayed at thumbnail size
    // until the chat screen was recreated (e.g. app restart) and re-mapped messages
    // fresh from TDLib/DB. Now we also check the message file registry.
    private var messageFileRegistry: com.spmods.spgram.data.infra.FileMessageRegistry? = null

    fun registerChatPhoto(fileId: Int, chatId: Long) {
        chatPhotoIds[fileId] = chatId
    }

    fun registerTrackedFile(fileId: Int) {
        if (fileId != 0) trackedFileIds.add(fileId)
    }

    /** Wires the message<->file registry so message photo/video downloads trigger UI refresh. */
    fun attachMessageFileRegistry(registry: com.spmods.spgram.data.infra.FileMessageRegistry) {
        messageFileRegistry = registry
    }

    fun handleFileUpdate(file: TdApi.File): Boolean {
        if (file.local.isDownloadingCompleted) {
            filePaths[file.id] = file.local.path
            return handleFileUpdated(file.id, file.local.path)
        }
        return false
    }

    private fun handleFileUpdated(fileId: Int, path: String): Boolean {
        if (path.isEmpty()) return false
        var updated = false
        fileUpdateHandler.fileIdToCustomEmojiId[fileId]?.let { emojiId ->
            fileUpdateHandler.customEmojiPaths[emojiId] = path
            updated = true
        }
        if (chatPhotoIds.containsKey(fileId)) updated = true
        if (trackedFileIds.remove(fileId)) updated = true
        if (messageFileRegistry?.getMessages(fileId)?.isNotEmpty() == true) updated = true
        return updated
    }

    /**
     * Returns the chat ID of any message this file belongs to (normal chat photo/video/etc),
     * so the caller can target onTriggerUpdate at the right chat even when it's not in
     * chatPhotoIds. Falls back to null if the file isn't a registered message file.
     */
    fun getMessageChatIdByFileId(fileId: Int): Long? =
        messageFileRegistry?.getMessages(fileId)?.firstOrNull()?.first

    fun downloadFile(fileId: Int, priority: Int, offset: Long = 0, limit: Long = 0, synchronous: Boolean = true) {
        if (fileId == 0) return
        val effectivePriority = if (priority <= 1) 16 else priority
        fileQueue.enqueue(fileId, effectivePriority, FileDownloadQueue.DownloadType.DEFAULT, offset, limit, synchronous)
        if (synchronous) {
            scope.launch(dispatchers.io) {
                coRunCatching {
                    fileQueue.waitForDownload(fileId).await()
                }
            }
        }
    }

    fun loadEmoji(emojiId: Long) {
        if (emojiId == 0L || fileUpdateHandler.customEmojiPaths.containsKey(emojiId)) return
        if (loadingEmojis.add(emojiId)) {
            scope.launch(dispatchers.io) {
                coRunCatching {
                    val result = gateway.execute(TdApi.GetCustomEmojiStickers(longArrayOf(emojiId)))
                    val sticker = result.stickers.firstOrNull() ?: return@launch
                    val file = sticker.sticker
                    val path = file.local.path.ifEmpty { filePaths[file.id] ?: "" }
                    fileUpdateHandler.fileIdToCustomEmojiId[file.id] = emojiId
                    if (path.isNotEmpty()) {
                        fileUpdateHandler.customEmojiPaths[emojiId] = path
                        onUpdate()
                    } else {
                        downloadFile(file.id, 32)
                    }
                }
                loadingEmojis.remove(emojiId)
            }
        }
    }
}
