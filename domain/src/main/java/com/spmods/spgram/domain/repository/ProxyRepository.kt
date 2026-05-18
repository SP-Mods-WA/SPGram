package com.spmods.spgram.domain.repository

import com.spmods.spgram.domain.models.Proxy
import com.spmods.spgram.domain.models.ProxyInput

interface ProxyRepository {
    suspend fun getProxies(): List<Proxy>
    suspend fun addProxy(input: ProxyInput, enable: Boolean): Proxy?
    suspend fun editProxy(proxyId: Int, input: ProxyInput, enable: Boolean): Proxy?
    suspend fun enableProxy(proxyId: Int): Boolean
    suspend fun disableProxy(): Boolean
    suspend fun removeProxy(proxyId: Int): Boolean
    suspend fun setDnsType(type: String)
    suspend fun setCustomDnsUrl(url: String)
    suspend fun setCustomDnsHeaders(headers: String)
    suspend fun getDnsType(): String
    suspend fun getCustomDnsUrl(): String
    suspend fun getCustomDnsHeaders(): String
}
