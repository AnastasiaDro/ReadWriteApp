package com.cerebus.core.game_engine.domain.model

data class SessionState(
    val newLimit: Int,
    val reviewLimit: Int,
    val newShownCount: Int,
    val reviewShownCount: Int,
    val lastCardType: CardType?,
)
