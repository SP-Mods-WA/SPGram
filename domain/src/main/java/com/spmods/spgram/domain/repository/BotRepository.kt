package com.spmods.spgram.domain.repository

import com.spmods.spgram.domain.models.BotCommandModel
import com.spmods.spgram.domain.models.BotInfoModel

interface BotRepository {
    suspend fun getBotCommands(botId: Long): List<BotCommandModel>
    suspend fun getBotInfo(botId: Long): BotInfoModel?
}