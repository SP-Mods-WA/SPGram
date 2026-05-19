package com.spmods.spgram.data.mapper

import java.io.File

internal fun isValidFilePath(path: String?): Boolean {
    return !path.isNullOrEmpty() && File(path).exists()
}