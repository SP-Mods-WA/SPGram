package com.spmods.spgram.data.mapper

import org.drinkless.tdlib.TdApi
import com.spmods.spgram.domain.models.AttachMenuBotIconModel
import com.spmods.spgram.domain.models.AttachMenuBotModel

fun TdApi.AttachmentMenuBot.toDomain(): AttachMenuBotModel {
    return AttachMenuBotModel(
        botUserId = this.botUserId,
        name = this.name,
        icon = AttachMenuBotIconModel(name = this.name, icon = this.androidSideMenuIcon?.toDomain()),
        requestWriteAccess = this.requestWriteAccess,
        isAdded = this.isAdded,
        showInSideMenu = this.showInSideMenu,
        showInAttachMenu = this.showInAttachmentMenu,
        showInDefaultMenu = this.showInSideMenu
    )
}
