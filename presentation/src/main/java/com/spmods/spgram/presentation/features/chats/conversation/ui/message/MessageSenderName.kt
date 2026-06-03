package com.spmods.spgram.presentation.features.chats.conversation.ui.message

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.spgram.domain.models.MessageModel
import com.spmods.spgram.presentation.R

// ── Telegram's 7-colour sender palette ─────────────────────────────────────
private val SENDER_PALETTE = listOf(
    Color(0xFFE17076), // 0 soft-red
    Color(0xFFFF7043), // 1 deep-orange
    Color(0xFF20CD8D), // 2 green
    Color(0xFF40A7E3), // 3 blue
    Color(0xFF00BCD4), // 4 cyan
    Color(0xFF9C27B0), // 5 purple
    Color(0xFFE91E63), // 6 pink
)

/** Returns the unique Telegram sender colour for a given [userId]. */
private fun senderNameColor(userId: Long): Color {
    val index = ((userId % SENDER_PALETTE.size) + SENDER_PALETTE.size).toInt() % SENDER_PALETTE.size
    return SENDER_PALETTE[index]
}

@Composable
fun MessageSenderName(
    msg: MessageModel,
    modifier: Modifier = Modifier,
    toProfile: (Long) -> Unit = {}
) {
    // Per-user colour — unique for each member, stable across messages
    val nameColor  = senderNameColor(msg.senderId)
    val badgeBg    = nameColor.copy(alpha = 0.15f)

    Row(
        modifier          = modifier.padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Sender name — unique colour per userId
        Text(
            text     = msg.senderName,
            style    = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color      = nameColor
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .clickable { toProfile(msg.senderId) }
        )

        // 2. Verified icon — tinted with sender colour
        if (msg.isSenderVerified) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector        = Icons.Rounded.Verified,
                contentDescription = stringResource(R.string.cd_verified),
                modifier           = Modifier.size(14.dp),
                tint               = nameColor
            )
        }

        // 3. Custom title badge (admin / owner)
        if (!msg.senderCustomTitle.isNullOrEmpty()) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape        = CircleShape,
                color        = badgeBg,
                contentColor = nameColor,
            ) {
                Text(
                    text     = msg.senderCustomTitle.toString(),
                    style    = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize   = 10.sp,
                        color      = nameColor
                    ),
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
    }
}
