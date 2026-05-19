package com.spmods.spgram.data.notifications

import com.spmods.spgram.domain.repository.NotificationSettingsRepository.TdNotificationScope

data class NotificationScopeState(
    val loadedScopes: Set<TdNotificationScope>,
    val enabledByScope: Map<TdNotificationScope, Boolean>
)
