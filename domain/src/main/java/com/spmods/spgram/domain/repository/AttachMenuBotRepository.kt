package com.spmods.spgram.domain.repository

import kotlinx.coroutines.flow.Flow
import com.spmods.spgram.domain.models.AttachMenuBotModel

interface AttachMenuBotRepository {
    fun getAttachMenuBots(): Flow<List<AttachMenuBotModel>>
}