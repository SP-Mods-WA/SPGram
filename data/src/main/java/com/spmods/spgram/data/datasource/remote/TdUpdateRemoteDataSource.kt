package com.spmods.spgram.data.datasource.remote

import com.spmods.spgram.domain.models.UpdateInfo

class TdUpdateRemoteDataSource(
    private val channelId: Long = -1003566234286L
) : UpdateRemoteDateSource {

    override suspend fun fetchLatestUpdate(): UpdateInfo? = null

    override suspend fun getTdLibVersion(): String = ""

    override suspend fun getTdLibCommitHash(): String = ""
}
