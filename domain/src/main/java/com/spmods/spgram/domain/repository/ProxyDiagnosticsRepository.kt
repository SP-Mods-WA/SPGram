package com.spmods.spgram.domain.repository

import com.spmods.spgram.domain.models.ProxyCheckResult
import com.spmods.spgram.domain.models.ProxyInput

interface ProxyDiagnosticsRepository {
    suspend fun pingProxy(proxyId: Int): ProxyCheckResult
    suspend fun testProxy(input: ProxyInput): ProxyCheckResult
    suspend fun testProxyAtDc(input: ProxyInput, dcId: Int): ProxyCheckResult
    suspend fun testDirectDc(dcId: Int): ProxyCheckResult
}
