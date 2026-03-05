package com.cerebus.core.game_engine.domain.usecase

import com.cerebus.core.game_engine.domain.model.CardProgress
import com.cerebus.core.game_engine.domain.model.CardState
import com.cerebus.core.game_engine.domain.model.Grade
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class SubmitAnswerAndRescheduleUseCaseTest {

    @Test
    fun submitAnswerAndReschedule_writesReviewLog() = runTest {
        val progressRepository = FakeCardProgressRepository()
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
                expectedAnswers = listOf("мама"),
                userInput = "мама",
                shownAtEpochMillis = 1000L,
                submittedAtEpochMillis = 2000L,
                usedHint = false,
                attemptIndex = 1,
            )
        )

        assertEquals(1, reviewLogRepository.insertCalls)
        assertNotNull(reviewLogRepository.lastInsertedLog)
        assertEquals("student1", reviewLogRepository.lastInsertedLog?.studentId)
        assertEquals("card1", reviewLogRepository.lastInsertedLog?.cardId)
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
                expectedAnswers = listOf("мама"),
                userInput = "мама",
                shownAtEpochMillis = 1000L,
                submittedAtEpochMillis = 2000L,
                usedHint = false,
                attemptIndex = 1,
            )
        )

        assertTrue(progressRepository.atomicCalled)
        assertEquals(0, progressRepository.nonAtomicUpsertCalls)
        assertNotNull(progressRepository.atomicProgress)
        assertNotNull(progressRepository.atomicLog)
        assertEquals(0, reviewLogRepository.insertCalls)
    }
}

private class FakeCardProgressRepository : CardProgressRepository {
    var storedProgress: CardProgress? = null
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

    override suspend fun getRecentGrades(
        studentId: String,
        cardId: String,
        limit: Int,
    ): List<Grade> {
        return emptyList()
    }

    override suspend fun getRecentGradesByCards(
        studentId: String,
        cardIds: List<String>,
        limitPerCard: Int,
    ): Map<String, List<Grade>> {
        return emptyMap()
    }
}

private class FakeStudentPrefsRepository : StudentPrefsRepository {
    override suspend fun getPrefs(studentId: String): StudentSrsPrefs {
        return StudentSrsPrefs(studentId = studentId)
    }

    override suspend fun savePrefs(prefs: StudentSrsPrefs) = Unit
}

private fun runTest(block: suspend () -> Unit) {
    var failure: Throwable? = null
    block.startCoroutine(
        object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) {
                failure = result.exceptionOrNull()
            }
        }
    )
    failure?.let { throw it }
}
