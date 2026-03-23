package com.cerebus.core.game_engine.domain.logic

import com.cerebus.core.game_engine.domain.model.SrsConfig

data class SrsSessionCandidate<T>(
    val item: T,
    val deckId: String,
    val cardId: String,
    val progressLevel: Int?,
    val dueAtEpochMillis: Long?,
    val showHintInitially: Boolean,
)

data class SrsPlanningResult<T>(
    val reviewNow: List<T>,
    val learningNow: List<T>,
    val carryoverNow: List<T>,
    val unseenNow: List<T>,
    val laterTodayCount: Int,
    val nextDueAtEpochMillis: Long?,
    val remainingNewToday: Int,
) {
    val availableNow: Int
        get() = reviewNow.size + learningNow.size + carryoverNow.size + unseenNow.size
}

data class SrsAvailability(
    val availableNow: Int,
    val reviewNow: Int,
    val learningNow: Int,
    val carryoverNow: Int,
    val unseenNow: Int,
    val laterTodayCount: Int,
    val nextDueAtEpochMillis: Long?,
    val remainingNewToday: Int,
)

fun <T> planSrsSession(
    candidates: List<SrsSessionCandidate<T>>,
    introducedTodayCardIds: Set<String>,
    reviewLimit: Int,
    newSessionLimit: Int,
    remainingDailyNewLimit: Int,
    nowEpochMillis: Long,
    dayEndEpochMillis: Long,
    learnedLevelThreshold: Int = SrsConfig().learnedLevelThreshold,
): SrsPlanningResult<T> {
    val reviewByDeck = mutableMapOf<String, MutableList<T>>()
    val learningByDeck = mutableMapOf<String, MutableList<T>>()
    val carryoverByDeck = mutableMapOf<String, MutableList<T>>()
    val unseenByDeck = mutableMapOf<String, MutableList<T>>()

    candidates.forEach { candidate ->
        when {
            candidate.progressLevel == null -> {
                unseenByDeck.getOrPut(candidate.deckId) { mutableListOf() }.add(candidate.item)
            }

            candidate.showHintInitially -> {
                learningByDeck.getOrPut(candidate.deckId) { mutableListOf() }.add(candidate.item)
            }

            candidate.dueAtEpochMillis != null && candidate.dueAtEpochMillis <= nowEpochMillis -> {
                reviewByDeck.getOrPut(candidate.deckId) { mutableListOf() }.add(candidate.item)
            }

            candidate.dueAtEpochMillis != null &&
                candidate.cardId in introducedTodayCardIds &&
                candidate.progressLevel < learnedLevelThreshold -> {
                carryoverByDeck.getOrPut(candidate.deckId) { mutableListOf() }.add(candidate.item)
            }
        }
    }

    val safeReviewLimit = reviewLimit.coerceAtLeast(0)
    val safeNewSessionLimit = newSessionLimit.coerceAtLeast(0)
    val safeRemainingDailyNewLimit = remainingDailyNewLimit.coerceAtLeast(0)

    val selectedReview = takeRoundRobinByDeck(
        cardsByDeck = reviewByDeck,
        limit = safeReviewLimit,
    )
    val selectedLearning = takeRoundRobinByDeck(
        cardsByDeck = learningByDeck,
        limit = safeNewSessionLimit,
    )
    val remainingNewSessionSlotsAfterLearning = (safeNewSessionLimit - selectedLearning.size).coerceAtLeast(0)
    val selectedCarryover = takeRoundRobinByDeck(
        cardsByDeck = carryoverByDeck,
        limit = remainingNewSessionSlotsAfterLearning,
    )
    val remainingNewSessionSlots = (remainingNewSessionSlotsAfterLearning - selectedCarryover.size).coerceAtLeast(0)
    val selectedUnseen = selectBalancedNewCardsByDeck(
        newCardsByDeck = unseenByDeck,
        newLimit = minOf(remainingNewSessionSlots, safeRemainingDailyNewLimit),
    )

    val laterTodayCandidates = candidates.filter { candidate ->
        val progressLevel = candidate.progressLevel ?: return@filter false
        val dueAtEpochMillis = candidate.dueAtEpochMillis ?: return@filter false
        if (candidate.showHintInitially) return@filter false
        if (dueAtEpochMillis <= nowEpochMillis || dueAtEpochMillis >= dayEndEpochMillis) return@filter false
        !(candidate.cardId in introducedTodayCardIds && progressLevel < learnedLevelThreshold)
    }

    return SrsPlanningResult(
        reviewNow = selectedReview,
        learningNow = selectedLearning,
        carryoverNow = selectedCarryover,
        unseenNow = selectedUnseen,
        laterTodayCount = laterTodayCandidates.size,
        nextDueAtEpochMillis = laterTodayCandidates.minOfOrNull { candidate -> candidate.dueAtEpochMillis ?: Long.MAX_VALUE },
        remainingNewToday = safeRemainingDailyNewLimit,
    )
}

fun <T> SrsPlanningResult<T>.interleavedSessionCards(
    reviewToNewRatio: Int,
): List<T> {
    return interleaveReviewAndNewCards(
        reviewCards = reviewNow,
        newCards = learningNow + carryoverNow + unseenNow,
        reviewToNewRatio = reviewToNewRatio,
    )
}

fun <T> SrsPlanningResult<T>.toAvailability(): SrsAvailability {
    return SrsAvailability(
        availableNow = availableNow,
        reviewNow = reviewNow.size,
        learningNow = learningNow.size,
        carryoverNow = carryoverNow.size,
        unseenNow = unseenNow.size,
        laterTodayCount = laterTodayCount,
        nextDueAtEpochMillis = nextDueAtEpochMillis,
        remainingNewToday = remainingNewToday,
    )
}
