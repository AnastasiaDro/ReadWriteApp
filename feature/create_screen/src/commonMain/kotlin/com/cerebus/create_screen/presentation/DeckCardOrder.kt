package com.cerebus.create_screen.presentation

import com.cerebus.data.flashcards.domain.models.Flashcard
import com.cerebus.data.preferences.domain.repositories.PreferencesRepository

internal fun PreferencesRepository.getOrderedDeckCards(
    deckId: String,
    flashcards: List<Flashcard>,
): List<Flashcard> {
    val savedOrder = getDeckCardOrder(deckId) ?: return flashcards
    return flashcards.sortedBySavedOrder(savedOrder)
}

internal fun PreferencesRepository.persistDeckCardOrder(
    deckId: String,
    orderedCardIds: List<String>,
) {
    val normalizedDeckId = deckId.takeIf { it.isNotBlank() } ?: return
    val normalizedCardIds = orderedCardIds
        .filter { it.isNotBlank() }
        .distinct()
    if (normalizedCardIds.isEmpty()) return

    setDeckCardOrder(
        deckId = normalizedDeckId,
        cardIds = normalizedCardIds,
    )
}

internal fun PreferencesRepository.removeDeletedCardsFromDeckOrder(
    deckId: String,
    deletedIds: Set<String>,
) {
    val normalizedDeckId = deckId.takeIf { it.isNotBlank() } ?: return
    if (deletedIds.isEmpty()) return

    val updatedOrder = getDeckCardOrder(normalizedDeckId)
        ?.filterNot { it in deletedIds }
        .orEmpty()
    if (updatedOrder.isEmpty()) {
        clearDeckCardOrder(normalizedDeckId)
    } else {
        setDeckCardOrder(normalizedDeckId, updatedOrder)
    }
}

internal fun List<Flashcard>.sortedBySavedOrder(savedOrder: List<String>): List<Flashcard> {
    if (isEmpty() || savedOrder.isEmpty()) return this

    val savedOrderIndex = savedOrder.withIndex().associate { it.value to it.index }
    return withIndex()
        .sortedWith(
            compareBy<IndexedValue<Flashcard>>(
                { savedOrderIndex[it.value.id] ?: Int.MAX_VALUE },
                { it.index },
            )
        )
        .map(IndexedValue<Flashcard>::value)
}
