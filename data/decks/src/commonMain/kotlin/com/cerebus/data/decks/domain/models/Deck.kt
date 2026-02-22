package com.cerebus.data.decks.domain.models

data class Deck(
    val id: String,
    val name: String,
    val coverUri: String? = null,
)
