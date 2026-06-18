package com.spmods.spgram.data.mapper

import org.drinkless.tdlib.TdApi
import com.spmods.spgram.domain.models.MessageEntity
import com.spmods.spgram.domain.models.RichText

fun TdApi.FormattedText.toChangelog(): List<RichText> {
    val text = this.text
    val markerIndex = text.indexOf("Changelog:", ignoreCase = true)
        .takeIf { it != -1 } ?: return emptyList()

    val changelogText = text.substring(markerIndex + "Changelog:".length).trimStart()
    val actualStart = text.indexOf(changelogText, markerIndex + "Changelog:".length)
    var currentOffset = actualStart

    return changelogText.lines().mapNotNull { line ->
        val trimmed = line.trim()
        val lineStart = text.indexOf(line, currentOffset)
        val trimmedStart = lineStart + line.indexOf(trimmed)
        currentOffset = lineStart + line.length

        if (trimmed.isEmpty()) return@mapNotNull null

        val numberingMatch = Regex("""^\d+\.\s*""").find(trimmed)
        val (finalText, finalStart) = if (numberingMatch != null) {
            trimmed.substring(numberingMatch.value.length) to (trimmedStart + numberingMatch.value.length)
        } else {
            trimmed to trimmedStart
        }

        val entities = this.entities?.mapNotNull { entity ->
            val overlapStart = maxOf(entity.offset, finalStart)
            val overlapEnd = minOf(entity.offset + entity.length, finalStart + finalText.length)
            if (overlapStart >= overlapEnd) return@mapNotNull null
            entity.toDomain()?.copy(
                offset = overlapStart - finalStart,
                length = overlapEnd - overlapStart
            )
        } ?: emptyList()

        RichText(finalText, entities)
    }
}

fun TdApi.TextEntity.toDomain(): MessageEntity? {
    return toMessageEntityOrNull()
}

// NOTE: TDLib-document-based update info is no longer used.
// Updates are now fetched from a GitHub-hosted JSON file via
// GitHubUpdateRemoteDataSource, which builds UpdateInfo directly
// from JSON (including the required downloadUrl). This function
// was removed because UpdateInfo no longer has a `fileId` field
// and now requires `downloadUrl`, which a TDLib document doesn't have.
