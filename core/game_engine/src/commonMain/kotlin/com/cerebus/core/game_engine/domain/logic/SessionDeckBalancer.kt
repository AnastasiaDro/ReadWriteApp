package com.cerebus.core.game_engine.domain.logic

private const val MIN_NEW_PER_DECK_CAP = 2
private const val MAX_NEW_PER_DECK_CAP = 3

fun computeNewDeckCap(
    activeDeckCount: Int,
    newLimit: Int,
): Int {
    if (activeDeckCount <= 0 || newLimit <= 0) return MIN_NEW_PER_DECK_CAP
    if (activeDeckCount <= 2) return MAX_NEW_PER_DECK_CAP

    val idealPerDeck = (newLimit + activeDeckCount - 1) / activeDeckCount
    return idealPerDeck.coerceIn(MIN_NEW_PER_DECK_CAP, MAX_NEW_PER_DECK_CAP)
}

fun <T> takeRoundRobinByDeck(
    cardsByDeck: Map<String, List<T>>,
    limit: Int,
): List<T> {
    if (limit <= 0 || cardsByDeck.isEmpty()) return emptyList()

    val queues = cardsByDeck
        .mapValues { (_, cards) -> cards.toMutableList() }
        .toMutableMap()
    val orderedDeckIds = queues.keys.sorted()
    val result = mutableListOf<T>()

    while (result.size < limit && queues.values.any { it.isNotEmpty() }) {
        orderedDeckIds.forEach { deckId ->
            val queue = queues[deckId] ?: return@forEach
            if (queue.isNotEmpty() && result.size < limit) {
                result += queue.removeAt(0)
            }
        }
    }

    return result
}

fun <T> selectBalancedNewCardsByDeck(
    newCardsByDeck: Map<String, List<T>>,
    newLimit: Int,
): List<T> {
    if (newLimit <= 0 || newCardsByDeck.isEmpty()) return emptyList()

    val queues = newCardsByDeck
        .mapValues { (_, cards) -> cards.toMutableList() }
        .toMutableMap()
    val shownPerDeck = queues.keys.associateWith { 0 }.toMutableMap()
    val activeDeckCount = queues.count { (_, cards) -> cards.isNotEmpty() }
    val maxPerDeck = computeNewDeckCap(activeDeckCount = activeDeckCount, newLimit = newLimit)
    val result = mutableListOf<T>()

    while (result.size < newLimit && queues.values.any { it.isNotEmpty() }) {
        val nextDeckId = queues
            .filter { (_, cards) -> cards.isNotEmpty() }
            .filter { (deckId, _) -> (shownPerDeck[deckId] ?: 0) < maxPerDeck }
            .minWithOrNull(
                compareBy<Map.Entry<String, MutableList<T>>> { (deckId, _) ->
                    shownPerDeck[deckId] ?: 0
                }.thenBy { (deckId, _) -> deckId }
            )
            ?.key
            ?: break

        val queue = queues[nextDeckId] ?: break
        result += queue.removeAt(0)
        shownPerDeck[nextDeckId] = (shownPerDeck[nextDeckId] ?: 0) + 1
    }

    return result
}

fun <T> interleaveReviewAndNewCards(
    reviewCards: List<T>,
    newCards: List<T>,
    reviewToNewRatio: Int = 3,
): List<T> {
    if (reviewCards.isEmpty()) return newCards
    if (newCards.isEmpty()) return reviewCards

    val reviews = reviewCards.toMutableList()
    val news = newCards.toMutableList()
    val result = mutableListOf<T>()

    while (reviews.isNotEmpty() || news.isNotEmpty()) {
        repeat(reviewToNewRatio) {
            if (reviews.isNotEmpty()) {
                result += reviews.removeAt(0)
            }
        }
        if (news.isNotEmpty()) {
            result += news.removeAt(0)
        }
    }

    return result
}
