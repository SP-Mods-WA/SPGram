package com.spmods.spgram.presentation.features.chats.list.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spmods.spgram.domain.models.UpdateInfo
import com.spmods.spgram.domain.models.UpdateState

/**
 * Bottom sheet shown on the main chat list screen when an update is available.
 *
 * - If [UpdateInfo.forceUpdate] == true, the sheet cannot be dismissed
 *   (no drag handle, back press consumed, outside click blocked).
 * - Otherwise it can be freely dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateBottomSheet(
    state: UpdateState,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
) {
    val updateInfo = when (state) {
        is UpdateState.UpdateAvailable -> state.info
        is UpdateState.Downloading -> null  // info only needed for forceUpdate flag
        is UpdateState.ReadyToInstall -> null
        else -> return  // nothing to show
    }

    // Retrieve forceUpdate from the last known UpdateAvailable state
    // We store it in the sheet so it doesn't change mid-download.
    val forceUpdate = updateInfo?.forceUpdate ?: false

    val sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !forceUpdate }  // block swipe-to-dismiss when forced
    )

    // Block hardware back when force update
    if (forceUpdate) {
        BackHandler(enabled = true) { /* consume — do nothing */ }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!forceUpdate) onDismiss()
        },
        sheetState = sheetState,
        dragHandle = if (forceUpdate) null else ({ BottomSheetDefaults.DragHandle() }),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        when (state) {
            is UpdateState.UpdateAvailable ->
                UpdateAvailableContent(
                    info = state.info,
                    forceUpdate = forceUpdate,
                    onDownload = onDownload,
                    onDismiss = onDismiss,
                )

            is UpdateState.Downloading ->
                DownloadingContent(
                    state = state,
                    onCancel = if (!forceUpdate) onCancel else null,
                )

            is UpdateState.ReadyToInstall ->
                ReadyToInstallContent(
                    forceUpdate = forceUpdate,
                    onInstall = onInstall,
                )

            else -> {}
        }
    }
}

@Composable
private fun UpdateAvailableContent(
    info: UpdateInfo,
    forceUpdate: Boolean,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.NewReleases,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = "Update v${info.version} available",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        if (info.changelog.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "What's new",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    info.changelog.forEach { change ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(
                                text = change.text,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }

        if (forceUpdate) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "This update is required to continue using the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!forceUpdate) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Later", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Button(
                onClick = onDownload,
                modifier = Modifier
                    .weight(if (forceUpdate) 2f else 1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Download", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DownloadingContent(
    state: UpdateState.Downloading,
    onCancel: (() -> Unit)?,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress,
        label = "download_progress",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = "Downloading update…",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${(state.progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
        )
        if (onCancel != null) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun ReadyToInstallContent(
    forceUpdate: Boolean,
    onInstall: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = "Ready to install",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "The update has been downloaded and is ready to install.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onInstall,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("Install now", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
