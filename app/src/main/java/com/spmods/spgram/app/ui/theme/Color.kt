package com.spmods.spgram.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Existing brand colours (unchanged) ────────────────────────────────────
val TelegramBlue80     = Color(0xFF64B5F6)
val TelegramBlueGrey80 = Color(0xFF81A9CA)
val TelegramCyan80     = Color(0xFF4DD0E1)

val TelegramBlue40     = Color(0xFF3390EC)
val TelegramBlueGrey40 = Color(0xFF4C7599)
val TelegramCyan40     = Color(0xFF00ACC1)

// ── Bubble backgrounds ─────────────────────────────────────────────────────
/** Outgoing bubble — light theme (#EFFDDE green-tinted white, official Telegram) */
val BubbleOutgoingLight  = Color(0xFFEFFDE7)
/** Outgoing bubble — dark theme (dark blue, official Telegram dark mode) */
val BubbleOutgoingDark   = Color(0xFF2B5278)

/** Incoming bubble — light theme (plain white) */
val BubbleIncomingLight  = Color(0xFFFFFFFF)
/** Incoming bubble — dark theme (dark surface) */
val BubbleIncomingDark   = Color(0xFF212121)

// ── Bubble text / meta colours ─────────────────────────────────────────────
val BubbleOutgoingContentLight = Color(0xFF000000)
val BubbleOutgoingContentDark  = Color(0xFFFFFFFF)
val BubbleIncomingContentLight = Color(0xFF000000)
val BubbleIncomingContentDark  = Color(0xFFFFFFFF)

// ── Reaction chip colours ──────────────────────────────────────────────────
/** Chosen reaction — same in both themes (Telegram accent blue) */
val ReactionChosenBg        = Color(0xFF3390EC)
val ReactionChosenContent   = Color.White

val ReactionNeutralBgLight  = Color(0xFFEDEEEF)
val ReactionNeutralBgDark   = Color(0xFF2C2C2E)
val ReactionNeutralBorderLight = Color(0xFFDEDEDE)
val ReactionNeutralBorderDark  = Color(0xFF3A3A3C)

val ReactionNeutralContentLight = Color(0xFF000000)
val ReactionNeutralContentDark  = Color(0xFFFFFFFF)

// ── Group sender name palette ──────────────────────────────────────────────
// Telegram cycles through these 7 colours by (userId % 7)
val SenderColors = listOf(
    Color(0xFFE17076), // 0 — soft red
    Color(0xFFFF7043), // 1 — deep orange
    Color(0xFF20CD8D), // 2 — green
    Color(0xFF40A7E3), // 3 — blue
    Color(0xFF00BCD4), // 4 — cyan
    Color(0xFF9C27B0), // 5 — purple
    Color(0xFFE91E63), // 6 — pink
)

/** Returns the Telegram sender colour for [userId]. */
fun senderColor(userId: Long): Color =
    SenderColors[((userId % SenderColors.size + SenderColors.size) % SenderColors.size).toInt()]
