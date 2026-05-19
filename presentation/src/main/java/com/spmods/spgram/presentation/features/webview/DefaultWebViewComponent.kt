package com.spmods.spgram.presentation.features.webview

import com.spmods.spgram.presentation.root.AppComponentContext

class DefaultWebViewComponent(
    context: AppComponentContext,
    override val url: String,
    private val onDismiss: () -> Unit
) : WebViewComponent, AppComponentContext by context {
    override fun onDismiss() = onDismiss.invoke()
}