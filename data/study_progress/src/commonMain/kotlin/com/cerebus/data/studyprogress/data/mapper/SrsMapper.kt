package com.cerebus.data.studyprogress.data.mapper

import com.cerebus.core.game_engine.domain.model.CardProgress
import com.cerebus.core.game_engine.domain.model.ReviewLog
import com.cerebus.core.game_engine.domain.model.StudentSrsPrefs
import com.cerebus.data.studyprogress.data.entity.ReviewLogEntity
import com.cerebus.data.studyprogress.data.entity.StudentSrsPrefsEntity
import com.cerebus.data.studyprogress.data.entity.StudyProgressEntity

fun StudyProgressEntity.toDomain(): CardProgress {
    return CardProgress(
        studentId = studentId,
        cardId = cardId,
        level = level,
        dueAtEpochMillis = dueAtEpochMillis,
        recallSuccessStreak = recallSuccessStreak,
        copySuccessStreak = copySuccessStreak,
        lastReviewedAtEpochMillis = lastReviewedAtEpochMillis,
        lastHintLevel = lastHintLevel,
        lastDurationMs = lastDurationMs,
        lastWrongPressCount = lastWrongPressCount,
    )
}

fun CardProgress.toEntity(): StudyProgressEntity {
    return StudyProgressEntity(
        studentId = studentId,
        cardId = cardId,
        level = level,
        dueAtEpochMillis = dueAtEpochMillis,
        recallSuccessStreak = recallSuccessStreak,
        copySuccessStreak = copySuccessStreak,
        lastReviewedAtEpochMillis = lastReviewedAtEpochMillis,
        lastHintLevel = lastHintLevel,
        lastDurationMs = lastDurationMs,
        lastWrongPressCount = lastWrongPressCount,
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
        expectedAnswerNormalized = expectedAnswerNormalized,
        isCorrect = isCorrect,
        hintLevel = hintLevel,
        wrongPressCount = wrongPressCount,
        durationMs = durationMs,
        copyStage = copyStage,
        levelBefore = levelBefore,
        levelAfter = levelAfter,
        recallSuccessStreakBefore = recallSuccessStreakBefore,
        recallSuccessStreakAfter = recallSuccessStreakAfter,
        copySuccessStreakBefore = copySuccessStreakBefore,
        copySuccessStreakAfter = copySuccessStreakAfter,
        dueAtBeforeEpochMillis = dueAtBeforeEpochMillis,
        dueAtAfterEpochMillis = dueAtAfterEpochMillis,
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
        guidedHintSuccessThreshold = guidedHintSuccessThreshold,
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
        guidedHintSuccessThreshold = guidedHintSuccessThreshold,
    )
}
