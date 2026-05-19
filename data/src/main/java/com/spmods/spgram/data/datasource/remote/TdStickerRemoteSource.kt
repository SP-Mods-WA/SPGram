package com.spmods.spgram.data.datasource.remote

import org.drinkless.tdlib.TdApi
import com.spmods.spgram.data.core.coRunCatching
import com.spmods.spgram.data.gateway.TelegramGateway
import com.spmods.spgram.data.mapper.toApi
import com.spmods.spgram.data.mapper.toDomain
import com.spmods.spgram.domain.models.StickerModel
import com.spmods.spgram.domain.models.StickerSetModel
import com.spmods.spgram.domain.models.StickerType

class TdStickerRemoteSource(
    private val gateway: TelegramGateway
) : StickerRemoteSource {
    private val inputLanguageCodes = buildTdInputLanguageCodes()

    override suspend fun getInstalledStickerSets(type: StickerType): List<StickerSetModel> {
        return coRunCatching {
            gateway.execute(TdApi.GetInstalledStickerSets(type.toApi()))
                .sets
                .mapNotNull { getStickerSet(it.id) }
        }.getOrDefault(emptyList())
    }

    override suspend fun getArchivedStickerSets(type: StickerType): List<StickerSetModel> {
        return coRunCatching {
            gateway.execute(TdApi.GetArchivedStickerSets(type.toApi(), 0, 100))
                .sets
                .mapNotNull { getStickerSet(it.id) }
        }.getOrDefault(emptyList())
    }

    override suspend fun getStickerSet(setId: Long): StickerSetModel? {
        return coRunCatching {
            gateway.execute(TdApi.GetStickerSet(setId)).toDomain()
        }.getOrNull()
    }

    override suspend fun getStickerSetByName(name: String): StickerSetModel? {
        return coRunCatching {
            gateway.execute(TdApi.SearchStickerSet(name, false)).toDomain()
        }.getOrNull()
    }

    override suspend fun getRecentStickers(): List<StickerModel> {
        return coRunCatching {
            gateway.execute(TdApi.GetRecentStickers(false))
                .stickers
                .map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    override suspend fun toggleStickerSetInstalled(setId: Long, isInstalled: Boolean) {
        coRunCatching { gateway.execute(TdApi.ChangeStickerSet(setId, isInstalled, false)) }
    }

    override suspend fun toggleStickerSetArchived(setId: Long, isArchived: Boolean) {
        coRunCatching { gateway.execute(TdApi.ChangeStickerSet(setId, false, isArchived)) }
    }

    override suspend fun reorderStickerSets(type: StickerType, setIds: List<Long>) {
        coRunCatching {
            gateway.execute(TdApi.ReorderInstalledStickerSets(type.toApi(), setIds.toLongArray()))
        }
    }

    override suspend fun searchStickers(query: String): List<StickerModel> {
        return coRunCatching {
            gateway.execute(
                TdApi.SearchStickers(
                    TdApi.StickerTypeRegular(),
                    "",
                    query,
                    inputLanguageCodes,
                    0,
                    100
                )
            ).stickers.map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    override suspend fun getStickerEmojiHints(query: String): List<String> {
        return coRunCatching {
            gateway.execute(
                TdApi.GetAllStickerEmojis(TdApi.StickerTypeRegular(), query, 0, false)
            ).emojis.toList()
        }.getOrDefault(emptyList())
    }

    override suspend fun searchStickerSets(query: String): List<StickerSetModel> {
        return coRunCatching {
            gateway.execute(TdApi.SearchStickerSets(TdApi.StickerTypeRegular(), query))
                .sets
                .mapNotNull { getStickerSet(it.id) }
        }.getOrDefault(emptyList())
    }

    override suspend fun clearRecentStickers() {
        coRunCatching { gateway.execute(TdApi.ClearRecentStickers()) }
    }

    override suspend fun getCustomEmojiFileId(customEmojiId: Long): Long? {
        return coRunCatching {
            val result = gateway.execute(TdApi.GetCustomEmojiStickers(longArrayOf(customEmojiId)))
            result.stickers.firstOrNull()?.sticker?.id?.toLong()
        }.getOrNull()
    }
}
