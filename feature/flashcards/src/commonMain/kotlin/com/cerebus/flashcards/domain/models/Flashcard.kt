package com.cerebus.flashcards.domain.models

/**
 * [Flashcard] - модель карточки
 *
 * @param name - название карточки, совпадает с ответом //TODO сделать английскую версию
 */
data class Flashcard(
    val id: String,
    val imageUrl: String,
    val name: String,
    val activeLetters: String = "",
    val deckId: String? = null,
)
