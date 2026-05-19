package com.spmods.spgram.data.mapper

import org.drinkless.tdlib.TdApi
import com.spmods.spgram.domain.models.ProxyTypeModel
import com.spmods.spgram.domain.repository.LinkAction

fun TdApi.SettingsSection?.toLinkSettingsType(): LinkAction.SettingsType = when (this) {
    is TdApi.SettingsSectionPrivacyAndSecurity -> LinkAction.SettingsType.PRIVACY
    is TdApi.SettingsSectionDevices -> LinkAction.SettingsType.SESSIONS
    is TdApi.SettingsSectionChatFolders -> LinkAction.SettingsType.FOLDERS
    is TdApi.SettingsSectionAppearance,
    is TdApi.SettingsSectionNotifications -> LinkAction.SettingsType.CHAT

    is TdApi.SettingsSectionDataAndStorage -> LinkAction.SettingsType.DATA_STORAGE
    is TdApi.SettingsSectionPowerSaving -> LinkAction.SettingsType.POWER_SAVING
    is TdApi.SettingsSectionPremium -> LinkAction.SettingsType.PREMIUM
    else -> LinkAction.SettingsType.MAIN
}

fun TdApi.ProxyType?.toLinkProxyTypeOrNull(): ProxyTypeModel? = when (this) {
    is TdApi.ProxyTypeMtproto -> ProxyTypeModel.Mtproto(secret)
    is TdApi.ProxyTypeSocks5 -> ProxyTypeModel.Socks5(username, password)
    is TdApi.ProxyTypeHttp -> ProxyTypeModel.Http(username, password, httpOnly)
    else -> null
}