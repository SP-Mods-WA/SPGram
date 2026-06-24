package com.spmods.spgram.domain.models

/**
 * Represents a single entry in the global "Downloads" list — a file that has
 * been (or is being) downloaded from any chat, mirroring Telegram's own
 * Downloads manager (backed by TDLib's file download list).
 */
data class DownloadedFileModel(
    val fileId: Int,
    val message: MessageModel,
    val addDate: Int,
    val completeDate: Int,
    val isPaused: Boolean
) {
    val isCompleted: Boolean get() = completeDate != 0
}

enum class DownloadsFilter {
    ALL,
    PHOTOS,
    VIDEOS,
    FILES,
    MUSIC,
    VOICE
}

data class DownloadedFileCountsModel(
    val activeCount: Int,
    val pausedCount: Int,
    val completedCount: Int
)

data class FoundDownloadsModel(
    val counts: DownloadedFileCountsModel,
    val files: List<DownloadedFileModel>,
    val nextOffset: String
)
