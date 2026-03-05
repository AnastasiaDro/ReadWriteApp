package com.cerebus.core.game_engine.domain.logic

import com.cerebus.core.game_engine.domain.model.MatchType
import com.cerebus.core.game_engine.domain.model.StudentSrsPrefs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextMatchEngineTest {

    @Test
    fun normalize_russianYoAndPunctuation_returnsExpected() {
        val actual = normalize(" Ёжик! ")
        assertEquals("ежик", actual)
    }

    @Test
    fun normalize_uppercaseWithDash_returnsExpected() {
        val actual = normalize("МАМА мыла-раму")
        assertEquals("мама мыла раму", actual)
    }

    @Test
    fun similarity_equalWords_equalsOne() {
        assertEquals(1.0, similarity("кот", "кот"))
    }

    @Test
    fun levenshteinAndSimilarity_kotKto_distTwoAndSimilarityPositive() {
        val dist = levenshtein("кот", "кто")
        val sim = similarity("кот", "кто")
        assertEquals(2, dist)
        assertTrue(sim > 0.0)
    }

    @Test
    fun computeMatch_lenThreeNearForbidden_returnsWrong() {
        val prefs = StudentSrsPrefs(studentId = "s1", allowNearMatch = true, similarityThreshold = 0.85)
        val result = computeMatch(
            userInput = "код",
            expectedAnswers = listOf("кот"),
            prefs = prefs,
        )
        assertEquals(MatchType.WRONG, result.matchType)
    }

    @Test
    fun computeMatch_lenSixHighSimilarity_returnsNear() {
        val prefs = StudentSrsPrefs(studentId = "s1", allowNearMatch = true, similarityThreshold = 0.85)
        val result = computeMatch(
            userInput = "planat",
            expectedAnswers = listOf("planet"),
            prefs = prefs,
        )
        assertEquals(MatchType.NEAR, result.matchType)
        assertTrue(result.similarity >= 0.80)
    }

    @Test
    fun computeMatch_allowNearDisabled_highSimilarityNonExact_returnsWrong() {
        val prefs = StudentSrsPrefs(studentId = "s1", allowNearMatch = false, similarityThreshold = 0.85)
        val result = computeMatch(
            userInput = "planat",
            expectedAnswers = listOf("planet"),
            prefs = prefs,
        )
        assertEquals(MatchType.WRONG, result.matchType)
    }
}
