package com.cerebus.game_screen.domain.models


/*

 */
data class Deck(
    val id: String,
    val title: String,
    val cardIds: List<String>, //ссылки на карточки
)
