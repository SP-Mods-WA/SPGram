package com.spmods.spgram.data.chats

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.spmods.spgram.core.DispatcherProvider
import com.spmods.spgram.data.datasource.remote.ChatRemoteSource
import com.spmods.spgram.data.db.dao.SearchHistoryDao
import com.spmods.spgram.data.db.model.SearchHistoryEntity
import com.spmods.spgram.data.mapper.MessageMapper
import com.spmods.spgram.domain.models.ChatModel
import com.spmods.spgram.domain.repository.CacheProvider
import com.spmods.spgram.domain.repository.SearchMessagesResult

class ChatSearchManager(
    private val chatRemoteSource: ChatRemoteSource,
    private val messageMapper: MessageMapper,
    private val cacheProvider: CacheProvider,
    private val searchHistoryDao: SearchHistoryDao,
    private val dispatchers: DispatcherProvider,
    private val scope: CoroutineScope,
    private val resolveChatById: suspend (Long) -> ChatModel?
) {
    val searchHistory: Flow<List<ChatModel>> = cacheProvider.searchHistory.map { ids ->
        coroutineScope {
            ids.map { id -> async { resolveChatById(id) } }.awaitAll().filterNotNull()
        }
    }

    init {
        scope.launch(dispatchers.io) {
            searchHistoryDao.getSearchHistory().collect { entities ->
                cacheProvider.setSearchHistory(entities.map { it.chatId })
            }
        }
    }

    suspend fun searchChats(query: String): List<ChatModel> {
        if (query.isBlank()) return emptyList()
        val result = chatRemoteSource.searchChats(query, 50) ?: return emptyList()
        return coroutineScope {
            result.chatIds.map { id -> async { resolveChatById(id) } }.awaitAll().filterNotNull()
        }
    }

    suspend fun searchPublicChats(query: String): List<ChatModel> {
        if (query.isBlank()) return emptyList()
        val result = chatRemoteSource.searchPublicChats(query) ?: return emptyList()
        return coroutineScope {
            result.chatIds.map { id -> async { resolveChatById(id) } }.awaitAll().filterNotNull()
        }
    }

    suspend fun searchMessages(query: String, offset: String, limit: Int): SearchMessagesResult {
        val result = chatRemoteSource.searchMessages(query, offset, limit)
            ?: return SearchMessagesResult(emptyList(), "")
        val models = coroutineScope {
            result.messages.map { message ->
                async { messageMapper.mapMessageToModel(message, isChatOpen = false) }
            }.awaitAll()
        }
        return SearchMessagesResult(models, result.nextOffset)
    }

    fun addSearchChatId(chatId: Long) {
        cacheProvider.addSearchChatId(chatId)
        scope.launch(dispatchers.io) {
            searchHistoryDao.insertSearchChatId(SearchHistoryEntity(chatId))
        }
    }

    fun removeSearchChatId(chatId: Long) {
        cacheProvider.removeSearchChatId(chatId)
        scope.launch(dispatchers.io) {
            searchHistoryDao.deleteSearchChatId(chatId)
        }
    }

    fun clearSearchHistory() {
        cacheProvider.clearSearchHistory()
        scope.launch(dispatchers.io) {
            searchHistoryDao.clearAll()
        }
    }
}