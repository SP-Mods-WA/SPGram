package com.spmods.spgram.app.di

import android.content.ClipData
import android.content.ClipboardManager
import com.spmods.spgram.domain.managers.ClipManager

class ClipManagerImpl(private val clipboardManager: ClipboardManager?) : ClipManager {
    override fun copyToClipboard(tag: String, text: String) {
        val clip = ClipData.newPlainText(tag, text)
        clipboardManager?.setPrimaryClip(clip)
    }
}