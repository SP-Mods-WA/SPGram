package com.spmods.spgram.domain.managers

interface DistrManager {
    fun isGmsAvailable(): Boolean
    fun isFcmAvailable(): Boolean
    fun isUnifiedPushDistributorAvailable(): Boolean
    fun isInstalledFromGooglePlay(): Boolean
}