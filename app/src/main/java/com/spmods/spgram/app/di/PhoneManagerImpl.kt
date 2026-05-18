package com.spmods.spgram.app.di

import android.telephony.TelephonyManager
import com.spmods.spgram.domain.managers.PhoneManager

class PhoneManagerImpl(private val telephonyManager: TelephonyManager?): PhoneManager {
    override fun getSimCountryIso(): String? {
        return telephonyManager?.simCountryIso
    }
}