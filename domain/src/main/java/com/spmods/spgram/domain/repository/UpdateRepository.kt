package com.spmods.spgram.domain.repository

import kotlinx.coroutines.flow.StateFlow
import com.spmods.spgram.domain.models.UpdateState

interface UpdateRepository {
    val updateState: StateFlow<UpdateState>
    suspend fun checkForUpdates()
    fun downloadUpdate()
    fun cancelDownload()
    fun installUpdate()
}
