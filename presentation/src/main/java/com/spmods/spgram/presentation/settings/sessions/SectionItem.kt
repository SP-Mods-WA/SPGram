package com.spmods.spgram.presentation.settings.sessions

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.spmods.spgram.domain.models.SessionModel
import com.spmods.spgram.presentation.core.ui.SectionHeader

internal fun LazyListScope.sessionSection(
    @StringRes titleRes: Int,
    items: List<SessionModel>,
    onTerminateClick: ((SessionModel) -> Unit)?,
) {
    if (items.isNotEmpty()) {
        item(key = titleRes) {
            SectionHeader(stringResource(titleRes))
        }
        items(
            items = items,
            key = { it.id }
        ) { session ->
            SessionItem(
                session = session,
                modifier = Modifier.animateItem(),
                isPending = session.isUnconfirmed,
                onTerminate = { onTerminateClick?.invoke(session) }
            )
        }
    }
}