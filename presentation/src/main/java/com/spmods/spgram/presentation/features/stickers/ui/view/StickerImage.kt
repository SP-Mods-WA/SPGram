package com.spmods.spgram.presentation.features.stickers.ui.view

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.spmods.spgram.presentation.core.util.namespacedCacheKey

@Composable
fun StickerImage(
    modifier: Modifier = Modifier,
    path: String?,
    animate: Boolean = true,
    isInline: Boolean = false,
) {
    if (path == null) return

    key(path) {
        StickerImageContent(
            modifier = modifier,
            path = path,
            animate = animate,
            isInline = isInline
        )
    }
}

@Composable
private fun StickerImageContent(
    modifier: Modifier = Modifier,
    path: String,
    animate: Boolean = true,
    isInline: Boolean = false,
) {

    val isAnimated = path.endsWith(".webm", ignoreCase = true) || 
                     path.endsWith(".tgs", ignoreCase = true) || 
                     path.endsWith(".json", ignoreCase = true)

    if (isAnimated) {
        StickerPlayer(
            path = path,
            modifier = modifier,
        )
        return
    }

    val cacheKey = remember(path) { namespacedCacheKey("sticker", path) }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(path)
            .apply {
                cacheKey?.let {
                    memoryCacheKey(it)
                    diskCacheKey(it)
                }
                if (!isInline) crossfade(true)
            }
            .build(),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        loading = {
            if (!isInline) Box(modifier = Modifier.shimmerEffect())
        }
    )
}
