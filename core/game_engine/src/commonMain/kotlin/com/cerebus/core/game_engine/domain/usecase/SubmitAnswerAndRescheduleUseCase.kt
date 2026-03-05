package com.cerebus.core.game_engine.domain.usecase

import com.cerebus.core.game_engine.domain.logic.computeGrade
import com.cerebus.core.game_engine.domain.logic.computeMatch
import com.cerebus.core.game_engine.domain.logic.scheduleNext
import com.cerebus.core.game_engine.domain.model.CardProgress
import com.cerebus.core.game_engine.domain.model.CardState
import com.cerebus.core.game_engine.domain.model.Grade
import com.cerebus.core.game_engine.domain.model.MatchType
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
    val expectedAnswers: List<String>,
    val userInput: String,
    val shownAtEpochMillis: Long,
    val submittedAtEpochMillis: Long,
    val usedHint: Boolean,
    val attemptIndex: Int,
)

data class SubmitAnswerResult(
    val grade: Grade,
    val matchType: MatchType,
    val similarity: Double,
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
            config = config,
        )

        val match = computeMatch(
            userInput = command.userInput,
            expectedAnswers = command.expectedAnswers,
            prefs = prefs,
        )

        val recentGrades = reviewLogRepository.getRecentGrades(
            studentId = command.studentId,
            cardId = command.cardId,
            limit = prefs.easyStreakRequired,
        )

        val grade = computeGrade(
            match = match,
            progress = progressBefore,
            usedHint = command.usedHint,
            attemptIndex = command.attemptIndex,
            recentGrades = recentGrades,
            prefs = prefs,
        )

        val scheduleResult = scheduleNext(
            progress = progressBefore,
            grade = grade,
            submittedAtEpochMillis = command.submittedAtEpochMillis,
            config = config,
        )

        val reviewLog = ReviewLog(
            studentId = command.studentId,
            cardId = command.cardId,
            shownAtEpochMillis = command.shownAtEpochMillis,
            submittedAtEpochMillis = command.submittedAtEpochMillis,
            userInputRaw = command.userInput,
            userInputNormalized = match.userNorm,
            bestExpectedNormalized = match.bestExpectedNorm,
            similarity = match.similarity,
            isExact = match.isExact,
            matchType = match.matchType,
            usedHint = command.usedHint,
            attemptIndex = command.attemptIndex,
            stateBefore = progressBefore.state,
            stateAfter = scheduleResult.updatedProgress.state,
            grade = grade,
            scheduledDueAtBeforeEpochMillis = progressBefore.dueAtEpochMillis,
            dueAtAfterEpochMillis = scheduleResult.dueAtEpochMillis,
            intervalBeforeDays = progressBefore.intervalDays,
            intervalAfterDays = scheduleResult.updatedProgress.intervalDays,
            easeBefore = progressBefore.ease,
            easeAfter = scheduleResult.updatedProgress.ease,
        )

        when {
            progressRepository is AtomicProgressLogRepository -> {
                progressRepository.upsertProgressAndInsertLog(
                    progress = scheduleResult.updatedProgress,
                    log = reviewLog,
                )
            }

            transactionRunner != null -> {
                transactionRunner.inTransaction {
                    progressRepository.upsertProgress(scheduleResult.updatedProgress)
                    reviewLogRepository.insertLog(reviewLog)
                }
            }

            else -> {
                progressRepository.upsertProgress(scheduleResult.updatedProgress)
                reviewLogRepository.insertLog(reviewLog)
            }
        }

        println("SRS progress upsert -> ${scheduleResult.updatedProgress}")
        println("SRS review log insert -> $reviewLog")

        return SubmitAnswerResult(
            grade = grade,
            matchType = match.matchType,
            similarity = match.similarity,
            nextDueAtEpochMillis = scheduleResult.dueAtEpochMillis,
            updatedProgress = scheduleResult.updatedProgress,
        )
    }
}

private fun defaultNewProgress(
    studentId: String,
    cardId: String,
    nowEpochMillis: Long,
    config: SrsConfig,
): CardProgress {
    return CardProgress(
        studentId = studentId,
        cardId = cardId,
        state = CardState.NEW,
        dueAtEpochMillis = nowEpochMillis,
        intervalDays = 0.0,
        ease = config.easeStart,
        learningStepIndex = 0,
        reps = 0,
        lapses = 0,
        lastReviewedAtEpochMillis = null,
        lastGrade = null,
    )
}
