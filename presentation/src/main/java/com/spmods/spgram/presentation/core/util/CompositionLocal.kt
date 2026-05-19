package com.spmods.spgram.presentation.core.util

import androidx.compose.runtime.staticCompositionLocalOf
import com.spmods.spgram.presentation.core.media.VideoPlayerPool

val LocalVideoPlayerPool = staticCompositionLocalOf<VideoPlayerPool> {
    error("VideoPlayerPool not provided")
}

val LocalTabletInterfaceEnabled = staticCompositionLocalOf { true }
