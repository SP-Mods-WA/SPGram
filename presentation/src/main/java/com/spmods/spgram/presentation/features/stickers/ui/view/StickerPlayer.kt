package com.spmods.spgram.presentation.features.stickers.ui.view

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.unit.Constraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.spmods.spgram.presentation.features.stickers.core.LottieStickerController
import com.spmods.spgram.presentation.features.stickers.core.StickerController
import com.spmods.spgram.presentation.features.stickers.core.StickerThumbnailCache
import com.spmods.spgram.presentation.features.stickers.core.VpxStickerController
import java.io.File

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // https://issuetracker.google.com/issues/432262806 lol
@Composable
fun StickerPlayer(
    path: String,
    modifier: Modifier = Modifier,
    isInline: Boolean = false,
) {
    var measuredWidth by remember { mutableIntStateOf(0) }
    var measuredHeight by remember { mutableIntStateOf(0) }
    Box(modifier = modifier.onSizeChanged { size ->
        measuredWidth = size.width
        measuredHeight = size.height
    }) {
        val widthPx = measuredWidth
        val heightPx = measuredHeight
        
        val renderWidth = if (widthPx > 0) minOf(widthPx, 512) else 512
        val renderHeight = if (heightPx > 0) minOf(heightPx, 512) else 512

        val scope = rememberCoroutineScope()
        val thumbnailKey = remember(path) {
            val file = File(path)
            if (file.exists()) {
                "${path}:${file.length()}:${file.lastModified()}"
            } else {
                path
            }
        }
        val controller = remember(path) {
            val ctrl: StickerController = if (path.endsWith(".webm", ignoreCase = true)) {
                VpxStickerController(path, scope)
            } else {
                LottieStickerController(path, scope, renderWidth, renderHeight)
            }
            ctrl
        }

        var cachedBitmap by remember(thumbnailKey) {
            mutableStateOf(StickerThumbnailCache.get(thumbnailKey))
        }

        LaunchedEffect(thumbnailKey, controller) {
            if (cachedBitmap == null) {
                val firstFrame = controller.renderFirstFrame()
                if (firstFrame != null) {
                    StickerThumbnailCache.put(thumbnailKey, firstFrame)
                    cachedBitmap = firstFrame
                }
            }
        }

        DisposableEffect(controller) {
            controller.start()
            onDispose {
                controller.release()
            }
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        val isScrolling = LocalIsScrolling.current
        
        DisposableEffect(controller, lifecycleOwner, isScrolling) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    controller.setPaused(isScrolling)
                } else if (event == Lifecycle.Event.ON_PAUSE) {
                    controller.setPaused(true)
                }
            }

            controller.setPaused(isScrolling || lifecycleOwner.lifecycle.currentState != Lifecycle.State.RESUMED)
            
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        val isLoaded by remember(controller, cachedBitmap) {
            derivedStateOf { controller.currentImageBitmap != null || cachedBitmap != null }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = isLoaded,
                animationSpec = tween(200),
                label = "StickerCrossfade"
            ) { loaded ->
                if (loaded) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                val bitmap = controller.currentImageBitmap ?: cachedBitmap

                                if (bitmap != null) {
                                    val srcW = bitmap.width.toFloat()
                                    val srcH = bitmap.height.toFloat()
                                    val dstW = size.width
                                    val dstH = size.height

                                    val scale = minOf(dstW / srcW, dstH / srcH)
                                    val drawW = srcW * scale
                                    val drawH = srcH * scale

                                    val left = (dstW - drawW) / 2
                                    val top = (dstH - drawH) / 2

                                    drawImage(
                                        image = bitmap,
                                        dstOffset = IntOffset(left.toInt(), top.toInt()),
                                        dstSize = IntSize(drawW.toInt(), drawH.toInt()),
                                        filterQuality = FilterQuality.Low
                                    )
                                }
                            }
                    )
                } else {
                    if (!isInline) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
    }
}
