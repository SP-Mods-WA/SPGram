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
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

class TelegramManager(private val context: Context) : Client.ResultHandler {

    companion object {
        init { System.loadLibrary("tdjni") }
    }

    private val scope  = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var client: Client? = null

    // ── Auth ─────────────────────────────────────────────────
    private val _authState = MutableStateFlow<TdApi.AuthorizationState?>(null)
    val authState: StateFlow<TdApi.AuthorizationState?> = _authState.asStateFlow()

    // ── Chat list ────────────────────────────────────────────
    private val _chatIds = MutableStateFlow<List<Long>>(emptyList())
    val chatIds: StateFlow<List<Long>> = _chatIds.asStateFlow()

    private val _chats = MutableStateFlow<Map<Long, TdApi.Chat>>(emptyMap())
    val chats: StateFlow<Map<Long, TdApi.Chat>> = _chats.asStateFlow()

    // ── Files ────────────────────────────────────────────────
    private val _downloadedFiles = MutableStateFlow<Map<Int, String>>(emptyMap())
    val downloadedFiles: StateFlow<Map<Int, String>> = _downloadedFiles.asStateFlow()

    // ── My profile ───────────────────────────────────────────
    private val _myName      = MutableStateFlow("S")
    val myName: StateFlow<String> = _myName.asStateFlow()

    private val _myPhotoPath = MutableStateFlow<String?>(null)
    val myPhotoPath: StateFlow<String?> = _myPhotoPath.asStateFlow()

    private var myUserId: Long = 0L

    // ── Chat screen ──────────────────────────────────────────
    private val _messages = MutableStateFlow<Map<Long, List<TdApi.Message>>>(emptyMap())
    val messages: StateFlow<Map<Long, List<TdApi.Message>>> = _messages.asStateFlow()

    private val _typingStatus = MutableStateFlow<Map<Long, String>>(emptyMap())
    val typingStatus: StateFlow<Map<Long, String>> = _typingStatus.asStateFlow()

    private val _outboxReadId = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val outboxReadId: StateFlow<Map<Long, Long>> = _outboxReadId.asStateFlow()

    // User status: chatId → "online" | "last seen ..."
    private val _userStatus = MutableStateFlow<Map<Long, String>>(emptyMap())
    val userStatus: StateFlow<Map<Long, String>> = _userStatus.asStateFlow()

    // userId → chatId mapping for status updates
    private val userIdToChatId = mutableMapOf<Long, Long>()

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
                // Map private chat userId → chatId
                val peer = chat.type
                if (peer is TdApi.ChatTypePrivate) {
                    userIdToChatId[peer.userId] = chat.id
                }
            }

            TdApi.UpdateChatLastMessage.CONSTRUCTOR -> {
                val upd = result as TdApi.UpdateChatLastMessage
                updateChatLastMessage(upd.chatId, upd.lastMessage)
            }

            TdApi.UpdateNewMessage.CONSTRUCTOR -> {
                val msg    = (result as TdApi.UpdateNewMessage).message
                val chatId = msg.chatId
                updateChatLastMessage(chatId, msg)
                // Move to top
                val newOrder = listOf(chatId) + _chatIds.value.filter { it != chatId }
                _chatIds.value = newOrder
                // Append to message list
                val current = _messages.value[chatId] ?: emptyList()
                _messages.value = _messages.value + (chatId to current + msg)
                // Auto mark read if open
                client?.send(TdApi.ViewMessages(chatId, longArrayOf(msg.id), null, true)) {}
            }

            TdApi.UpdateMessageContent.CONSTRUCTOR -> {
                val upd  = result as TdApi.UpdateMessageContent
                val list = _messages.value[upd.chatId]?.toMutableList() ?: return
                val idx  = list.indexOfFirst { it.id == upd.messageId }
                if (idx >= 0) {
                    list[idx].content = upd.newContent
                    _messages.value   = _messages.value + (upd.chatId to list.toList())
                }
            }

            TdApi.UpdateDeleteMessages.CONSTRUCTOR -> {
                val upd = result as TdApi.UpdateDeleteMessages
                if (!upd.isPermanent) return
                val ids  = upd.messageIds.toSet()
                val list = _messages.value[upd.chatId]?.filter { it.id !in ids } ?: return
                _messages.value = _messages.value + (upd.chatId to list)
            }

            TdApi.UpdateChatReadOutbox.CONSTRUCTOR -> {
                val upd = result as TdApi.UpdateChatReadOutbox
                _outboxReadId.value = _outboxReadId.value + (upd.chatId to upd.lastReadOutboxMessageId)
            }

            TdApi.UpdateChatReadInbox.CONSTRUCTOR -> {
                val upd      = result as TdApi.UpdateChatReadInbox
                val existing = _chats.value[upd.chatId] ?: return
                val updated  = cloneChat(existing, unreadCount = upd.unreadCount)
                _chats.value = _chats.value + (upd.chatId to updated)
            }

            TdApi.UpdateChatAction.CONSTRUCTOR -> {
                val upd  = result as TdApi.UpdateChatAction
                val text = when (upd.action.constructor) {
                    TdApi.ChatActionTyping.CONSTRUCTOR             -> "typing..."
                    TdApi.ChatActionRecordingVoiceNote.CONSTRUCTOR -> "recording voice..."
                    TdApi.ChatActionUploadingDocument.CONSTRUCTOR  -> "sending file..."
                    TdApi.ChatActionUploadingPhoto.CONSTRUCTOR     -> "sending photo..."
                    TdApi.ChatActionUploadingVideo.CONSTRUCTOR     -> "sending video..."
                    TdApi.ChatActionCancel.CONSTRUCTOR             -> null
                    else                                           -> null
                }
                _typingStatus.value = if (text != null)
                    _typingStatus.value + (upd.chatId to text)
                else
                    _typingStatus.value - upd.chatId
            }

            TdApi.UpdateUserStatus.CONSTRUCTOR -> {
                val upd    = result as TdApi.UpdateUserStatus
                val chatId = userIdToChatId[upd.userId] ?: return
                val status = formatStatus(upd.status)
                _userStatus.value = _userStatus.value + (chatId to status)
            }

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

    private fun formatStatus(status: TdApi.UserStatus?): String {
        return when (status?.constructor) {
            TdApi.UserStatusOnline.CONSTRUCTOR    -> "online"
            TdApi.UserStatusOffline.CONSTRUCTOR   -> {
                val ts = (status as TdApi.UserStatusOffline).wasOnline
                val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                "last seen at ${sdf.format(java.util.Date(ts * 1000L))}"
            }
            TdApi.UserStatusRecently.CONSTRUCTOR  -> "last seen recently"
            TdApi.UserStatusLastWeek.CONSTRUCTOR  -> "last seen last week"
            TdApi.UserStatusLastMonth.CONSTRUCTOR -> "last seen last month"
            else -> ""
        }
    }

    private fun updateChatLastMessage(chatId: Long, msg: TdApi.Message?) {
        val existing = _chats.value[chatId] ?: return
        val updated  = cloneChat(existing, lastMessage = msg)
        _chats.value = _chats.value + (chatId to updated)
    }

    private fun cloneChat(
        src: TdApi.Chat,
        lastMessage: TdApi.Message? = src.lastMessage,
        unreadCount: Int = src.unreadCount
    ): TdApi.Chat {
        val c = TdApi.Chat()
        c.id          = src.id
        c.title       = src.title
        c.photo       = src.photo
        c.type        = src.type
        c.lastMessage = lastMessage
        c.unreadCount = unreadCount
        return c
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

    // ── Chat screen API ──────────────────────────────────────

    fun openChat(chatId: Long) {
        client?.send(TdApi.OpenChat(chatId)) {}
        loadMessages(chatId)
        // Fetch user status for private chats
        val chat = _chats.value[chatId]
        val peer = chat?.type
        if (peer is TdApi.ChatTypePrivate) {
            userIdToChatId[peer.userId] = chatId
            client?.send(TdApi.GetUser(peer.userId)) { result ->
                if (result.constructor == TdApi.User.CONSTRUCTOR) {
                    val user   = result as TdApi.User
                    val status = formatStatus(user.status)
                    if (status.isNotEmpty()) {
                        _userStatus.value = _userStatus.value + (chatId to status)
                    }
                }
            }
        }
    }

    fun closeChat(chatId: Long) {
        client?.send(TdApi.CloseChat(chatId)) {}
    }

    fun loadMessages(chatId: Long, fromMessageId: Long = 0L) {
        client?.send(TdApi.GetChatHistory(chatId, fromMessageId, 0, 50, false)) { result ->
            if (result.constructor != TdApi.Messages.CONSTRUCTOR) return@send
            val newMsgs  = (result as TdApi.Messages).messages.toList().reversed()
            val existing = _messages.value[chatId] ?: emptyList()
            val merged   = if (fromMessageId == 0L) newMsgs else newMsgs + existing
            _messages.value = _messages.value + (chatId to merged)
        }
    }

    fun loadMoreMessages(chatId: Long) {
        val oldest = _messages.value[chatId]?.firstOrNull()?.id ?: return
        loadMessages(chatId, oldest)
    }

    fun sendMessage(chatId: Long, text: String, replyToId: Long = 0L) {
        val content = TdApi.InputMessageText(TdApi.FormattedText(text, emptyArray()), null, false)
        val replyTo = if (replyToId != 0L) {
            val r = TdApi.InputMessageReplyToMessage()
            r.messageId = replyToId
            r
        } else null
        val req = TdApi.SendMessage()
        req.chatId              = chatId
        req.replyTo             = replyTo
        req.options             = TdApi.MessageSendOptions()
        req.inputMessageContent = content
        client?.send(req) { result ->
            if (result.constructor == TdApi.Error.CONSTRUCTOR) {
                Log.e("SPGram", "Send error: ${(result as TdApi.Error).message}")
            }
        }
    }

    fun deleteMessage(chatId: Long, messageId: Long, forEveryone: Boolean) {
        client?.send(TdApi.DeleteMessages(chatId, longArrayOf(messageId), forEveryone)) {}
    }

    fun copyMessageText(message: TdApi.Message): String? {
        return when (val c = message.content) {
            is TdApi.MessageText -> c.text.text
            else -> null
        }
    }

    fun downloadFile(fileId: Int) =
        client?.send(TdApi.DownloadFile(fileId, 1, 0, 0, true)) {}

    fun getMyUserId() = myUserId

    // ── Private helpers ──────────────────────────────────────

    private fun fetchMe() {
        client?.send(TdApi.GetMe()) { result ->
            if (result.constructor != TdApi.User.CONSTRUCTOR) return@send
            val user = result as TdApi.User
            myUserId      = user.id
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

    fun sendPhoneNumber(phone: String) =
        client?.send(TdApi.SetAuthenticationPhoneNumber(phone, null), this)
    fun sendVerificationCode(code: String) =
        client?.send(TdApi.CheckAuthenticationCode(code), this)
    fun sendPassword(password: String) =
        client?.send(TdApi.CheckAuthenticationPassword(password), this)
    fun logout() = client?.send(TdApi.LogOut()) {}
}
