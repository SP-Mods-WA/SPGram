package com.spmods.spgram.presentation.core.util

import android.content.Context
import android.widget.Toast
import com.spmods.spgram.domain.repository.MessageDisplayer

class ToastMessageDisplayer(
    private val context: Context
) : MessageDisplayer {
    override fun show(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}