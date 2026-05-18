package com.spmods.spgram.domain.repository

import kotlinx.coroutines.flow.Flow
import com.spmods.spgram.domain.models.ChatModel
import com.spmods.spgram.domain.models.MessageModel

data class SearchMessagesResult(
    val messages: List<MessageModel>,
    val nextOffset: String
)

interface ChatSearchRepository {
    val searchHistory: Flow<List<ChatModel>>

    suspend fun searchChats(query: String): List<ChatModel>
    suspend fun searchPublicChats(query: String): List<ChatModel>
    suspend fun searchMessages(query: String, offset: String = "", limit: Int = 50): SearchMessagesResult

    fun addSearchChatId(chatId: Long)
    fun removeSearchChatId(chatId: Long)
    fun clearSearchHistory()
}