package com.cerebus.core.game_engine.domain.logic

import com.cerebus.core.game_engine.domain.model.CardType
import com.cerebus.core.game_engine.domain.model.SchedulerQueues
import com.cerebus.core.game_engine.domain.model.SessionState
import kotlin.random.Random

private const val DEFAULT_NEW_RATIO = 3

fun shuffleReviewQueue(
    queues: SchedulerQueues,
    random: Random = Random.Default,
): SchedulerQueues {
    if (queues.dueReview.size < 2) return queues
    return queues.copy(dueReview = queues.dueReview.shuffled(random))
}

fun decideNextCard(
    state: SessionState,
    queues: SchedulerQueues,
): CardType? {
    if (queues.dueLearning.isNotEmpty()) return CardType.LEARNING
    if (queues.dueRelearning.isNotEmpty()) return CardType.RELEARNING
    if (shouldShowNew(state) && queues.newCards.isNotEmpty()) return CardType.NEW
    if (queues.dueReview.isNotEmpty()) return CardType.REVIEW
    if (queues.newCards.isNotEmpty()) return CardType.NEW
    return null
}

fun shouldShowNew(
    state: SessionState,
    ratio: Int = DEFAULT_NEW_RATIO,
): Boolean {
    return state.reviewShownCount % ratio == 0 &&
        state.newShownCount < state.newLimit
}
