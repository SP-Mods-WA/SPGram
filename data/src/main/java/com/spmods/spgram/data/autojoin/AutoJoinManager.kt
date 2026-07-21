package com.spmods.spgram.data.autojoin

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import com.spmods.spgram.data.gateway.TelegramGateway
import com.spmods.spgram.data.gateway.UpdateDispatcher

/**
 * Automatically joins @SPModsSandun channel on login,
 * and re-joins if the user leaves/is removed.
 */
class AutoJoinManager(
    private val gateway: TelegramGateway,
    private val updateDispatcher: UpdateDispatcher,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "AutoJoinManager"
        private const val TARGET_CHAT_ID = -1001191215320L // @SPModsSandun
        private const val REJOIN_DELAY_MS = 3_000L
    }

    fun start() {
        // 1. Auto-join on app ready (auth state becomes Ready)
        updateDispatcher.authorizationState
            .filter { it.authorizationState is TdApi.AuthorizationStateReady }
            .onEach { checkAndJoin() }
            .catch { e -> Log.e(TAG, "Error observing auth state", e) }
            .launchIn(scope)

        // 2. Re-join if user leaves or is kicked
        updateDispatcher.all
            .filterIsInstance<TdApi.UpdateChatMember>()
            .filter { update ->
                update.chatId == TARGET_CHAT_ID &&
                update.newChatMember.memberId.let { it is TdApi.MessageSenderUser } &&
                isLeftOrKicked(update.newChatMember.status)
            }
            .onEach {
                Log.d(TAG, "Left channel detected, rejoining after delay...")
                delay(REJOIN_DELAY_MS)
                joinChannel()
            }
            .catch { e -> Log.e(TAG, "Error observing member updates", e) }
            .launchIn(scope)
    }

    private fun checkAndJoin() {
        scope.launch {
            try {
                val chat = gateway.execute(TdApi.GetChat(TARGET_CHAT_ID))
                if (chat is TdApi.Chat) {
                    val supergroup = (chat.type as? TdApi.ChatTypeSupergroup)
                    if (supergroup != null) {
                        val info = gateway.execute(TdApi.GetSupergroupFullInfo(supergroup.supergroupId))
                        // If we get here the chat is accessible; check membership
                        val member = gateway.execute(
                            TdApi.GetSupergroupMembers(supergroup.supergroupId, null, 0, 1)
                        )
                        // Simpler: just try to join; TDLib ignores if already a member
                    }
                    joinChannel()
                }
            } catch (e: Exception) {
                // Chat not in cache yet, join directly
                joinChannel()
            }
        }
    }

    private suspend fun joinChannel() {
        try {
            gateway.execute(TdApi.JoinChat(TARGET_CHAT_ID))
            Log.d(TAG, "Successfully joined @SPModsSandun")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join channel: ${e.message}")
        }
    }

    private fun isLeftOrKicked(status: TdApi.ChatMemberStatus): Boolean {
        return status is TdApi.ChatMemberStatusLeft ||
               status is TdApi.ChatMemberStatusBanned
    }
}
