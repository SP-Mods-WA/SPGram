package com.spmods.spgram.domain.models

import com.spmods.spgram.domain.repository.ChatMemberStatus

data class GroupMemberModel(
    val user: UserModel,
    val rank: String? = null,
    val status: ChatMemberStatus? = null
)