package com.spmods.spgram.domain.repository

sealed class ConnectionStatus {
    data object Connected : ConnectionStatus()
    data object Connecting : ConnectionStatus()
    data object Updating : ConnectionStatus()
    data object WaitingForNetwork : ConnectionStatus()
    data object ConnectingToProxy : ConnectionStatus()
}