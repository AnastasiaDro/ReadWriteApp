package com.cerebus.game_screen.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class GameScreenSrsSessionCardsTest {

    @Test
    fun learningCardsRemainAvailableWhenDailyNewLimitIsExhausted() {
        val result = composeSrsSessionCards(
            reviewByDeck = emptyMap(),
            learningByDeck = mapOf("deck1" to listOf("L1", "L2")),
            carryoverByDeck = emptyMap(),
            unseenByDeck = mapOf("deck1" to listOf("N1", "N2")),
            reviewLimit = 0,
            newSessionLimit = 2,
            remainingDailyNewLimit = 0,
        )

        assertEquals(listOf("L1", "L2"), result)
    }

    @Test
    fun unseenCardsRespectRemainingDailyLimit() {
        val result = composeSrsSessionCards(
            reviewByDeck = emptyMap(),
            learningByDeck = emptyMap(),
            carryoverByDeck = emptyMap(),
            unseenByDeck = mapOf("deck1" to listOf("N1", "N2", "N3")),
            reviewLimit = 0,
            newSessionLimit = 3,
            remainingDailyNewLimit = 1,
        )

        assertEquals(listOf("N1"), result)
    }

    @Test
    fun learningCardsConsumeSessionNewSlotsBeforeUnseenCards() {
        val result = composeSrsSessionCards(
            reviewByDeck = emptyMap(),
            learningByDeck = mapOf("deck1" to listOf("L1")),
            carryoverByDeck = emptyMap(),
            unseenByDeck = mapOf("deck1" to listOf("N1", "N2")),
            reviewLimit = 0,
            newSessionLimit = 2,
            remainingDailyNewLimit = 2,
        )

        assertEquals(listOf("L1", "N1"), result)
    }

    @Test
    fun carryoverCardsFromTodayComeBeforeUnseenCards() {
        val result = composeSrsSessionCards(
            reviewByDeck = emptyMap(),
            learningByDeck = emptyMap(),
            carryoverByDeck = mapOf("deck1" to listOf("C1", "C2")),
            unseenByDeck = mapOf("deck1" to listOf("N1", "N2")),
            reviewLimit = 0,
            newSessionLimit = 2,
            remainingDailyNewLimit = 2,
        )

        assertEquals(listOf("C1", "C2"), result)
    }
}
