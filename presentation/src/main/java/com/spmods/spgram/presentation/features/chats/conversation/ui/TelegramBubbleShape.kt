package com.spmods.spgram.presentation.features.chats.conversation.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Telegram-style message bubble shape with a smooth pointed tail.
 *
 * When [hasTail] is true (last/standalone message in a sender group),
 * the sender-side bottom corner becomes a bezier-curved pointed tail
 * instead of a rounded arc — matching the exact Telegram visual.
 *
 *  Outgoing:  tail at bottom-RIGHT corner
 *  Incoming:  tail at bottom-LEFT  corner
 *
 * Grouping logic:
 *  - [isSameSenderAbove] = message above is same sender → TOP corner on
 *    sender side uses [smallCorner] radius (visually "flat" edge effect)
 *  - [hasTail] = !isSameSenderBelow → bottom sender-side corner is the tail
 */
class TelegramBubbleShape(
    private val isOutgoing: Boolean,
    private val hasTail: Boolean,
    private val isSameSenderAbove: Boolean,
    private val cornerRadius: Dp = 18.dp,
    private val smallCorner: Dp = 5.dp,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(
        buildPath(
            size = size,
            r = with(density) { cornerRadius.toPx() },
            s = with(density) { smallCorner.toPx() },
        )
    )

    private fun buildPath(size: Size, r: Float, s: Float): Path {
        val w = size.width
        val h = size.height

        // Safety: never exceed half the dimension
        val safeR = r.coerceAtMost(minOf(w, h) / 2f)
        val safeS = s.coerceAtMost(safeR)

        return Path().apply {
            if (isOutgoing) {
                // ─────────────────────────────────────────
                //  OUTGOING  (tail → bottom-right corner)
                // ─────────────────────────────────────────
                val topL = safeR                                    // top-left  : always full
                val topR = if (isSameSenderAbove) safeS else safeR // top-right : small if grouped above

                // Start after top-left arc
                moveTo(topL, 0f)
                // Top edge
                lineTo(w - topR, 0f)
                // ↗ Top-right corner
                arcTo(Rect(w - topR * 2, 0f, w, topR * 2), -90f, 90f, false)

                if (hasTail) {
                    // Right side straight down, then tail bezier
                    lineTo(w, h - safeR * 0.65f)
                    // Tail: smooth concave pinch → pointed tip at bottom-right
                    cubicTo(
                        w,              h - safeR * 0.2f,   // control 1
                        w - safeR * 0.2f, h,               // control 2
                        w - safeR * 0.65f, h                // end point
                    )
                } else {
                    // Right side → small bottom-right corner
                    lineTo(w, h - safeS)
                    arcTo(Rect(w - safeS * 2, h - safeS * 2, w, h), 0f, 90f, false)
                }

                // Bottom edge (right → left)
                lineTo(safeR, h)
                // ↙ Bottom-left corner : always full radius
                arcTo(Rect(0f, h - safeR * 2, safeR * 2, h), 90f, 90f, false)
                // Left side
                lineTo(0f, topL)
                // ↖ Top-left corner : always full radius
                arcTo(Rect(0f, 0f, topL * 2, topL * 2), 180f, 90f, false)

            } else {
                // ─────────────────────────────────────────
                //  INCOMING  (tail → bottom-left corner)
                // ─────────────────────────────────────────
                val topL = if (isSameSenderAbove) safeS else safeR // top-left  : small if grouped above
                val topR = safeR                                    // top-right : always full

                moveTo(topL, 0f)
                // Top edge
                lineTo(w - topR, 0f)
                // ↗ Top-right corner : always full
                arcTo(Rect(w - topR * 2, 0f, w, topR * 2), -90f, 90f, false)
                // Right side
                lineTo(w, h - safeR)
                // ↘ Bottom-right corner : always full
                arcTo(Rect(w - safeR * 2, h - safeR * 2, w, h), 0f, 90f, false)

                if (hasTail) {
                    // Bottom edge → tail bezier at bottom-left
                    lineTo(safeR * 0.65f, h)
                    // Tail: smooth concave pinch → pointed tip at bottom-left
                    cubicTo(
                        safeR * 0.2f, h,                // control 1
                        0f,           h - safeR * 0.2f,  // control 2
                        0f,           h - safeR * 0.65f  // end point
                    )
                } else {
                    // Bottom edge → small bottom-left corner
                    lineTo(safeS, h)
                    arcTo(Rect(0f, h - safeS * 2, safeS * 2, h), 90f, 90f, false)
                }

                // Left side
                lineTo(0f, topL)
                // ↖ Top-left corner
                arcTo(Rect(0f, 0f, topL * 2, topL * 2), 180f, 90f, false)
            }

            close()
        }
    }
}
