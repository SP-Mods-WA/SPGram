package com.spmods.spgram.data.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_folders")
data class ChatFolderEntity(
    @PrimaryKey val id: Int,
    val data: String,
    val order: Int
)
