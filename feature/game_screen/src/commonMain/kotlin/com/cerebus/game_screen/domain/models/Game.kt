package com.cerebus.game_screen.domain.models

import com.cerebus.core.utils.UniqueIdGenerator

data class Game(
    val id: String = UniqueIdGenerator.randomAlphanumeric(prefix = "game"),
    val user: Student,
    val deck: Deck,
)
