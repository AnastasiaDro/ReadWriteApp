package com.cerebus.core.game_engine.domain.usecase

import com.cerebus.core.game_engine.domain.logic.computeMatch
import com.cerebus.core.game_engine.domain.logic.scheduleNext
import com.cerebus.core.game_engine.domain.model.CardProgress
import com.cerebus.core.game_engine.domain.model.ReviewLog
import com.cerebus.core.game_engine.domain.model.SrsConfig
import com.cerebus.core.game_engine.domain.repository.AtomicProgressLogRepository
import com.cerebus.core.game_engine.domain.repository.CardProgressRepository
import com.cerebus.core.game_engine.domain.repository.GameEngineTransactionRunner
import com.cerebus.core.game_engine.domain.repository.ReviewLogRepository
import com.cerebus.core.game_engine.domain.repository.StudentPrefsRepository

data class SubmitAnswerCommand(
    val studentId: String,
    val cardId: String,
    val expectedAnswer: String,
    val userInput: String,
    val shownAtEpochMillis: Long,
    val submittedAtEpochMillis: Long,
    val hintLevel: Int,
    val wrongPressCount: Int,
    val durationMs: Long,
    val isRecallStage: Boolean,
    val copyStageSuccessThreshold: Int,
)

data class SubmitAnswerResult(
    val isCorrect: Boolean,
    val hintLevel: Int,
    val nextDueAtEpochMillis: Long,
    val updatedProgress: CardProgress,
)

class SubmitAnswerAndRescheduleUseCase(
    private val progressRepository: CardProgressRepository,
    private val studentPrefsRepository: StudentPrefsRepository,
    private val reviewLogRepository: ReviewLogRepository,
    private val config: SrsConfig = SrsConfig(),
    private val transactionRunner: GameEngineTransactionRunner? = null,
) {
    suspend operator fun invoke(command: SubmitAnswerCommand): SubmitAnswerResult {
        val prefs = studentPrefsRepository.getPrefs(command.studentId)
        val progressBefore = progressRepository.getProgress(
            studentId = command.studentId,
            cardId = command.cardId,
        ) ?: defaultNewProgress(
            studentId = command.studentId,
            cardId = command.cardId,
            nowEpochMillis = command.submittedAtEpochMillis,
        )

        val match = computeMatch(
            userInput = command.userInput,
            expectedAnswers = listOf(command.expectedAnswer),
            prefs = prefs,
        )
        val isCorrect = match.isExact
        val normalizedHintLevel = command.hintLevel.coerceIn(0, 3)

        val updatedProgress = if (command.isRecallStage) {
            if (!isCorrect) {
                progressBefore.copy(
                    lastReviewedAtEpochMillis = command.submittedAtEpochMillis,
                    lastHintLevel = normalizedHintLevel,
                    lastDurationMs = command.durationMs,
                    lastWrongPressCount = command.wrongPressCount,
                )
            } else {
                val scheduled = scheduleNext(
                    progress = progressBefore,
                    hintLevel = normalizedHintLevel,
                    submittedAtEpochMillis = command.submittedAtEpochMillis,
                    config = config.copy(
                        requiredRecallSuccesses = prefs.easyStreakRequired.coerceAtLeast(1),
                    ),
                )
                scheduled.updatedProgress.copy(
                    lastHintLevel = normalizedHintLevel,
                    lastDurationMs = command.durationMs,
                    lastWrongPressCount = command.wrongPressCount,
                )
            }
        } else {
            buildCopyStageProgress(
                progressBefore = progressBefore,
                isCorrect = isCorrect,
                hintLevel = normalizedHintLevel,
                submittedAtEpochMillis = command.submittedAtEpochMillis,
                durationMs = command.durationMs,
                wrongPressCount = command.wrongPressCount,
                copyStageSuccessThreshold = command.copyStageSuccessThreshold.coerceAtLeast(1),
            )
        }

        val reviewLog = ReviewLog(
            studentId = command.studentId,
            cardId = command.cardId,
            shownAtEpochMillis = command.shownAtEpochMillis,
            submittedAtEpochMillis = command.submittedAtEpochMillis,
            userInputRaw = command.userInput,
            userInputNormalized = match.userNorm,
            expectedAnswerNormalized = match.bestExpectedNorm,
            isCorrect = isCorrect,
            hintLevel = normalizedHintLevel,
            wrongPressCount = command.wrongPressCount,
            durationMs = command.durationMs,
            copyStage = !command.isRecallStage,
            levelBefore = progressBefore.level,
            levelAfter = updatedProgress.level,
            recallSuccessStreakBefore = progressBefore.recallSuccessStreak,
            recallSuccessStreakAfter = updatedProgress.recallSuccessStreak,
            copySuccessStreakBefore = progressBefore.copySuccessStreak,
            copySuccessStreakAfter = updatedProgress.copySuccessStreak,
            dueAtBeforeEpochMillis = progressBefore.dueAtEpochMillis,
            dueAtAfterEpochMillis = updatedProgress.dueAtEpochMillis,
        )

        when {
            progressRepository is AtomicProgressLogRepository -> {
                progressRepository.upsertProgressAndInsertLog(
                    progress = updatedProgress,
                    log = reviewLog,
                )
            }

            transactionRunner != null -> {
                transactionRunner.inTransaction {
                    progressRepository.upsertProgress(updatedProgress)
                    reviewLogRepository.insertLog(reviewLog)
                }
            }

            else -> {
                progressRepository.upsertProgress(updatedProgress)
                reviewLogRepository.insertLog(reviewLog)
            }
        }

        return SubmitAnswerResult(
            isCorrect = isCorrect,
            hintLevel = normalizedHintLevel,
            nextDueAtEpochMillis = updatedProgress.dueAtEpochMillis,
            updatedProgress = updatedProgress,
        )
    }
}

private fun defaultNewProgress(
    studentId: String,
    cardId: String,
    nowEpochMillis: Long,
): CardProgress {
    return CardProgress(
        studentId = studentId,
        cardId = cardId,
        level = 0,
        dueAtEpochMillis = nowEpochMillis,
        recallSuccessStreak = 0,
        copySuccessStreak = 0,
        lastReviewedAtEpochMillis = null,
        lastHintLevel = null,
        lastDurationMs = null,
        lastWrongPressCount = 0,
    )
}

private fun buildCopyStageProgress(
    progressBefore: CardProgress,
    isCorrect: Boolean,
    hintLevel: Int,
    submittedAtEpochMillis: Long,
    durationMs: Long,
    wrongPressCount: Int,
    copyStageSuccessThreshold: Int,
): CardProgress {
    val nextCopySuccessStreak = if (isCorrect && hintLevel == 0) {
        (progressBefore.copySuccessStreak + 1).coerceAtMost(copyStageSuccessThreshold)
    } else {
        0
    }
    return progressBefore.copy(
        dueAtEpochMillis = submittedAtEpochMillis,
        copySuccessStreak = nextCopySuccessStreak,
        lastReviewedAtEpochMillis = submittedAtEpochMillis,
        lastHintLevel = hintLevel,
        lastDurationMs = durationMs,
        lastWrongPressCount = wrongPressCount,
    )
}
