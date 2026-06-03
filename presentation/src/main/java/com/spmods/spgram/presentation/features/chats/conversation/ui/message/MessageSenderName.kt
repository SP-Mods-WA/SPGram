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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.spgram.app.ui.theme.senderColor
import com.spmods.spgram.domain.models.MessageModel
import com.spmods.spgram.presentation.R

@Composable
fun MessageSenderName(
    msg: MessageModel,
    modifier: Modifier = Modifier,
    toProfile: (Long) -> Unit = {},
) {
    // ── Unique colour per sender (Telegram 7-colour palette) ──────────────
    val nameColor = senderColor(msg.senderId)

    // Adjust lightness for dark mode: darken bright colours slightly
    val isDark    = isSystemInDarkTheme()
    val finalColor = if (isDark && nameColor.luminance() > 0.4f) {
        nameColor.copy(alpha = 0.85f)
    } else {
        nameColor
    }

    // Badge container colour derived from the sender colour (low-alpha tint)
    val badgeBg = finalColor.copy(alpha = 0.15f)

    Row(
        modifier           = modifier.padding(bottom = 4.dp),
        verticalAlignment  = Alignment.CenterVertically,
    ) {
        // 1. Sender name — unique colour per userId
        Text(
            text     = msg.senderName,
            style    = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color      = finalColor,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .clickable { toProfile(msg.senderId) },
        )

        // 2. Verified / Premium icon (tinted with sender colour)
        if (msg.isSenderVerified) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector        = Icons.Rounded.Verified,
                contentDescription = stringResource(R.string.cd_verified),
                modifier           = Modifier.size(14.dp),
                tint               = finalColor,
            )
        }

        // 3. Custom title badge (admin / owner tag)
        if (!msg.senderCustomTitle.isNullOrEmpty()) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape        = CircleShape,
                color        = badgeBg,
                contentColor = finalColor,
            ) {
                Text(
                    text     = msg.senderCustomTitle.toString(),
                    style    = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize   = 10.sp,
                        color      = finalColor,
                    ),
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
        }
    }
}
