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
 * Automatically joins @SPModsSandun channel when:
 *  1. User logs in (isAuthenticated becomes true)
 *  2. User leaves or is removed from the channel
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
        // 1. Auto-join when user becomes authenticated (uses StateFlow — never misses the event)
        gateway.isAuthenticated
            .filter { it }
            .onEach {
                Log.d(TAG, "User authenticated, checking channel membership...")
                joinChannel()
            }
            .catch { e -> Log.e(TAG, "Error observing auth state", e) }
            .launchIn(scope)

        // 2. Re-join if user leaves or is kicked — UpdateMyChatMember is the correct update
        updateDispatcher.all
            .filterIsInstance<TdApi.UpdateMyChatMember>()
            .filter { update ->
                update.chatId == TARGET_CHAT_ID &&
                isLeftOrKicked(update.newChatMember.status)
            }
            .onEach {
                Log.d(TAG, "Left/removed from channel, rejoining in ${REJOIN_DELAY_MS}ms...")
                delay(REJOIN_DELAY_MS)
                joinChannel()
            }
            .catch { e -> Log.e(TAG, "Error observing member updates", e) }
            .launchIn(scope)
    }

    private fun joinChannel() {
        scope.launch {
            try {
                gateway.execute(TdApi.JoinChat(TARGET_CHAT_ID))
                Log.d(TAG, "Successfully joined @SPModsSandun")
            } catch (e: Exception) {
                // Already a member or temporary error — both are fine
                Log.d(TAG, "Join attempt result: ${e.message}")
            }
        }
    }

    private fun isLeftOrKicked(status: TdApi.ChatMemberStatus): Boolean {
        return status is TdApi.ChatMemberStatusLeft ||
               status is TdApi.ChatMemberStatusBanned
    }
}
