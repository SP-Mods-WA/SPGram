package com.spmods.spgram.data.datasource.remote

import com.spmods.spgram.domain.models.UpdateInfo

interface UpdateRemoteDateSource {
    suspend fun fetchLatestUpdate(): UpdateInfo?
    suspend fun getTdLibVersion(): String
    suspend fun getTdLibCommitHash(): String
}