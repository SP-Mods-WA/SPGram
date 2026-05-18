package com.spmods.spgram.domain.managers

interface ClipManager {
    fun copyToClipboard(tag: String, text: String)
}