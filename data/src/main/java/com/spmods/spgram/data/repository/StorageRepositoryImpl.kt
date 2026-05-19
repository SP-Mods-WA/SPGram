package com.spmods.spgram.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.drinkless.tdlib.TdApi
import com.spmods.spgram.core.DispatcherProvider
import com.spmods.spgram.data.datasource.cache.SettingsCacheDataSource
import com.spmods.spgram.data.datasource.remote.ChatsRemoteDataSource
import com.spmods.spgram.data.datasource.remote.SettingsRemoteDataSource
import com.spmods.spgram.data.mapper.StorageMapper
import com.spmods.spgram.domain.models.StorageUsageModel
import com.spmods.spgram.domain.repository.StorageRepository
import com.spmods.spgram.domain.repository.StringProvider

class StorageRepositoryImpl(
    private val remote: SettingsRemoteDataSource,
    private val cache: SettingsCacheDataSource,
    private val chatsRemote: ChatsRemoteDataSource,
    private val dispatchers: DispatcherProvider,
    private val storageMapper: StorageMapper,
    private val stringProvider: StringProvider
) : StorageRepository {
    private val manuallyClearableFileTypes = arrayOf(
        TdApi.FileTypeAnimation(),
        TdApi.FileTypeAudio(),
        TdApi.FileTypeDocument(),
        TdApi.FileTypePhoto(),
        TdApi.FileTypePhotoStory(),
        TdApi.FileTypeProfilePhoto(),
        TdApi.FileTypeSticker(),
        TdApi.FileTypeThumbnail(),
        TdApi.FileTypeVideo(),
        TdApi.FileTypeVideoNote(),
        TdApi.FileTypeVideoStory(),
        TdApi.FileTypeVoiceNote(),
        TdApi.FileTypeWallpaper()
    )
    private val backgroundMaintainedFileTypes = arrayOf(
        TdApi.FileTypeAudio(),
        TdApi.FileTypeDocument(),
        TdApi.FileTypePhoto(),
        TdApi.FileTypePhotoStory(),
        TdApi.FileTypeVideo(),
        TdApi.FileTypeVideoNote(),
        TdApi.FileTypeVideoStory(),
        TdApi.FileTypeVoiceNote()
    )

    override suspend fun getStorageUsage(): StorageUsageModel? = coroutineScope {
        val stats = remote.getStorageStatistics(100) ?: return@coroutineScope null
        val processedChats = (stats.byChat ?: emptyArray()).map { chatStat ->
            async(dispatchers.default) {
                val title = when {
                    chatStat.chatId == 0L -> stringProvider.getString("storage_other_cache")
                    else -> cache.getChat(chatStat.chatId)?.title
                        ?: chatsRemote.getChat(chatStat.chatId)?.title
                        ?: stringProvider.getString("storage_chat_format", chatStat.chatId)
                }
                storageMapper.mapChatStatsToDomain(chatStat, title)
            }
        }.awaitAll()

        storageMapper.mapToDomain(stats, processedChats)
    }

    override suspend fun clearStorage(chatId: Long?): Boolean {
        return remote.optimizeStorage(
            size = 0,
            ttl = 0,
            count = 0,
            immunityDelay = 0,
            fileTypes = manuallyClearableFileTypes,
            chatIds = chatId?.let { longArrayOf(it) },
            returnDeletedFileStatistics = false,
            chatLimit = 20
        )
    }

    override suspend fun setDatabaseMaintenanceSettings(
        maxDatabaseSize: Long,
        maxTimeFromLastAccess: Int
    ): Boolean {
        return remote.optimizeStorage(
            size = maxDatabaseSize,
            ttl = maxTimeFromLastAccess,
            count = -1,
            immunityDelay = -1,
            fileTypes = backgroundMaintainedFileTypes,
            chatIds = null,
            returnDeletedFileStatistics = true,
            chatLimit = 0
        )
    }

    override suspend fun getStorageOptimizerEnabled(): Boolean {
        val result = remote.getOption("use_storage_optimizer")
        return if (result is TdApi.OptionValueBoolean) {
            result.value
        } else {
            false
        }
    }

    override suspend fun setStorageOptimizerEnabled(enabled: Boolean) {
        remote.setOption("use_storage_optimizer", TdApi.OptionValueBoolean(enabled))
    }
}