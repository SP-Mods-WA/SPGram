package com.spmods.spgram.presentation.features.stickers.core

import android.graphics.Bitmap
import android.os.Build
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

object BitmapPool {
    private val pool = LinkedList<Bitmap>()
    private val mutex = Mutex()
    // 12 bitmaps max — 3 per sticker (front/back/spare) × 4 concurrent stickers max
    private const val MAX_POOL_SIZE = 12

    suspend fun obtain(width: Int, height: Int): Bitmap {
        mutex.withLock {
            val iterator = pool.iterator()
            while (iterator.hasNext()) {
                val bitmap = iterator.next()
                if (bitmap.width == width && bitmap.height == height) {
                    iterator.remove()
                    return bitmap
                }
            }
        }
        return try {
            createBitmap(width, height)
        } catch (oom: OutOfMemoryError) {
            // Memory pressure — clear pool and retry
            clear()
            createBitmap(width.coerceAtMost(256), height.coerceAtMost(256))
        }
    }

    suspend fun recycle(bitmap: Bitmap) {
        if (bitmap.isRecycled) return
        mutex.withLock {
            if (pool.size < MAX_POOL_SIZE) {
                pool.add(bitmap)
            } else {
                bitmap.recycle()
            }
        }
    }

    suspend fun clear() {
        mutex.withLock {
            pool.forEach { it.recycle() }
            pool.clear()
        }
    }
}
