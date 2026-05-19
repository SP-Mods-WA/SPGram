package com.spmods.spgram.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.spmods.spgram.core.DispatcherProvider
import com.spmods.spgram.data.datasource.cache.SettingsCacheDataSource
import com.spmods.spgram.data.datasource.remote.SettingsRemoteDataSource
import com.spmods.spgram.data.db.dao.AttachBotDao
import com.spmods.spgram.data.db.model.AttachBotEntity
import com.spmods.spgram.data.gateway.UpdateDispatcher
import com.spmods.spgram.data.infra.FileObserverHub
import com.spmods.spgram.data.mapper.toDomain
import com.spmods.spgram.domain.models.AttachMenuBotModel
import com.spmods.spgram.domain.models.FileLocalModel
import com.spmods.spgram.domain.repository.AttachMenuBotRepository
import com.spmods.spgram.domain.repository.CacheProvider
import java.util.concurrent.ConcurrentHashMap

class AttachMenuBotRepositoryImpl(
    private val remote: SettingsRemoteDataSource,
    private val cache: SettingsCacheDataSource,
    private val cacheProvider: CacheProvider,
    private val updates: UpdateDispatcher,
    private val fileObserverHub: FileObserverHub,
    private val dispatchers: DispatcherProvider,
    private val attachBotDao: AttachBotDao,
    private val scope: CoroutineScope
) : AttachMenuBotRepository {
    private val attachMenuBots = MutableStateFlow<List<AttachMenuBotModel>>(cacheProvider.attachBots.value)
    private val sideMenuIconFileToBotId = ConcurrentHashMap<Int, Long>()

    init {
        scope.launch {
            updates.attachmentMenuBots.collect { update ->
                cache.putAttachMenuBots(update.bots)
                val bots = update.bots.map { it.toDomain() }
                attachMenuBots.value = bots
                cacheProvider.setAttachBots(bots)
                rebuildTrackedIcons(bots)

                saveAttachBotsToDb(bots)

                update.bots.forEach { bot ->
                    bot.androidSideMenuIcon?.let { icon ->
                        if (icon.local.path.isEmpty()) {
                            remote.downloadFile(icon.id, 1)
                        }
                    }
                }
            }
        }

        scope.launch {
            fileObserverHub.fileStates.collect { state ->
                if (!state.isDownloaded || state.path.isNullOrBlank()) return@collect
                val botId = sideMenuIconFileToBotId[state.fileId] ?: return@collect
                applyBotIconPath(botId, state.fileId, state.path)
            }
        }

        scope.launch {
            attachBotDao.getAttachBots().collect { entities ->
                val bots = entities.mapNotNull {
                    try {
                        Json.decodeFromString<AttachMenuBotModel>(it.data)
                    } catch (_: Exception) {
                        null
                    }
                }
                if (bots.isNotEmpty()) {
                    attachMenuBots.value = bots
                    cacheProvider.setAttachBots(bots)
                    rebuildTrackedIcons(bots)
                }
            }
        }
    }

    override fun getAttachMenuBots(): Flow<List<AttachMenuBotModel>> {
        return attachMenuBots
    }

    private fun rebuildTrackedIcons(bots: List<AttachMenuBotModel>) {
        sideMenuIconFileToBotId.clear()
        bots.forEach { bot ->
            bot.icon?.icon?.id?.takeIf { it != 0 }?.let { fileId ->
                sideMenuIconFileToBotId[fileId] = bot.botUserId
            }
        }
    }

    private suspend fun applyBotIconPath(botId: Long, fileId: Int, path: String) {
        val current = attachMenuBots.value
        if (current.isEmpty()) return

        var changed = false
        val updated = current.map { bot ->
            if (bot.botUserId != botId) return@map bot
            val iconContainer = bot.icon ?: return@map bot
            val iconModel = iconContainer.icon ?: return@map bot
            if (iconModel.id != fileId) return@map bot
            if (iconModel.local.path == path && iconModel.local.isDownloadingCompleted) return@map bot

            changed = true
            bot.copy(
                icon = iconContainer.copy(
                    icon = iconModel.copy(
                        local = FileLocalModel(
                            path = path,
                            isDownloadingActive = false,
                            canBeDownloaded = iconModel.local.canBeDownloaded,
                            isDownloadingCompleted = true,
                            canBeDeleted = iconModel.local.canBeDeleted,
                            downloadOffset = iconModel.local.downloadOffset,
                            downloadedPrefixSize = iconModel.local.downloadedPrefixSize,
                            downloadedSize = iconModel.size
                        )
                    )
                )
            )
        }

        if (!changed) return

        attachMenuBots.value = updated
        cacheProvider.setAttachBots(updated)
        saveAttachBotsToDb(updated)
    }

    private suspend fun saveAttachBotsToDb(bots: List<AttachMenuBotModel>) {
        withContext(dispatchers.io) {
            attachBotDao.clearAll()
            attachBotDao.insertAttachBots(
                bots.map {
                    AttachBotEntity(it.botUserId, Json.encodeToString(it))
                }
            )
        }
    }
}