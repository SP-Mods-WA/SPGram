package com.spmods.spgram.data.datasource.remote

import org.drinkless.tdlib.TdApi
import com.spmods.spgram.data.gateway.TelegramGateway

class TdAuthRemoteDataSource(
    private val gateway: TelegramGateway
) : AuthRemoteDataSource {

    override suspend fun setTdlibParameters(parameters: TdApi.SetTdlibParameters) {
        gateway.execute(parameters)
    }

    override suspend fun getAuthorizationState(): TdApi.AuthorizationState {
        return gateway.execute(TdApi.GetAuthorizationState())
    }

    override suspend fun setPhoneNumber(phone: String) {
        val settings = TdApi.PhoneNumberAuthenticationSettings().apply {
            isCurrentPhoneNumber  = false
            allowFlashCall        = false
            allowMissedCall       = false
            allowSmsRetrieverApi  = false
            // FIX [1]: true = Telegram always sends SMS instead of trying
            // flash/missed-call first (critical for numbers not on this SIM)
            hasUnknownPhoneNumber = true
        }
        gateway.execute(TdApi.SetAuthenticationPhoneNumber(phone, settings))
    }

    override suspend fun resendCode() {
        gateway.execute(TdApi.ResendAuthenticationCode())
    }

    override suspend fun setAuthCode(code: String) {
        gateway.execute(TdApi.CheckAuthenticationCode(code))
    }

    override suspend fun checkEmailCode(code: String) {
        gateway.execute(
            TdApi.CheckAuthenticationEmailCode(TdApi.EmailAddressAuthenticationCode(code))
        )
    }

    override suspend fun checkPassword(password: String) {
        gateway.execute(TdApi.CheckAuthenticationPassword(password))
    }

    // FIX [3]: New-user registration after code verified
    override suspend fun registerUser(firstName: String, lastName: String) {
        gateway.execute(TdApi.RegisterUser(firstName, lastName, false))
    }

    override suspend fun logout() {
        gateway.execute(TdApi.LogOut())
    }
}
