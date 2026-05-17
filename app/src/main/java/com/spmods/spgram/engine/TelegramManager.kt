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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var client: Client? = null

    private val _authState       = MutableStateFlow<TdApi.AuthorizationState?>(null)
    val authState: StateFlow<TdApi.AuthorizationState?> = _authState.asStateFlow()

    private val _chatIds         = MutableStateFlow<List<Long>>(emptyList())
    val chatIds: StateFlow<List<Long>> = _chatIds.asStateFlow()

    private val _chats           = MutableStateFlow<Map<Long, TdApi.Chat>>(emptyMap())
    val chats: StateFlow<Map<Long, TdApi.Chat>> = _chats.asStateFlow()

    private val _downloadedFiles = MutableStateFlow<Map<Int, String>>(emptyMap())
    val downloadedFiles: StateFlow<Map<Int, String>> = _downloadedFiles.asStateFlow()

    private val _myName          = MutableStateFlow("S")
    val myName: StateFlow<String> = _myName.asStateFlow()

    private val _myPhotoPath     = MutableStateFlow<String?>(null)
    val myPhotoPath: StateFlow<String?> = _myPhotoPath.asStateFlow()

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

            // New chat discovered
            TdApi.UpdateNewChat.CONSTRUCTOR -> {
                val chat = (result as TdApi.UpdateNewChat).chat
                _chats.value = _chats.value + (chat.id to chat)
            }

            // Last message updated — real-time preview
            TdApi.UpdateChatLastMessage.CONSTRUCTOR -> {
                val upd      = result as TdApi.UpdateChatLastMessage
                val existing = _chats.value[upd.chatId] ?: return
                val updated  = TdApi.Chat().also { c ->
                    c.id          = existing.id
                    c.title       = existing.title
                    c.photo       = existing.photo
                    c.lastMessage = upd.lastMessage
                }
                _chats.value = _chats.value + (upd.chatId to updated)
            }

            // New message received — update preview & re-sort chat list
            TdApi.UpdateNewMessage.CONSTRUCTOR -> {
                val msg      = (result as TdApi.UpdateNewMessage).message
                val chatId   = msg.chatId
                val existing = _chats.value[chatId] ?: return
                val updated  = TdApi.Chat().also { c ->
                    c.id          = existing.id
                    c.title       = existing.title
                    c.photo       = existing.photo
                    c.lastMessage = msg
                }
                _chats.value = _chats.value + (chatId to updated)
                // Move chat to top
                val newOrder = listOf(chatId) + _chatIds.value.filter { it != chatId }
                _chatIds.value = newOrder
            }

            // Chat position changed (pinned, folder, etc.)
            TdApi.UpdateChatPosition.CONSTRUCTOR -> {
                val upd = result as TdApi.UpdateChatPosition
                if (upd.position.list.constructor == TdApi.ChatListMain.CONSTRUCTOR) {
                    scope.launch { refreshChatOrder() }
                }
            }

            // File download progress
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
                loadChats()
                fetchMe()
            }
            TdApi.AuthorizationStateClosed.CONSTRUCTOR -> {
                client = null
                initClient()
            }
        }
    }

    private fun fetchMe() {
        client?.send(TdApi.GetMe()) { result ->
            if (result.constructor != TdApi.User.CONSTRUCTOR) return@send
            val user   = result as TdApi.User
            val letter = user.firstName.firstOrNull()?.uppercaseChar()?.toString() ?: "S"
            _myName.value = letter
            val photo  = user.profilePhoto ?: return@send
            val small  = photo.small
            if (small.local.isDownloadingCompleted) {
                _myPhotoPath.value = small.local.path
            } else {
                downloadFile(small.id)
            }
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
            if (result.constructor == TdApi.Chats.CONSTRUCTOR) {
                _chatIds.value = (result as TdApi.Chats).chatIds.toList()
            }
        }
    }

    private fun refreshChatOrder() {
        client?.send(TdApi.GetChats(TdApi.ChatListMain(), 200)) { result ->
            if (result.constructor == TdApi.Chats.CONSTRUCTOR) {
                _chatIds.value = (result as TdApi.Chats).chatIds.toList()
            }
        }
    }

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
