package com.cerebus.core.game_engine.domain.logic

import com.cerebus.core.game_engine.domain.model.MatchResult
import com.cerebus.core.game_engine.domain.model.MatchType
import com.cerebus.core.game_engine.domain.model.StudentSrsPrefs
import kotlin.math.max

private val punctuationRegex = Regex("[\\.,!?;:\"'\\-]")
private val spacesRegex = Regex("\\s+")

fun normalize(text: String): String {
    return text
        .lowercase()
        .trim()
        .replace('ё', 'е')
        .replace(punctuationRegex, " ")
        .replace(spacesRegex, " ")
        .trim()
}

fun levenshtein(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length

    val previous = IntArray(b.length + 1) { it }
    val current = IntArray(b.length + 1)

    for (i in 1..a.length) {
        current[0] = i
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            current[j] = minOf(
                current[j - 1] + 1,
                previous[j] + 1,
                previous[j - 1] + cost,
            )
        }
        for (j in previous.indices) {
            previous[j] = current[j]
        }
    }

    return previous[b.length]
}

fun similarity(a: String, b: String): Double {
    val maxLen = max(a.length, b.length)
    if (maxLen == 0) return 1.0
    val distance = levenshtein(a, b)
    return 1.0 - (distance.toDouble() / maxLen.toDouble())
}

fun computeMatch(
    userInput: String,
    expectedAnswers: List<String>,
    prefs: StudentSrsPrefs,
): MatchResult {
    require(expectedAnswers.isNotEmpty()) { "expectedAnswers must not be empty" }

    val userNorm = normalize(userInput)
    val expectedNorms = expectedAnswers.map(::normalize)

    var bestExpected = expectedNorms.first()
    var bestSimilarity = similarity(userNorm, bestExpected)

    for (i in 1 until expectedNorms.size) {
        val candidate = expectedNorms[i]
        val candidateSimilarity = similarity(userNorm, candidate)
        if (candidateSimilarity > bestSimilarity) {
            bestSimilarity = candidateSimilarity
            bestExpected = candidate
        }
    }

    val isExact = userNorm == bestExpected
    val threshold = thresholdByLength(
        expectedLength = bestExpected.length,
        baseThreshold = prefs.similarityThreshold,
    )

    val matchType = when {
        isExact -> MatchType.EXACT
        prefs.allowNearMatch && bestSimilarity >= threshold -> MatchType.NEAR
        else -> MatchType.WRONG
    }

    return MatchResult(
        userNorm = userNorm,
        bestExpectedNorm = bestExpected,
        similarity = bestSimilarity,
        isExact = isExact,
        matchType = matchType,
    )
}

fun thresholdByLength(
    expectedLength: Int,
    baseThreshold: Double,
): Double {
    return when {
        expectedLength <= 3 -> 1.0
        expectedLength <= 5 -> max(baseThreshold, 0.85)
        else -> max(baseThreshold - 0.05, 0.80)
    }
}
