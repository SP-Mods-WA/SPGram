package com.spmods.spgram.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.spmods.spgram.data.db.dao.AttachBotDao
import com.spmods.spgram.data.db.dao.ChatDao
import com.spmods.spgram.data.db.dao.ChatFolderDao
import com.spmods.spgram.data.db.dao.ChatFullInfoDao
import com.spmods.spgram.data.db.dao.KeyValueDao
import com.spmods.spgram.data.db.dao.MessageDao
import com.spmods.spgram.data.db.dao.NotificationExceptionDao
import com.spmods.spgram.data.db.dao.NotificationSettingDao
import com.spmods.spgram.data.db.dao.RecentEmojiDao
import com.spmods.spgram.data.db.dao.SearchHistoryDao
import com.spmods.spgram.data.db.dao.SponsorDao
import com.spmods.spgram.data.db.dao.StickerPathDao
import com.spmods.spgram.data.db.dao.StickerSetDao
import com.spmods.spgram.data.db.dao.TextCompositionStyleDao
import com.spmods.spgram.data.db.dao.TopicDao
import com.spmods.spgram.data.db.dao.UserDao
import com.spmods.spgram.data.db.dao.UserFullInfoDao
import com.spmods.spgram.data.db.dao.WallpaperDao
import com.spmods.spgram.data.db.model.AttachBotEntity
import com.spmods.spgram.data.db.model.ChatEntity
import com.spmods.spgram.data.db.model.ChatFolderEntity
import com.spmods.spgram.data.db.model.ChatFullInfoEntity
import com.spmods.spgram.data.db.model.KeyValueEntity
import com.spmods.spgram.data.db.model.MessageEntity
import com.spmods.spgram.data.db.model.NotificationExceptionEntity
import com.spmods.spgram.data.db.model.NotificationSettingEntity
import com.spmods.spgram.data.db.model.RecentEmojiEntity
import com.spmods.spgram.data.db.model.SearchHistoryEntity
import com.spmods.spgram.data.db.model.SponsorEntity
import com.spmods.spgram.data.db.model.StickerPathEntity
import com.spmods.spgram.data.db.model.StickerSetEntity
import com.spmods.spgram.data.db.model.TextCompositionStyleEntity
import com.spmods.spgram.data.db.model.TopicEntity
import com.spmods.spgram.data.db.model.UserEntity
import com.spmods.spgram.data.db.model.UserFullInfoEntity
import com.spmods.spgram.data.db.model.WallpaperEntity

@Database(
    entities = [
        ChatEntity::class,
        MessageEntity::class,
        UserEntity::class,
        ChatFullInfoEntity::class,
        TopicEntity::class,
        UserFullInfoEntity::class,
        StickerSetEntity::class,
        RecentEmojiEntity::class,
        SearchHistoryEntity::class,
        ChatFolderEntity::class,
        AttachBotEntity::class,
        KeyValueEntity::class,
        NotificationSettingEntity::class,
        NotificationExceptionEntity::class,
        WallpaperEntity::class,
        StickerPathEntity::class,
        SponsorEntity::class,
        TextCompositionStyleEntity::class
    ],
    version = 31,
    exportSchema = false
)
abstract class SpgramDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun userDao(): UserDao
    abstract fun chatFullInfoDao(): ChatFullInfoDao
    abstract fun topicDao(): TopicDao
    abstract fun userFullInfoDao(): UserFullInfoDao
    abstract fun stickerSetDao(): StickerSetDao
    abstract fun recentEmojiDao(): RecentEmojiDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun chatFolderDao(): ChatFolderDao
    abstract fun attachBotDao(): AttachBotDao
    abstract fun keyValueDao(): KeyValueDao
    abstract fun notificationSettingDao(): NotificationSettingDao
    abstract fun notificationExceptionDao(): NotificationExceptionDao
    abstract fun wallpaperDao(): WallpaperDao
    abstract fun stickerPathDao(): StickerPathDao
    abstract fun sponsorDao(): SponsorDao
    abstract fun textCompositionStyleDao(): TextCompositionStyleDao
}
