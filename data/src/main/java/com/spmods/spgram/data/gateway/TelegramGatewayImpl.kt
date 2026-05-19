package com.spmods.spgram.data.gateway

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.drinkless.tdlib.TdApi
import com.spmods.spgram.data.di.TdLibClient

internal class TelegramGatewayImpl(
    private val client: TdLibClient
) : TelegramGateway {
    override suspend fun <T : TdApi.Object> execute(function: TdApi.Function<T>): T =
        client.sendSuspend(function)

    override val updates: SharedFlow<TdApi.Update>
        get() = client.updates

    override val isAuthenticated: StateFlow<Boolean>
        get() = client.isAuthenticated
}
