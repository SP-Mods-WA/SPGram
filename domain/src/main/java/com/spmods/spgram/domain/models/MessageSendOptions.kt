package com.spmods.spgram.domain.models

data class MessageSendOptions(
    val silent: Boolean = false,
    val scheduleDate: Int? = null,
    val sendAsDocument: Boolean = false,
    val selfDestructImmediately: Boolean = false
)
