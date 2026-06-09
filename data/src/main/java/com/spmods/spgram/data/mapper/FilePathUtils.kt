package com.spmods.spgram.data.mapper

import java.io.File

internal fun isValidFilePath(path: String?): Boolean {
    if (path.isNullOrEmpty()) return false
    val file = File(path)
    if (!file.exists()) return false
    // Reject TDLib partial/temp download files — these are incomplete and should
    // not be treated as valid paths. TDLib uses a .part suffix for in-progress files.
    if (path.endsWith(".part", ignoreCase = true)) return false
    return true
}
