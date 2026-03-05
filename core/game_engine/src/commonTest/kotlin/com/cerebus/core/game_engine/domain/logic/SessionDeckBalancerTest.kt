package com.cerebus.core.game_engine.domain.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionDeckBalancerTest {

    @Test
    fun computeNewDeckCap_forOneOrTwoDecks_returns3() {
        assertEquals(3, computeNewDeckCap(activeDeckCount = 1, newLimit = 10))
        assertEquals(3, computeNewDeckCap(activeDeckCount = 2, newLimit = 10))
    }

    @Test
    fun computeNewDeckCap_forManyDecks_clampsBetween2And3() {
        assertEquals(2, computeNewDeckCap(activeDeckCount = 4, newLimit = 5))
        assertEquals(3, computeNewDeckCap(activeDeckCount = 3, newLimit = 9))
    }

    @Test
    fun takeRoundRobinByDeck_preservesDeckRotation() {
        val result = takeRoundRobinByDeck(
            cardsByDeck = mapOf(
                "B" to listOf("b1", "b2"),
                "A" to listOf("a1"),
            ),
            limit = 3,
        )

        assertEquals(listOf("a1", "b1", "b2"), result)
    }

    @Test
    fun selectBalancedNewCardsByDeck_usesFirstCardAndBalancesByDeck() {
        val result = selectBalancedNewCardsByDeck(
            newCardsByDeck = mapOf(
                "A" to listOf("a1", "a2", "a3"),
                "B" to listOf("b1", "b2", "b3"),
                "C" to listOf("c1", "c2", "c3"),
            ),
            newLimit = 5,
        )

        assertEquals(listOf("a1", "b1", "c1", "a2", "b2"), result)
    }

    @Test
    fun selectBalancedNewCardsByDeck_respectsMaxPerDeckCap() {
        val result = selectBalancedNewCardsByDeck(
            newCardsByDeck = mapOf(
                "A" to listOf("a1", "a2", "a3", "a4"),
                "B" to listOf("b1", "b2", "b3", "b4"),
            ),
            newLimit = 8,
        )

        val counts = result.groupingBy { it.first().toString() }.eachCount()
        assertEquals(3, counts["a"])
        assertEquals(3, counts["b"])
    }

    @Test
    fun interleaveReviewAndNewCards_applies3to1Pattern() {
        val reviews = listOf("r1", "r2", "r3", "r4", "r5")
        val news = listOf("n1", "n2")

        val result = interleaveReviewAndNewCards(
            reviewCards = reviews,
            newCards = news,
            reviewToNewRatio = 3,
        )

        assertEquals(listOf("r1", "r2", "r3", "n1", "r4", "r5", "n2"), result)
    }

    @Test
    fun selectBalancedNewCardsByDeck_handlesUnevenDeckSizes() {
        val result = selectBalancedNewCardsByDeck(
            newCardsByDeck = mapOf(
                "A" to listOf("a1"),
                "B" to listOf("b1", "b2", "b3"),
                "C" to listOf("c1", "c2", "c3"),
            ),
            newLimit = 6,
        )

        val byDeck = result.groupingBy { it.first().toString() }.eachCount()
        assertEquals(1, byDeck["a"])
        assertTrue((byDeck["b"] ?: 0) <= 2)
        assertTrue((byDeck["c"] ?: 0) <= 2)
    }
}
