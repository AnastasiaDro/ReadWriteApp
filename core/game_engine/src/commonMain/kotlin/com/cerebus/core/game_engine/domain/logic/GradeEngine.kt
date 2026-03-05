package com.cerebus.core.game_engine.domain.logic

import com.cerebus.core.game_engine.domain.model.CardProgress
import com.cerebus.core.game_engine.domain.model.CardState
import com.cerebus.core.game_engine.domain.model.Grade
import com.cerebus.core.game_engine.domain.model.MatchResult
import com.cerebus.core.game_engine.domain.model.MatchType
import com.cerebus.core.game_engine.domain.model.StudentSrsPrefs
import kotlin.math.max

fun computeGrade(
    match: MatchResult,
    progress: CardProgress,
    usedHint: Boolean,
    attemptIndex: Int,
    recentGrades: List<Grade>,
    prefs: StudentSrsPrefs,
): Grade {
    return when (match.matchType) {
        MatchType.WRONG -> Grade.AGAIN
        MatchType.NEAR -> Grade.GOOD
        MatchType.EXACT -> computeExactGrade(
            progress = progress,
            usedHint = usedHint,
            attemptIndex = attemptIndex,
            recentGrades = recentGrades,
            prefs = prefs,
        )
    }
}

private fun computeExactGrade(
    progress: CardProgress,
    usedHint: Boolean,
    attemptIndex: Int,
    recentGrades: List<Grade>,
    prefs: StudentSrsPrefs,
): Grade {
    if (progress.state != CardState.REVIEW) return Grade.GOOD
    if (usedHint) return Grade.GOOD
    if (attemptIndex > 1) return Grade.GOOD

    val streakRequired = max(1, prefs.easyStreakRequired)
    if (recentGrades.size < streakRequired) return Grade.GOOD

    val recentStreak = recentGrades.takeLast(streakRequired)
    val streakPassed = recentStreak.all { grade ->
        grade == Grade.GOOD || grade == Grade.EASY
    }

    return if (streakPassed) Grade.EASY else Grade.GOOD
}
