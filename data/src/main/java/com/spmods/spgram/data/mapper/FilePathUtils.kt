package com.spmods.spgram.data.mapper

import java.io.File

internal fun isValidFilePath(path: String?): Boolean {
    if (path.isNullOrEmpty()) return false
    val file = File(path)
    if (!file.exists()) return false
    // Reject TDLib partial/temp download files.
    // TDLib uses a .part suffix for in-progress downloads, but may also
    // write temp files without any extension. The primary guard is
    // isDownloadingCompleted in TdFileHelper.resolveLocalFilePath —
    // this catches the .part case as a secondary safety net.
    if (path.endsWith(".part", ignoreCase = true)) return false
    return true
}
