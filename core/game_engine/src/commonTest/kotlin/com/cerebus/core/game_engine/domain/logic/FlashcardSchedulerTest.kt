package com.cerebus.core.game_engine.domain.logic

import com.cerebus.core.game_engine.domain.model.CardType
import com.cerebus.core.game_engine.domain.model.SchedulerQueues
import com.cerebus.core.game_engine.domain.model.SessionState
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FlashcardSchedulerTest {

    @Test
    fun decideNextCard_dueLearning_hasHighestPriority() {
        val next = decideNextCard(
            state = state(),
            queues = queues(
                dueLearning = listOf("c1"),
                dueRelearning = listOf("c2"),
                dueReview = listOf("c3"),
                newCards = listOf("c4"),
            ),
        )
        assertEquals(CardType.LEARNING, next)
    }

    @Test
    fun decideNextCard_dueRelearning_hasPriorityAfterLearning() {
        val next = decideNextCard(
            state = state(),
            queues = queues(
                dueRelearning = listOf("c2"),
                dueReview = listOf("c3"),
                newCards = listOf("c4"),
            ),
        )
        assertEquals(CardType.RELEARNING, next)
    }

    @Test
    fun decideNextCard_shouldShowNew_true_returnsNew() {
        val next = decideNextCard(
            state = state(
                reviewShownCount = 3,
                newShownCount = 0,
                newLimit = 10,
            ),
            queues = queues(
                dueReview = listOf("c3"),
                newCards = listOf("c4"),
            ),
        )
        assertEquals(CardType.NEW, next)
    }

    @Test
    fun decideNextCard_shouldShowNew_false_and_reviewAvailable_returnsReview() {
        val next = decideNextCard(
            state = state(
                reviewShownCount = 1,
                newShownCount = 0,
            ),
            queues = queues(
                dueReview = listOf("c3"),
                newCards = listOf("c4"),
            ),
        )
        assertEquals(CardType.REVIEW, next)
    }

    @Test
    fun decideNextCard_whenNoReviewButNewAvailable_returnsNew() {
        val next = decideNextCard(
            state = state(
                reviewShownCount = 1,
                newShownCount = 0,
            ),
            queues = queues(
                newCards = listOf("c4"),
            ),
        )
        assertEquals(CardType.NEW, next)
    }

    @Test
    fun decideNextCard_whenAllQueuesEmpty_returnsNull() {
        val next = decideNextCard(
            state = state(),
            queues = queues(),
        )
        assertEquals(null, next)
    }

    @Test
    fun shouldShowNew_whenReviewCountMultipleOfRatio_andUnderLimit_returnsTrue() {
        assertTrue(
            shouldShowNew(
                state = state(
                    reviewShownCount = 6,
                    newShownCount = 1,
                    newLimit = 5,
                ),
            )
        )
    }

    @Test
    fun shouldShowNew_whenReviewCountNotMultipleOfRatio_returnsFalse() {
        assertFalse(
            shouldShowNew(
                state = state(
                    reviewShownCount = 5,
                    newShownCount = 1,
                    newLimit = 5,
                ),
            )
        )
    }

    @Test
    fun shouldShowNew_whenNewLimitReached_returnsFalse() {
        assertFalse(
            shouldShowNew(
                state = state(
                    reviewShownCount = 6,
                    newShownCount = 5,
                    newLimit = 5,
                ),
            )
        )
    }

    @Test
    fun shuffleReviewQueue_keepsSameCardsAndCount() {
        val source = queues(
            dueReview = listOf("A", "B", "C", "D", "E"),
        )

        val shuffled = shuffleReviewQueue(
            queues = source,
            random = Random(12345),
        )

        assertEquals(source.dueReview.size, shuffled.dueReview.size)
        assertEquals(source.dueReview.toSet(), shuffled.dueReview.toSet())
    }

    @Test
    fun shuffleReviewQueue_withSameSeed_isDeterministic() {
        val source = queues(
            dueReview = listOf("A", "B", "C", "D", "E"),
        )

        val shuffled1 = shuffleReviewQueue(source, Random(42))
        val shuffled2 = shuffleReviewQueue(source, Random(42))

        assertEquals(shuffled1.dueReview, shuffled2.dueReview)
        assertNotEquals(source.dueReview, shuffled1.dueReview)
    }

    private fun state(
        newLimit: Int = 10,
        reviewLimit: Int = 100,
        newShownCount: Int = 0,
        reviewShownCount: Int = 0,
        lastCardType: CardType? = null,
    ): SessionState {
        return SessionState(
            newLimit = newLimit,
            reviewLimit = reviewLimit,
            newShownCount = newShownCount,
            reviewShownCount = reviewShownCount,
            lastCardType = lastCardType,
        )
    }

    private fun queues(
        dueLearning: List<String> = emptyList(),
        dueRelearning: List<String> = emptyList(),
        dueReview: List<String> = emptyList(),
        newCards: List<String> = emptyList(),
    ): SchedulerQueues {
        return SchedulerQueues(
            dueLearning = dueLearning,
            dueRelearning = dueRelearning,
            dueReview = dueReview,
            newCards = newCards,
        )
    }
}
