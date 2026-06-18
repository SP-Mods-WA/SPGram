package com.spmods.spgram.domain.models

data class UpdateInfo(
    val version: String,
    val versionCode: Int,
    val description: String,
    val changelog: List<RichText>,
    val downloadUrl: String,       // direct download URL from website
    val fileName: String,
    val fileSize: Long,
    val forceUpdate: Boolean = false  // JSON flag — true = sheet cannot be dismissed
)

data class RichText(
    val text: String,
    val entities: List<MessageEntity> = emptyList()
)

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState
    object UpToDate : UpdateState
    data class Downloading(val progress: Float, val totalSize: Long) : UpdateState
    data class ReadyToInstall(val filePath: String) : UpdateState
    data class Error(val message: String) : UpdateState
}
