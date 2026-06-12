package com.spmods.spgram.data.datasource.remote

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.drinkless.tdlib.TdApi
import com.spmods.spgram.data.core.coRunCatching
import com.spmods.spgram.data.gateway.TelegramGateway
import com.spmods.spgram.data.mapper.toDomain
import com.spmods.spgram.domain.models.StickerModel

class TdEmojiRemoteSource(
    private val gateway: TelegramGateway
) : EmojiRemoteSource {
    override suspend fun getEmojiCategories(): List<String> {
        val types = listOf(
            TdApi.EmojiCategoryTypeDefault(),
            TdApi.EmojiCategoryTypeRegularStickers(),
            TdApi.EmojiCategoryTypeEmojiStatus(),
            TdApi.EmojiCategoryTypeChatPhoto()
        )

        return coroutineScope {
            types
                .map { type ->
                    async {
                        coRunCatching {
                            gateway.execute(TdApi.GetEmojiCategories(type))
                        }.getOrNull()
                    }
                }
                .awaitAll()
                .asSequence()
                .filterNotNull()
                .flatMap { it.categories.asSequence() }
                .mapNotNull { it.source as? TdApi.EmojiCategorySourceSearch }
                .flatMap { it.emojis.asSequence() }
                .toList()
        }
    }

    override suspend fun getMessageAvailableReactions(chatId: Long, messageId: Long): List<String> {
        return coRunCatching {
            val result = gateway.execute(TdApi.GetMessageAvailableReactions(chatId, messageId, 32))
            buildSet {
                result.topReactions.forEach { (it.type as? TdApi.ReactionTypeEmoji)?.let { r -> add(r.emoji) } }
                result.recentReactions.forEach { (it.type as? TdApi.ReactionTypeEmoji)?.let { r -> add(r.emoji) } }
                result.popularReactions.forEach { (it.type as? TdApi.ReactionTypeEmoji)?.let { r -> add(r.emoji) } }
            }.toList()
        }.getOrDefault(emptyList())
    }

    override suspend fun getReactionSticker(emoji: String): StickerModel? {
        return coRunCatching {
            val reaction = gateway.execute(TdApi.GetEmojiReaction(emoji))

            // Use selectAnimation — the looping animated sticker shown in the reaction picker row
            // (identical to original Telegram reaction picker behaviour)
            val sticker = reaction.selectAnimation ?: return@coRunCatching null
            val file = sticker.sticker

            // File already downloaded — return immediately
            if (file.local.isDownloadingCompleted && file.local.path.isNotEmpty()) {
                return@coRunCatching sticker.toDomain()
            }

            // File not on disk yet — download synchronously (small .tgs file, typically < 50 KB).
            // When synchronous=true, TDLib blocks the coroutine until the download completes
            // and returns the TdApi.File object with local.path filled in.
            gateway.execute(
                TdApi.DownloadFile(file.id, /* priority= */ 32, 0L, 0L, /* synchronous= */ true)
            )

            // Re-fetch reaction so the embedded sticker.sticker.local.path is up to date
            val updated = gateway.execute(TdApi.GetEmojiReaction(emoji))
            updated.selectAnimation?.toDomain()
        }.getOrNull()
    }

    override suspend fun searchEmojis(query: String): List<String> {
        return coRunCatching {
            gateway.execute(TdApi.SearchEmojis(query, emptyArray()))
                .emojiKeywords
                .map { it.emoji }
        }.getOrDefault(emptyList())
    }

    override suspend fun searchCustomEmojis(query: String): List<StickerModel> {
        return coRunCatching {
            gateway.execute(
                TdApi.SearchStickers(TdApi.StickerTypeCustomEmoji(), "", query, emptyArray(), 0, 100)
            ).stickers.map { it.toDomain() }
        }.getOrDefault(emptyList())
    }
}
