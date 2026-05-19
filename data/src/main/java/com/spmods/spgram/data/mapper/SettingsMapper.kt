package com.spmods.spgram.data.mapper

import org.drinkless.tdlib.TdApi
import com.spmods.spgram.domain.repository.NotificationSettingsRepository.TdNotificationScope

fun TdNotificationScope.toApi(): TdApi.NotificationSettingsScope = when (this) {
    TdNotificationScope.PRIVATE_CHATS -> TdApi.NotificationSettingsScopePrivateChats()
    TdNotificationScope.GROUPS -> TdApi.NotificationSettingsScopeGroupChats()
    TdNotificationScope.CHANNELS -> TdApi.NotificationSettingsScopeChannelChats()
}