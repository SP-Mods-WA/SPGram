@file:Suppress("SpellCheckingInspection")

package com.spmods.spgram.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

class TelegramManager(private val context: Context) : Client.ResultHandler {

    companion object {
        init { System.loadLibrary("tdjni") }
    }

    private val scope  = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var client: Client? = null

    // Auth
    private val _authState = MutableStateFlow<TdApi.AuthorizationState?>(null)
    val authState: StateFlow<TdApi.AuthorizationState?> = _authState.asStateFlow()

    // Chat list
    private val _chatIds = MutableStateFlow<List<Long>>(emptyList())
    val chatIds: StateFlow<List<Long>> = _chatIds.asStateFlow()

    private val _chats = MutableStateFlow<Map<Long, TdApi.Chat>>(emptyMap())
    val chats: StateFlow<Map<Long, TdApi.Chat>> = _chats.asStateFlow()

    // Files
    private val _downloadedFiles = MutableStateFlow<Map<Int, String>>(emptyMap())
    val downloadedFiles: StateFlow<Map<Int, String>> = _downloadedFiles.asStateFlow()

    // My profile
    private val _myName      = MutableStateFlow("S")
    val myName: StateFlow<String> = _myName.asStateFlow()

    private val _myPhotoPath = MutableStateFlow<String?>(null)
    val myPhotoPath: StateFlow<String?> = _myPhotoPath.asStateFlow()

    private var myUserId: Long = 0L

    // ── Chat Screen ──────────────────────────────────────────
    // Messages per chat
    private val _messages = MutableStateFlow<Map<Long, List<TdApi.Message>>>(emptyMap())
    val messages: StateFlow<Map<Long, List<TdApi.Message>>> = _messages.asStateFlow()

    // Typing status per chat  (chatId → text)
    private val _typingStatus = MutableStateFlow<Map<Long, String>>(emptyMap())
    val typingStatus: StateFlow<Map<Long, String>> = _typingStatus.asStateFlow()

    // Read outbox (last message id the OTHER person read) per chat
    private val _outboxReadId = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val outboxReadId: StateFlow<Map<Long, Long>> = _outboxReadId.asStateFlow()

    // Currently open chat
    private val _openChatId = MutableStateFlow<Long?>(null)
    val openChatId: StateFlow<Long?> = _openChatId.asStateFlow()

    private val apiId   = 35214748
    private val apiHash = "2bc7633d816864cd22fe4173cedc67c9"

    fun initClient() {
        Client.execute(TdApi.SetLogVerbosityLevel(0))
        client = Client.create(this, null, null)
    }

    override fun onResult(result: TdApi.Object) {
        when (result.constructor) {

            TdApi.UpdateAuthorizationState.CONSTRUCTOR ->
                onAuthorizationStateUpdated((result as TdApi.UpdateAuthorizationState).authorizationState)

            TdApi.UpdateNewChat.CONSTRUCTOR -> {
                val chat = (result as TdApi.UpdateNewChat).chat
                _chats.value = _chats.value + (chat.id to chat)
            }

            TdApi.UpdateChatLastMessage.CONSTRUCTOR -> {
                val upd      = result as TdApi.UpdateChatLastMessage
                val existing = _chats.value[upd.chatId] ?: return
                val updated  = TdApi.Chat().also { c ->
                    c.id          = existing.id
                    c.title       = existing.title
                    c.photo       = existing.photo
                    c.lastMessage = upd.lastMessage
                    c.unreadCount = existing.unreadCount
                }
                _chats.value = _chats.value + (upd.chatId to updated)
            }

            // New message → update chat preview + append to open chat
            TdApi.UpdateNewMessage.CONSTRUCTOR -> {
                val msg    = (result as TdApi.UpdateNewMessage).message
                val chatId = msg.chatId
                // Update chat list preview
                val existing = _chats.value[chatId]
                if (existing != null) {
                    val updated = TdApi.Chat().also { c ->
                        c.id          = existing.id
                        c.title       = existing.title
                        c.photo       = existing.photo
                        c.lastMessage = msg
                        c.unreadCount = existing.unreadCount + 1
                    }
                    _chats.value = _chats.value + (chatId to updated)
                    val newOrder = listOf(chatId) + _chatIds.value.filter { it != chatId }
                    _chatIds.value = newOrder
                }
                // Append to message list
                val current = _messages.value[chatId] ?: emptyList()
                _messages.value = _messages.value + (chatId to current + msg)

                // Auto mark as read if chat is open
                if (_openChatId.value == chatId) {
                    client?.send(TdApi.ViewMessages(chatId, longArrayOf(msg.id), null, true)) {}
                }
            }

            // Message content edited
            TdApi.UpdateMessageContent.CONSTRUCTOR -> {
                val upd    = result as TdApi.UpdateMessageContent
                val list   = _messages.value[upd.chatId]?.toMutableList() ?: return
                val idx    = list.indexOfFirst { it.id == upd.messageId }
                if (idx >= 0) {
                    list[idx].content = upd.newContent
                    _messages.value   = _messages.value + (upd.chatId to list.toList())
                }
            }

            // Message deleted
            TdApi.UpdateDeleteMessages.CONSTRUCTOR -> {
                val upd = result as TdApi.UpdateDeleteMessages
                if (!upd.isPermanent) return
                val ids    = upd.messageIds.toSet()
                val list   = _messages.value[upd.chatId]?.filter { it.id !in ids } ?: return
                _messages.value = _messages.value + (upd.chatId to list)
            }

            // Read receipts — outbox (other person read our message)
            TdApi.UpdateChatReadOutbox.CONSTRUCTOR -> {
                val upd = result as TdApi.UpdateChatReadOutbox
                _outboxReadId.value = _outboxReadId.value + (upd.chatId to upd.lastReadOutboxMessageId)
            }

            // Read receipts — inbox (we read their message)
            TdApi.UpdateChatReadInbox.CONSTRUCTOR -> {
                val upd      = result as TdApi.UpdateChatReadInbox
                val existing = _chats.value[upd.chatId] ?: return
                val updated  = TdApi.Chat().also { c ->
                    c.id          = existing.id
                    c.title       = existing.title
                    c.photo       = existing.photo
                    c.lastMessage = existing.lastMessage
                    c.unreadCount = upd.unreadCount
                }
                _chats.value = _chats.value + (upd.chatId to updated)
            }

            // Typing indicator
            TdApi.UpdateChatAction.CONSTRUCTOR -> {
                val upd = result as TdApi.UpdateChatAction
                val text = when (upd.action.constructor) {
                    TdApi.ChatActionTyping.CONSTRUCTOR          -> "typing..."
                    TdApi.ChatActionRecordingVoiceNote.CONSTRUCTOR -> "recording voice..."
                    TdApi.ChatActionUploadingDocument.CONSTRUCTOR -> "sending file..."
                    TdApi.ChatActionUploadingPhoto.CONSTRUCTOR  -> "sending photo..."
                    TdApi.ChatActionUploadingVideo.CONSTRUCTOR  -> "sending video..."
                    TdApi.ChatActionCancel.CONSTRUCTOR          -> null
                    else -> null
                }
                _typingStatus.value = if (text != null)
                    _typingStatus.value + (upd.chatId to text)
                else
                    _typingStatus.value - upd.chatId
            }

            // File download
            TdApi.UpdateFile.CONSTRUCTOR -> {
                val file = (result as TdApi.UpdateFile).file
                if (file.local.isDownloadingCompleted) {
                    val path = file.local.path
                    _downloadedFiles.value = _downloadedFiles.value + (file.id to path)
                    if (_myPhotoPath.value == null) _myPhotoPath.value = path
                }
            }
        }
    }

    private fun onAuthorizationStateUpdated(state: TdApi.AuthorizationState) {
        _authState.value = state
        when (state.constructor) {
            TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
                client?.send(TdApi.SetTdlibParameters(
                    false,
                    context.filesDir.absolutePath + "/tdlib",
                    null, null,
                    true, true, true, true,
                    apiId, apiHash,
                    "en", "Android", "Unknown", "1.0"
                ), this)
            }
            TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
                loadChats(); fetchMe()
            }
            TdApi.AuthorizationStateClosed.CONSTRUCTOR -> {
                client = null; initClient()
            }
        }
    }

    // ── Chat Screen API ──────────────────────────────────────

    fun openChat(chatId: Long) {
        _openChatId.value = chatId
        client?.send(TdApi.OpenChat(chatId)) {}
        loadMessages(chatId)
    }

    fun closeChat(chatId: Long) {
        _openChatId.value = null
        client?.send(TdApi.CloseChat(chatId)) {}
    }

    fun loadMessages(chatId: Long, fromMessageId: Long = 0L) {
        client?.send(TdApi.GetChatHistory(chatId, fromMessageId, 0, 50, false)) { result ->
            if (result.constructor != TdApi.Messages.CONSTRUCTOR) return@send
            val newMsgs  = (result as TdApi.Messages).messages.toList().reversed()
            val existing = _messages.value[chatId] ?: emptyList()
            val merged   = if (fromMessageId == 0L) newMsgs
                           else newMsgs + existing
            _messages.value = _messages.value + (chatId to merged)
        }
    }

    fun sendMessage(chatId: Long, text: String, replyToId: Long = 0L) {
        val content     = TdApi.InputMessageText(TdApi.FormattedText(text, emptyArray()), null, false)
        val replyTo     = if (replyToId != 0L) TdApi.InputMessageReplyToMessage(replyToId, null, 0, null) else null
        val sendOptions = TdApi.MessageSendOptions()
        client?.send(TdApi.SendMessage(chatId, 0, replyTo, sendOptions, null, content)) {}
    }

    fun deleteMessage(chatId: Long, messageId: Long, forEveryone: Boolean) {
        client?.send(TdApi.DeleteMessages(chatId, longArrayOf(messageId), forEveryone)) {}
    }

    fun markAsRead(chatId: Long, messageId: Long) {
        client?.send(TdApi.ViewMessages(chatId, longArrayOf(messageId), null, true)) {}
    }

    // ── Private helpers ──────────────────────────────────────

    private fun fetchMe() {
        client?.send(TdApi.GetMe()) { result ->
            if (result.constructor != TdApi.User.CONSTRUCTOR) return@send
            val user = result as TdApi.User
            myUserId = user.id
            _myName.value = user.firstName.firstOrNull()?.uppercaseChar()?.toString() ?: "S"
            val photo = user.profilePhoto ?: return@send
            val small = photo.small
            if (small.local.isDownloadingCompleted) _myPhotoPath.value = small.local.path
            else downloadFile(small.id)
        }
    }

    private fun loadChats() {
        client?.send(TdApi.LoadChats(TdApi.ChatListMain(), 100)) { result ->
            when (result.constructor) {
                TdApi.Ok.CONSTRUCTOR    -> getChatIds()
                TdApi.Error.CONSTRUCTOR -> Log.e("SPGram", "LoadChats failed")
            }
        }
    }

    private fun getChatIds() {
        client?.send(TdApi.GetChats(TdApi.ChatListMain(), 100)) { result ->
            if (result.constructor == TdApi.Chats.CONSTRUCTOR)
                _chatIds.value = (result as TdApi.Chats).chatIds.toList()
        }
    }

    fun getMyUserId() = myUserId

    fun sendPhoneNumber(phone: String) =
        client?.send(TdApi.SetAuthenticationPhoneNumber(phone, null), this)
    fun sendVerificationCode(code: String) =
        client?.send(TdApi.CheckAuthenticationCode(code), this)
    fun sendPassword(password: String) =
        client?.send(TdApi.CheckAuthenticationPassword(password), this)
    fun logout() = client?.send(TdApi.LogOut()) {}
    fun downloadFile(fileId: Int) =
        client?.send(TdApi.DownloadFile(fileId, 1, 0, 0, true)) {}
}
