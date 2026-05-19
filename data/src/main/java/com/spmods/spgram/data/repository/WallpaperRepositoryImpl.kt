package com.spmods.spgram.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import com.spmods.spgram.core.DispatcherProvider
import com.spmods.spgram.data.datasource.remote.SettingsRemoteDataSource
import com.spmods.spgram.data.db.dao.WallpaperDao
import com.spmods.spgram.data.infra.FileObserverHub
import com.spmods.spgram.data.mapper.mapBackgrounds
import com.spmods.spgram.data.mapper.toBackgroundType
import com.spmods.spgram.data.mapper.toDomain
import com.spmods.spgram.data.mapper.toEntity
import com.spmods.spgram.data.mapper.toInputBackground
import com.spmods.spgram.domain.models.WallpaperModel
import com.spmods.spgram.domain.models.WallpaperType
import com.spmods.spgram.domain.repository.WallpaperRepository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class WallpaperRepositoryImpl(
    private val remote: SettingsRemoteDataSource,
    private val wallpaperDao: WallpaperDao,
    private val fileObserverHub: FileObserverHub,
    private val dispatchers: DispatcherProvider,
    private val scope: CoroutineScope
) : WallpaperRepository {

    private val wallpapers = MutableStateFlow<List<WallpaperModel>>(emptyList())
    private val thumbnailFileToWallpaperId = ConcurrentHashMap<Int, Long>()
    private val documentFileToWallpaperId = ConcurrentHashMap<Int, Long>()
    private val refreshMutex = Mutex()
    private val initialLoadRequested = AtomicBoolean(false)

    init {
        scope.launch {
            wallpaperDao.observeWallpapers().collect { entities ->
                val models = entities.map { it.toDomain() }
                wallpapers.value = models
                rebuildTrackedFiles(models)
            }
        }

        scope.launch {
            fileObserverHub.fileStates.collect { fileState ->
                if (!fileState.isDownloaded || fileState.path.isNullOrBlank()) return@collect
                val wallpaperId = documentFileToWallpaperId[fileState.fileId]
                    ?: thumbnailFileToWallpaperId[fileState.fileId]
                    ?: return@collect
                applyFileUpdate(wallpaperId, fileState.fileId, fileState.path)
            }
        }
    }

    override fun getWallpapers(): Flow<List<WallpaperModel>> = wallpapers.onStart {
        ensureInitialLoad()
    }

    override suspend fun downloadWallpaper(fileId: Int) {
        remote.downloadFile(fileId, 1)
    }

    override suspend fun setDefaultWallpaper(
        wallpaper: WallpaperModel,
        isBlurred: Boolean,
        isMoving: Boolean
    ): WallpaperModel? {
        val background = wallpaper.toInputBackground()
        val type = wallpaper.toBackgroundType(isBlurred = isBlurred, isMoving = isMoving)
        val result = remote.setDefaultBackground(
            background = background,
            type = type,
            forDarkTheme = false
        ) ?: return null

        refreshFromRemote()
        return result.toDomain()
    }

    override suspend fun uploadWallpaper(
        filePath: String,
        isBlurred: Boolean,
        isMoving: Boolean
    ): WallpaperModel? {
        val result = remote.setDefaultBackground(
            background = TdApi.InputBackgroundLocal(TdApi.InputFileLocal(filePath)),
            type = TdApi.BackgroundTypeWallpaper(isBlurred, isMoving),
            forDarkTheme = false
        ) ?: return null

        refreshFromRemote()
        return result.toDomain()
    }

    private fun ensureInitialLoad() {
        if (initialLoadRequested.compareAndSet(false, true)) {
            scope.launch {
                refreshFromRemote()
            }
        }
    }

    private suspend fun refreshFromRemote() {
        refreshMutex.withLock {
            val result = remote.getInstalledBackgrounds(false)
            val mappedWallpapers = mapBackgrounds(result?.backgrounds ?: emptyArray())
            wallpapers.value = mappedWallpapers
            rebuildTrackedFiles(mappedWallpapers)
            saveWallpapersToDb(mappedWallpapers)
        }
    }

    private fun rebuildTrackedFiles(models: List<WallpaperModel>) {
        thumbnailFileToWallpaperId.clear()
        documentFileToWallpaperId.clear()

        models.forEach { wallpaper ->
            wallpaper.thumbnail?.fileId?.takeIf { it != 0 }?.let { fileId ->
                thumbnailFileToWallpaperId[fileId] = wallpaper.id
            }
            if (wallpaper.type == WallpaperType.WALLPAPER && wallpaper.documentId != 0L) {
                wallpaper.documentId.toInt().takeIf { it != 0 }?.let { fileId ->
                    documentFileToWallpaperId[fileId] = wallpaper.id
                }
            }
        }
    }

    private suspend fun applyFileUpdate(wallpaperId: Long, fileId: Int, path: String) {
        val current = wallpapers.value
        if (current.isEmpty()) return

        var changed = false
        val updated = current.map { wallpaper ->
            if (wallpaper.id != wallpaperId) return@map wallpaper

            val next = when {
                wallpaper.thumbnail?.fileId == fileId -> {
                    val thumbnail = wallpaper.thumbnail ?: return@map wallpaper
                    val currentThumbPath = thumbnail.localPath
                    if (currentThumbPath == path) {
                        wallpaper
                    } else {
                        wallpaper.copy(
                            thumbnail = thumbnail.copy(localPath = path)
                        )
                    }
                }

                wallpaper.documentId == fileId.toLong() -> {
                    if (wallpaper.localPath == path && wallpaper.isDownloaded) {
                        wallpaper
                    } else {
                        wallpaper.copy(
                            localPath = path,
                            isDownloaded = true
                        )
                    }
                }

                else -> wallpaper
            }

            if (next != wallpaper) {
                changed = true
            }
            next
        }

        if (!changed) return

        wallpapers.value = updated
        withContext(dispatchers.io) {
            wallpaperDao.upsertWallpapers(updated.filter { it.id == wallpaperId }
                .map { it.toEntity() })
        }
    }

    private suspend fun saveWallpapersToDb(wallpapers: List<WallpaperModel>) {
        withContext(dispatchers.io) {
            val entities = wallpapers.map { it.toEntity() }
            wallpaperDao.upsertWallpapers(entities)
            if (entities.isEmpty()) {
                wallpaperDao.clearAll()
            } else {
                wallpaperDao.deleteNotIn(entities.map { it.id })
            }
        }
    }
}