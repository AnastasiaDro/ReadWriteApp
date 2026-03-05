package com.cerebus.core.game_engine.domain.repository

interface GameEngineTransactionRunner {
    suspend fun <T> inTransaction(block: suspend () -> T): T
}
