package com.cerebus.core.deck_package.domain.model

import kotlinx.serialization.Serializable

const val RW_STUDENT_FORMAT = "rwstudent"
const val RW_STUDENT_SCHEMA_VERSION = 1

@Serializable
data class StudentPackageManifest(
    val format: String = RW_STUDENT_FORMAT,
    val schemaVersion: Int = RW_STUDENT_SCHEMA_VERSION,
    val exportedAtEpochMillis: Long,
    val student: StudentPackageStudent,
    val decks: List<StudentPackageDeck> = emptyList(),
    val media: List<DeckPackageMediaAsset> = emptyList(),
)

@Serializable
data class StudentPackageStudent(
    val sourceStudentId: String? = null,
    val name: String,
    val avatarMedia: DeckPackageMediaRef? = null,
    val activeLetters: String,
    val srsPrefs: StudentPackageSrsPrefs,
)

@Serializable
data class StudentPackageDeck(
    val sourceDeckId: String? = null,
    val deckName: String,
    val cards: List<StudentPackageCardProgress> = emptyList(),
    val reviewLogs: List<StudentPackageReviewLog> = emptyList(),
)

@Serializable
data class StudentPackageCardProgress(
    val sourceCardId: String? = null,
    val cardName: String,
    val level: Int,
    val dueAtEpochMillis: Long,
    val recallSuccessStreak: Int,
    val copySuccessStreak: Int,
    val lastReviewedAtEpochMillis: Long? = null,
    val lastHintLevel: Int? = null,
    val lastDurationMs: Long? = null,
    val lastWrongPressCount: Int = 0,
)

@Serializable
data class StudentPackageReviewLog(
    val sourceCardId: String? = null,
    val cardName: String,
    val shownAtEpochMillis: Long,
    val submittedAtEpochMillis: Long,
    val userInputRaw: String,
    val userInputNormalized: String,
    val expectedAnswerNormalized: String,
    val isCorrect: Boolean,
    val hintLevel: Int,
    val wrongPressCount: Int,
    val durationMs: Long,
    val copyStage: Boolean,
    val levelBefore: Int,
    val levelAfter: Int,
    val recallSuccessStreakBefore: Int,
    val recallSuccessStreakAfter: Int,
    val copySuccessStreakBefore: Int,
    val copySuccessStreakAfter: Int,
    val dueAtBeforeEpochMillis: Long,
    val dueAtAfterEpochMillis: Long,
)

@Serializable
data class StudentPackageSrsPrefs(
    val newCardsPerSession: Int = 5,
    val reviewsPerSession: Int = 10,
    val learnMoreStep: Int = 5,
    val maxNewCardsPerDay: Int = 5,
    val allowNearMatch: Boolean = true,
    val similarityThreshold: Double = 0.85,
    val easyStreakRequired: Int = 2,
    val guidedHintSuccessThreshold: Int = 1,
    val updatedAtEpochMillis: Long = 0L,
)
