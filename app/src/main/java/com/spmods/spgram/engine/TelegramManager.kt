@file:Suppress("SpellCheckingInspection")

package com.spmods.spgram.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

class TelegramManager(private val context: Context) : Client.ResultHandler {

    companion object {
        init { System.loadLibrary("tdjni") }
    }

    private var client: Client? = null

    private val _authState        = MutableStateFlow<TdApi.AuthorizationState?>(null)
    val authState: StateFlow<TdApi.AuthorizationState?> = _authState.asStateFlow()

    private val _chatIds          = MutableStateFlow<List<Long>>(emptyList())
    val chatIds: StateFlow<List<Long>> = _chatIds.asStateFlow()

    private val _chats            = MutableStateFlow<Map<Long, TdApi.Chat>>(emptyMap())
    val chats: StateFlow<Map<Long, TdApi.Chat>> = _chats.asStateFlow()

    private val _downloadedFiles  = MutableStateFlow<Map<Int, String>>(emptyMap())
    val downloadedFiles: StateFlow<Map<Int, String>> = _downloadedFiles.asStateFlow()

    // Simple extracted values — no TdApi.User exposed to UI
    private val _myName           = MutableStateFlow("S")
    val myName: StateFlow<String> = _myName.asStateFlow()

    private val _myPhotoPath      = MutableStateFlow<String?>(null)
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

            TdApi.UpdateNewChat.CONSTRUCTOR -> {
                val chat = (result as TdApi.UpdateNewChat).chat
                _chats.value = _chats.value + (chat.id to chat)
            }

            TdApi.UpdateChatLastMessage.CONSTRUCTOR -> {
                val upd = result as TdApi.UpdateChatLastMessage
                val existing = _chats.value[upd.chatId] ?: return
                val updated = TdApi.Chat()
                updated.id          = existing.id
                updated.title       = existing.title
                updated.photo       = existing.photo
                updated.lastMessage = upd.lastMessage
                _chats.value = _chats.value + (upd.chatId to updated)
            }

            TdApi.UpdateFile.CONSTRUCTOR -> {
                val file = (result as TdApi.UpdateFile).file
                if (file.local.isDownloadingCompleted) {
                    val path = file.local.path
                    _downloadedFiles.value = _downloadedFiles.value + (file.id to path)
                    // If this is my profile photo, update myPhotoPath
                    if (_myPhotoPath.value == null) {
                        _myPhotoPath.value = path
                    }
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
            if (result.constructor == TdApi.User.CONSTRUCTOR) {
                val user = result as TdApi.User
                // Extract first letter safely
                val letter = user.firstName.firstOrNull()?.uppercaseChar()?.toString() ?: "S"
                _myName.value = letter

                // Download profile photo if available
                val photo = user.profilePhoto
                if (photo != null) {
                    val smallFile = photo.small
                    if (smallFile.local.isDownloadingCompleted) {
                        _myPhotoPath.value = smallFile.local.path
                    } else {
                        downloadFile(smallFile.id)
                    }
                }
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
}
