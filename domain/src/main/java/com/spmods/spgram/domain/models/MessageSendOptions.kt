package com.spmods.spgram.domain.models

data class MessageSendOptions(
    val silent: Boolean = false,
    val scheduleDate: Int? = null,
    val sendAsDocument: Boolean = false,
    val selfDestructImmediately: Boolean = false,
    val selfDestructSeconds: Int? = null  // null = Do Not Delete, 0 = View Once, 3/10/30 = timer
)
