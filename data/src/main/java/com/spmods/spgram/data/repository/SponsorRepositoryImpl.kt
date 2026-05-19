package com.spmods.spgram.data.repository

import com.spmods.spgram.data.infra.SponsorSyncManager
import com.spmods.spgram.domain.repository.SponsorRepository

class SponsorRepositoryImpl(
    private val sponsorSyncManager: SponsorSyncManager
) : SponsorRepository {
    override fun forceSponsorSync() {
        sponsorSyncManager.forceSync()
    }
}