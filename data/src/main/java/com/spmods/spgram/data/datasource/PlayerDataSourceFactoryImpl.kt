package com.spmods.spgram.data.datasource

import com.spmods.spgram.domain.repository.PlayerDataSourceFactory

class PlayerDataSourceFactoryImpl(
    private val fileDataSource: FileDataSource
) : PlayerDataSourceFactory {

    override fun createPayload(fileId: Int): Any {
        return TelegramStreamingDataSource.Factory(fileDataSource, fileId)
    }
}