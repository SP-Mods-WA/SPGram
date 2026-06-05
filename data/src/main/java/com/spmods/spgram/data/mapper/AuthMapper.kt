package com.spmods.spgram.data.mapper

import org.drinkless.tdlib.TdApi
import com.spmods.spgram.domain.repository.AuthStep

fun TdApi.AuthorizationState.toDomain(): AuthStep =
    when (this) {
        is TdApi.AuthorizationStateReady ->
            AuthStep.Ready

        is TdApi.AuthorizationStateWaitPhoneNumber ->
            AuthStep.InputPhone

        is TdApi.AuthorizationStateWaitCode ->
            AuthStep.InputCode(
                codeType   = resolveCodeTypeName(this.codeInfo.type),
                codeLength = resolveCodeLength(this.codeInfo.type),
                nextType   = this.codeInfo.nextType?.let { resolveCodeTypeName(it) },
                timeout    = this.codeInfo.timeout
            )

        is TdApi.AuthorizationStateWaitEmailCode ->
            AuthStep.InputCode(
                codeType      = "Email",
                codeLength    = this.codeInfo.length,
                isEmailCode   = true,
                emailPattern  = this.codeInfo.emailAddressPattern
            )

        is TdApi.AuthorizationStateWaitPassword ->
            AuthStep.InputPassword

        // FIX [3]: New user — show registration (name-input) screen
        is TdApi.AuthorizationStateWaitRegistration ->
            AuthStep.InputRegistration(
                termsText = this.termsOfService?.text?.text
            )

        is TdApi.AuthorizationStateWaitTdlibParameters ->
            AuthStep.WaitParameters

        else ->
            AuthStep.Loading
    }

// FIX [2]: Resolve all TDLib code types to a readable name used by CodeInputScreen
private fun resolveCodeTypeName(type: TdApi.AuthenticationCodeType): String =
    when (type) {
        is TdApi.AuthenticationCodeTypeTelegramMessage -> "TelegramMessage"
        is TdApi.AuthenticationCodeTypeSms             -> "Sms"
        is TdApi.AuthenticationCodeTypeSmsWord         -> "Sms"     // word OTP, treat as SMS
        is TdApi.AuthenticationCodeTypeSmsPhrase       -> "Sms"     // phrase OTP, treat as SMS
        is TdApi.AuthenticationCodeTypeCall            -> "Call"
        is TdApi.AuthenticationCodeTypeFlashCall       -> "FlashCall"
        is TdApi.AuthenticationCodeTypeMissedCall      -> "Call"
        is TdApi.AuthenticationCodeTypeFragment        -> "Fragment" // anonymous number
        is TdApi.AuthenticationCodeTypeFirebaseAndroid -> "Sms"     // Firebase → fallback SMS
        else                                           -> "Sms"
    }

// FIX [2]: Resolve code length for all types
private fun resolveCodeLength(type: TdApi.AuthenticationCodeType): Int =
    when (type) {
        is TdApi.AuthenticationCodeTypeTelegramMessage -> type.length
        is TdApi.AuthenticationCodeTypeSms             -> type.length
        is TdApi.AuthenticationCodeTypeSmsWord         -> type.wordLength
        is TdApi.AuthenticationCodeTypeSmsPhrase       -> type.phraseLength
        is TdApi.AuthenticationCodeTypeCall            -> type.length
        is TdApi.AuthenticationCodeTypeFlashCall       -> 0
        is TdApi.AuthenticationCodeTypeMissedCall      -> type.length
        is TdApi.AuthenticationCodeTypeFragment        -> type.length
        is TdApi.AuthenticationCodeTypeFirebaseAndroid -> type.length
        else                                           -> 5
    }
