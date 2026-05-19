package com.spmods.spgram.presentation.features.chats.conversation.ui.message

import androidx.compose.runtime.Composable
import com.spmods.spgram.domain.models.MessageEntity
import com.spmods.spgram.domain.models.MessageEntityType
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.model.blockFor
import com.spmods.spgram.presentation.features.chats.conversation.ui.message.model.inlineEntitiesForBlock

@Composable
internal fun TextBlocks(
    text: String,
    entities: List<MessageEntity>,
    entity: MessageEntity,
    isOutgoing: Boolean,
) {
    when (val type = entity.type) {
        is MessageEntityType.Pre -> {
            CodeBlock(
                text = text blockFor entity,
                language = type.language,
                isOutgoing = isOutgoing
            )
        }
        is MessageEntityType.BlockQuote -> {
            QuoteBlock(
                text = text blockFor entity,
                entities = entities.inlineEntitiesForBlock(entity),
                isOutgoing = isOutgoing,
                expandable = false,
            )
        }
        is MessageEntityType.BlockQuoteExpandable -> {
            QuoteBlock(
                text = text blockFor entity,
                entities = entities.inlineEntitiesForBlock(entity),
                isOutgoing = isOutgoing,
                expandable = true,
            )
        }
        else -> {
            /***/
        }
    }
}