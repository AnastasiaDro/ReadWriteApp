package com.cerebus.core.game_engine.domain.logic

import com.cerebus.core.game_engine.domain.model.CardProgress
import com.cerebus.core.game_engine.domain.model.CardState
import com.cerebus.core.game_engine.domain.model.Grade
import com.cerebus.core.game_engine.domain.model.MatchResult
import com.cerebus.core.game_engine.domain.model.MatchType
import com.cerebus.core.game_engine.domain.model.StudentSrsPrefs
import kotlin.test.Test
import kotlin.test.assertEquals

class GradeEngineTest {

    private val prefs = StudentSrsPrefs(studentId = "s1", easyStreakRequired = 2)

    @Test
    fun computeGrade_wrongMatch_returnsAgain() {
        val grade = computeGrade(
            match = match(MatchType.WRONG),
            progress = progress(CardState.REVIEW),
            usedHint = false,
            attemptIndex = 1,
            recentGrades = listOf(Grade.GOOD, Grade.GOOD),
            prefs = prefs,
        )
        assertEquals(Grade.AGAIN, grade)
    }

    @Test
    fun computeGrade_nearMatch_returnsGood() {
        val grade = computeGrade(
            match = match(MatchType.NEAR),
            progress = progress(CardState.REVIEW),
            usedHint = false,
            attemptIndex = 1,
            recentGrades = listOf(Grade.GOOD, Grade.GOOD),
            prefs = prefs,
        )
        assertEquals(Grade.GOOD, grade)
    }

    @Test
    fun computeGrade_exactLearning_returnsGood() {
        val grade = computeGrade(
            match = match(MatchType.EXACT),
            progress = progress(CardState.LEARNING),
            usedHint = false,
            attemptIndex = 1,
            recentGrades = listOf(Grade.GOOD, Grade.GOOD),
            prefs = prefs,
        )
        assertEquals(Grade.GOOD, grade)
    }

    @Test
    fun computeGrade_exactReviewWithHint_returnsGood() {
        val grade = computeGrade(
            match = match(MatchType.EXACT),
            progress = progress(CardState.REVIEW),
            usedHint = true,
            attemptIndex = 1,
            recentGrades = listOf(Grade.GOOD, Grade.GOOD),
            prefs = prefs,
        )
        assertEquals(Grade.GOOD, grade)
    }

    @Test
    fun computeGrade_exactReviewAttemptMoreThanOne_returnsGood() {
        val grade = computeGrade(
            match = match(MatchType.EXACT),
            progress = progress(CardState.REVIEW),
            usedHint = false,
            attemptIndex = 2,
            recentGrades = listOf(Grade.GOOD, Grade.GOOD),
            prefs = prefs,
        )
        assertEquals(Grade.GOOD, grade)
    }

    @Test
    fun computeGrade_exactReviewWithGoodStreak_returnsEasy() {
        val grade = computeGrade(
            match = match(MatchType.EXACT),
            progress = progress(CardState.REVIEW),
            usedHint = false,
            attemptIndex = 1,
            recentGrades = listOf(Grade.GOOD, Grade.GOOD),
            prefs = prefs,
        )
        assertEquals(Grade.EASY, grade)
    }

    private fun progress(state: CardState): CardProgress {
        return CardProgress(
            studentId = "s1",
            cardId = "c1",
            state = state,
            dueAtEpochMillis = 0L,
            intervalDays = 1.0,
            ease = 2.3,
            learningStepIndex = 0,
            reps = 0,
            lapses = 0,
            lastReviewedAtEpochMillis = null,
            lastGrade = null,
        )
    }

    private fun match(type: MatchType): MatchResult {
        return MatchResult(
            userNorm = "abc",
            bestExpectedNorm = "abc",
            similarity = if (type == MatchType.WRONG) 0.0 else 1.0,
            isExact = type == MatchType.EXACT,
            matchType = type,
        )
    }
}
