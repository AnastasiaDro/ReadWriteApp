package com.cerebus.data.studyprogress.data.mapper

import com.cerebus.core.game_engine.domain.model.CardProgress
import com.cerebus.core.game_engine.domain.model.CardState
import com.cerebus.core.game_engine.domain.model.Grade
import com.cerebus.core.game_engine.domain.model.MatchType
import com.cerebus.core.game_engine.domain.model.ReviewLog
import com.cerebus.core.game_engine.domain.model.StudentSrsPrefs
import com.cerebus.data.studyprogress.data.entity.ReviewLogEntity
import com.cerebus.data.studyprogress.data.entity.StudentSrsPrefsEntity
import com.cerebus.data.studyprogress.data.entity.StudyProgressEntity

fun StudyProgressEntity.toDomain(): CardProgress {
    return CardProgress(
        studentId = studentId,
        cardId = cardId,
        state = CardState.entries[state],
        dueAtEpochMillis = dueAtEpochMillis,
        intervalDays = intervalDays,
        ease = ease,
        learningStepIndex = learningStepIndex,
        reps = reps,
        lapses = lapses,
        lastReviewedAtEpochMillis = lastReviewedAtEpochMillis,
        lastGrade = lastGrade?.let { Grade.entries[it] },
    )
}

fun CardProgress.toEntity(): StudyProgressEntity {
    return StudyProgressEntity(
        studentId = studentId,
        cardId = cardId,
        state = state.ordinal,
        dueAtEpochMillis = dueAtEpochMillis,
        intervalDays = intervalDays,
        ease = ease,
        learningStepIndex = learningStepIndex,
        reps = reps,
        lapses = lapses,
        lastReviewedAtEpochMillis = lastReviewedAtEpochMillis,
        lastGrade = lastGrade?.ordinal,
    )
}

fun ReviewLog.toEntity(): ReviewLogEntity {
    return ReviewLogEntity(
        studentId = studentId,
        cardId = cardId,
        shownAtEpochMillis = shownAtEpochMillis,
        submittedAtEpochMillis = submittedAtEpochMillis,
        userInputRaw = userInputRaw,
        userInputNormalized = userInputNormalized,
        bestExpectedNormalized = bestExpectedNormalized,
        similarity = similarity,
        isExact = isExact,
        matchType = matchType.ordinal,
        usedHint = usedHint,
        attemptIndex = attemptIndex,
        stateBefore = stateBefore.ordinal,
        stateAfter = stateAfter.ordinal,
        grade = grade.ordinal,
        scheduledDueAtBeforeEpochMillis = scheduledDueAtBeforeEpochMillis,
        dueAtAfterEpochMillis = dueAtAfterEpochMillis,
        intervalBeforeDays = intervalBeforeDays,
        intervalAfterDays = intervalAfterDays,
        easeBefore = easeBefore,
        easeAfter = easeAfter,
    )
}

fun StudentSrsPrefsEntity.toDomain(): StudentSrsPrefs {
    return StudentSrsPrefs(
        studentId = studentId,
        newCardsPerSession = newCardsPerSession,
        reviewsPerSession = reviewsPerSession,
        learnMoreStep = learnMoreStep,
        maxNewCardsPerDay = maxNewCardsPerDay,
        allowNearMatch = allowNearMatch,
        similarityThreshold = similarityThreshold,
        easyStreakRequired = easyStreakRequired,
    )
}

fun StudentSrsPrefs.toEntity(): StudentSrsPrefsEntity {
    return StudentSrsPrefsEntity(
        studentId = studentId,
        newCardsPerSession = newCardsPerSession,
        reviewsPerSession = reviewsPerSession,
        learnMoreStep = learnMoreStep,
        maxNewCardsPerDay = maxNewCardsPerDay,
        allowNearMatch = allowNearMatch,
        similarityThreshold = similarityThreshold,
        easyStreakRequired = easyStreakRequired,
    )
}

fun Int.toGrade(): Grade {
    return Grade.entries.getOrElse(this) { Grade.GOOD }
}

fun MatchType.toDbValue(): Int {
    return ordinal
}
