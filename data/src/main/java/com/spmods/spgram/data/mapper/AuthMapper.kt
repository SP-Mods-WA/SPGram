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

        is TdApi.AuthorizationStateWaitRegistration ->
            AuthStep.InputRegistration(
                termsText = this.termsOfService?.text?.text
            )

        is TdApi.AuthorizationStateWaitTdlibParameters ->
            AuthStep.WaitParameters

        else ->
            AuthStep.Loading
    }

private fun resolveCodeTypeName(type: TdApi.AuthenticationCodeType): String =
    when (type) {
        is TdApi.AuthenticationCodeTypeTelegramMessage -> "TelegramMessage"
        is TdApi.AuthenticationCodeTypeSms             -> "Sms"
        is TdApi.AuthenticationCodeTypeSmsWord         -> "Sms"
        is TdApi.AuthenticationCodeTypeSmsPhrase       -> "Sms"
        is TdApi.AuthenticationCodeTypeCall            -> "Call"
        is TdApi.AuthenticationCodeTypeFlashCall       -> "FlashCall"
        is TdApi.AuthenticationCodeTypeMissedCall      -> "Call"
        is TdApi.AuthenticationCodeTypeFragment        -> "Fragment"
        is TdApi.AuthenticationCodeTypeFirebaseAndroid -> "Sms"
        else                                           -> "Sms"
    }

private fun resolveCodeLength(type: TdApi.AuthenticationCodeType): Int =
    when (type) {
        is TdApi.AuthenticationCodeTypeTelegramMessage -> type.length
        is TdApi.AuthenticationCodeTypeSms             -> type.length
        is TdApi.AuthenticationCodeTypeSmsWord         -> resolveWordLength(type)
        is TdApi.AuthenticationCodeTypeSmsPhrase       -> resolvePhraseLength(type)
        is TdApi.AuthenticationCodeTypeCall            -> type.length
        is TdApi.AuthenticationCodeTypeFlashCall       -> 0
        is TdApi.AuthenticationCodeTypeMissedCall      -> type.length
        is TdApi.AuthenticationCodeTypeFragment        -> type.length
        is TdApi.AuthenticationCodeTypeFirebaseAndroid -> type.length
        else                                           -> 5
    }

// Safe helpers — use reflection to read the property if the TDLib version exposes it,
// otherwise fall back gracefully so the build never fails on an older/newer TDLib jar.
private fun resolveWordLength(type: TdApi.AuthenticationCodeTypeSmsWord): Int =
    try {
        type.javaClass.getField("wordLength").getInt(type)
    } catch (_: Exception) {
        try { type.javaClass.getField("length").getInt(type) } catch (_: Exception) { 0 }
    }

private fun resolvePhraseLength(type: TdApi.AuthenticationCodeTypeSmsPhrase): Int =
    try {
        type.javaClass.getField("phraseLength").getInt(type)
    } catch (_: Exception) {
        try { type.javaClass.getField("length").getInt(type) } catch (_: Exception) { 0 }
    }
