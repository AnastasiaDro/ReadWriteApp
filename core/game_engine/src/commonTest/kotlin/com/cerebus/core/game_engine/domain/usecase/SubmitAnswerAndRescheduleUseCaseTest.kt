package com.cerebus.core.game_engine.domain.usecase

import com.cerebus.core.game_engine.domain.model.CardProgress
import com.cerebus.core.game_engine.domain.model.ReviewLog
import com.cerebus.core.game_engine.domain.model.StudentSrsPrefs
import com.cerebus.core.game_engine.domain.repository.AtomicProgressLogRepository
import com.cerebus.core.game_engine.domain.repository.CardProgressRepository
import com.cerebus.core.game_engine.domain.repository.ReviewLogRepository
import com.cerebus.core.game_engine.domain.repository.StudentPrefsRepository
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class SubmitAnswerAndRescheduleUseCaseTest {

    @Test
    fun submitAnswerAndReschedule_writesReviewLog_forRecallAttempt() = runTest {
        val progressRepository = FakeCardProgressRepository()
        val reviewLogRepository = FakeReviewLogRepository()
        val prefsRepository = FakeStudentPrefsRepository()
        val useCase = SubmitAnswerAndRescheduleUseCase(
            progressRepository = progressRepository,
            studentPrefsRepository = prefsRepository,
            reviewLogRepository = reviewLogRepository,
        )

        val result = useCase(
            SubmitAnswerCommand(
                studentId = "student1",
                cardId = "card1",
                expectedAnswer = "мама",
                userInput = "мама",
                shownAtEpochMillis = 1_000L,
                submittedAtEpochMillis = 2_000L,
                hintLevel = 1,
                wrongPressCount = 2,
                durationMs = 900L,
                isRecallStage = true,
                copyStageSuccessThreshold = 3,
            ),
        )

        assertTrue(result.isCorrect)
        assertEquals(1, reviewLogRepository.insertCalls)
        assertNotNull(reviewLogRepository.lastInsertedLog)
        assertEquals("student1", reviewLogRepository.lastInsertedLog?.studentId)
        assertEquals("card1", reviewLogRepository.lastInsertedLog?.cardId)
        assertFalse(reviewLogRepository.lastInsertedLog?.copyStage ?: true)
    }

    @Test
    fun submitAnswerAndReschedule_usesAtomicWritePathWhenAvailable() = runTest {
        val progressRepository = FakeAtomicCardProgressRepository()
        val reviewLogRepository = FakeReviewLogRepository()
        val prefsRepository = FakeStudentPrefsRepository()
        val useCase = SubmitAnswerAndRescheduleUseCase(
            progressRepository = progressRepository,
            studentPrefsRepository = prefsRepository,
            reviewLogRepository = reviewLogRepository,
        )

        useCase(
            SubmitAnswerCommand(
                studentId = "student1",
                cardId = "card1",
                expectedAnswer = "мама",
                userInput = "мама",
                shownAtEpochMillis = 1_000L,
                submittedAtEpochMillis = 2_000L,
                hintLevel = 0,
                wrongPressCount = 0,
                durationMs = 1_000L,
                isRecallStage = true,
                copyStageSuccessThreshold = 3,
            ),
        )

        assertTrue(progressRepository.atomicCalled)
        assertEquals(0, progressRepository.nonAtomicUpsertCalls)
        assertNotNull(progressRepository.atomicProgress)
        assertNotNull(progressRepository.atomicLog)
        assertEquals(0, reviewLogRepository.insertCalls)
    }

    @Test
    fun submitAnswerAndReschedule_copyStage_exactWithoutHelp_incrementsCopySuccessStreak() = runTest {
        val progressRepository = FakeCardProgressRepository()
        val reviewLogRepository = FakeReviewLogRepository()
        val prefsRepository = FakeStudentPrefsRepository()
        val useCase = SubmitAnswerAndRescheduleUseCase(
            progressRepository = progressRepository,
            studentPrefsRepository = prefsRepository,
            reviewLogRepository = reviewLogRepository,
        )

        val result = useCase(
            SubmitAnswerCommand(
                studentId = "student1",
                cardId = "card1",
                expectedAnswer = "мама",
                userInput = "мама",
                shownAtEpochMillis = 1_000L,
                submittedAtEpochMillis = 2_000L,
                hintLevel = 0,
                wrongPressCount = 0,
                durationMs = 1_200L,
                isRecallStage = false,
                copyStageSuccessThreshold = 3,
            ),
        )

        assertTrue(result.isCorrect)
        assertEquals(1, result.updatedProgress.copySuccessStreak)
        assertEquals(0, result.updatedProgress.level)
        assertEquals(2_000L, result.nextDueAtEpochMillis)
        assertEquals(0, reviewLogRepository.lastInsertedLog?.levelAfter)
        assertTrue(reviewLogRepository.lastInsertedLog?.copyStage ?: false)
    }

    @Test
    fun submitAnswerAndReschedule_copyStage_withHelp_resetsCopySuccessStreak() = runTest {
        val progressRepository = FakeCardProgressRepository(
            initialProgress = progress(
                copySuccessStreak = 2,
            ),
        )
        val reviewLogRepository = FakeReviewLogRepository()
        val prefsRepository = FakeStudentPrefsRepository()
        val useCase = SubmitAnswerAndRescheduleUseCase(
            progressRepository = progressRepository,
            studentPrefsRepository = prefsRepository,
            reviewLogRepository = reviewLogRepository,
        )

        val result = useCase(
            SubmitAnswerCommand(
                studentId = "student1",
                cardId = "card1",
                expectedAnswer = "мама",
                userInput = "мама",
                shownAtEpochMillis = 1_000L,
                submittedAtEpochMillis = 2_000L,
                hintLevel = 1,
                wrongPressCount = 1,
                durationMs = 1_200L,
                isRecallStage = false,
                copyStageSuccessThreshold = 3,
            ),
        )

        assertEquals(0, result.updatedProgress.copySuccessStreak)
        assertEquals(1, result.updatedProgress.lastHintLevel)
        assertEquals(1, result.updatedProgress.lastWrongPressCount)
    }

    @Test
    fun submitAnswerAndReschedule_recallStage_onThreshold_levelsUp() = runTest {
        val progressRepository = FakeCardProgressRepository(
            initialProgress = progress(
                level = 2,
                recallSuccessStreak = 1,
            ),
        )
        val reviewLogRepository = FakeReviewLogRepository()
        val prefsRepository = FakeStudentPrefsRepository(
            prefs = StudentSrsPrefs(studentId = "student1", easyStreakRequired = 2),
        )
        val useCase = SubmitAnswerAndRescheduleUseCase(
            progressRepository = progressRepository,
            studentPrefsRepository = prefsRepository,
            reviewLogRepository = reviewLogRepository,
        )

        val result = useCase(
            SubmitAnswerCommand(
                studentId = "student1",
                cardId = "card1",
                expectedAnswer = "мама",
                userInput = "мама",
                shownAtEpochMillis = 1_000L,
                submittedAtEpochMillis = 2_000L,
                hintLevel = 1,
                wrongPressCount = 1,
                durationMs = 1_100L,
                isRecallStage = true,
                copyStageSuccessThreshold = 3,
            ),
        )

        assertEquals(3, result.updatedProgress.level)
        assertEquals(0, result.updatedProgress.recallSuccessStreak)
        assertEquals(2_000L + 10 * 60_000L, result.nextDueAtEpochMillis)
    }

    @Test
    fun submitAnswerAndReschedule_recallStage_wrongAnswer_keepsLevelAndStoresMetrics() = runTest {
        val progressRepository = FakeCardProgressRepository(
            initialProgress = progress(
                level = 3,
                recallSuccessStreak = 1,
            ),
        )
        val reviewLogRepository = FakeReviewLogRepository()
        val prefsRepository = FakeStudentPrefsRepository()
        val useCase = SubmitAnswerAndRescheduleUseCase(
            progressRepository = progressRepository,
            studentPrefsRepository = prefsRepository,
            reviewLogRepository = reviewLogRepository,
        )

        val result = useCase(
            SubmitAnswerCommand(
                studentId = "student1",
                cardId = "card1",
                expectedAnswer = "мама",
                userInput = "папа",
                shownAtEpochMillis = 1_000L,
                submittedAtEpochMillis = 2_500L,
                hintLevel = 1,
                wrongPressCount = 3,
                durationMs = 1_500L,
                isRecallStage = true,
                copyStageSuccessThreshold = 3,
            ),
        )

        assertFalse(result.isCorrect)
        assertEquals(3, result.updatedProgress.level)
        assertEquals(1, result.updatedProgress.recallSuccessStreak)
        assertEquals(1, result.updatedProgress.lastHintLevel)
        assertEquals(3, result.updatedProgress.lastWrongPressCount)
        assertEquals(2_500L, result.updatedProgress.lastReviewedAtEpochMillis)
    }
}

private class FakeCardProgressRepository(
    initialProgress: CardProgress? = null,
) : CardProgressRepository {
    var storedProgress: CardProgress? = initialProgress
    var upsertCalls: Int = 0

    override suspend fun getProgress(
        studentId: String,
        cardId: String,
    ): CardProgress? {
        return storedProgress
    }

    override fun observeProgress(studentId: String): Flow<List<CardProgress>> = flowOf(emptyList())

    override suspend fun upsertProgress(progress: CardProgress) {
        upsertCalls++
        storedProgress = progress
    }
}

private class FakeAtomicCardProgressRepository : CardProgressRepository, AtomicProgressLogRepository {
    var storedProgress: CardProgress? = null
    var nonAtomicUpsertCalls: Int = 0
    var atomicCalled: Boolean = false
    var atomicProgress: CardProgress? = null
    var atomicLog: ReviewLog? = null

    override suspend fun getProgress(
        studentId: String,
        cardId: String,
    ): CardProgress? {
        return storedProgress
    }

    override fun observeProgress(studentId: String): Flow<List<CardProgress>> = flowOf(emptyList())

    override suspend fun upsertProgress(progress: CardProgress) {
        nonAtomicUpsertCalls++
        storedProgress = progress
    }

    override suspend fun upsertProgressAndInsertLog(
        progress: CardProgress,
        log: ReviewLog,
    ) {
        atomicCalled = true
        atomicProgress = progress
        atomicLog = log
        storedProgress = progress
    }
}

private class FakeReviewLogRepository : ReviewLogRepository {
    var insertCalls: Int = 0
    var lastInsertedLog: ReviewLog? = null

    override suspend fun insertLog(log: ReviewLog) {
        insertCalls++
        lastInsertedLog = log
    }

    override suspend fun getCardIdsFirstReviewedSince(
        studentId: String,
        sinceEpochMillis: Long,
    ): Set<String> = emptySet()
}

private class FakeStudentPrefsRepository(
    private val prefs: StudentSrsPrefs = StudentSrsPrefs(studentId = "student1"),
) : StudentPrefsRepository {
    override suspend fun getPrefs(studentId: String): StudentSrsPrefs {
        return prefs.copy(studentId = studentId)
    }

    override suspend fun savePrefs(prefs: StudentSrsPrefs) = Unit
}

private fun progress(
    level: Int = 0,
    dueAtEpochMillis: Long = 1_000L,
    recallSuccessStreak: Int = 0,
    copySuccessStreak: Int = 0,
): CardProgress {
    return CardProgress(
        studentId = "student1",
        cardId = "card1",
        level = level,
        dueAtEpochMillis = dueAtEpochMillis,
        recallSuccessStreak = recallSuccessStreak,
        copySuccessStreak = copySuccessStreak,
        lastReviewedAtEpochMillis = null,
        lastHintLevel = null,
        lastDurationMs = null,
        lastWrongPressCount = 0,
    )
}

private fun runTest(block: suspend () -> Unit) {
    var failure: Throwable? = null
    block.startCoroutine(
        object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) {
                failure = result.exceptionOrNull()
            }
        },
    )
    failure?.let { throw it }
}
