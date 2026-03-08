package com.cerebus.data.preferences.data.storage

import com.russhwolf.settings.Settings

private const val LAST_ACTIVE_STUDENT_KEY = "last_active_student"
private const val LAST_SESSION_PREFIX = "last_session_v1_"
private const val SESSION_IDS_SEPARATOR = ","
private const val ANONYMOUS_STUDENT_ID = "_anonymous_"
private const val NO_DECKS_MARKER = "_no_decks_"

class PreferencesStorageImpl(
    private val settings: Settings = Settings(),
) : PreferencesStorage {
    override fun getLastActiveStudentId(): String? {
        return settings.getStringOrNull(LAST_ACTIVE_STUDENT_KEY)
    }

    override fun setLastActiveStudentId(studentId: String) {
        settings.putString(LAST_ACTIVE_STUDENT_KEY, studentId)
    }

    override fun clearLastActiveStudentId() {
        settings.remove(LAST_ACTIVE_STUDENT_KEY)
    }

    override fun getLastSessionCardIds(
        studentId: String,
        deckIds: List<String>,
    ): List<String>? {
        val key = buildLastSessionKey(
            studentId = studentId,
            deckIds = deckIds,
        )
        return settings.getStringOrNull(key)
            ?.split(SESSION_IDS_SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
    }

    override fun setLastSessionCardIds(
        studentId: String,
        deckIds: List<String>,
        cardIds: List<String>,
    ) {
        val key = buildLastSessionKey(
            studentId = studentId,
            deckIds = deckIds,
        )
        val normalizedCardIds = cardIds.filter { it.isNotBlank() }
        if (normalizedCardIds.isEmpty()) {
            settings.remove(key)
            return
        }
        settings.putString(
            key = key,
            value = normalizedCardIds.joinToString(SESSION_IDS_SEPARATOR),
        )
    }

    override fun clearLastSessionCardIds(
        studentId: String,
        deckIds: List<String>,
    ) {
        settings.remove(
            buildLastSessionKey(
                studentId = studentId,
                deckIds = deckIds,
            )
        )
    }

    private fun buildLastSessionKey(
        studentId: String,
        deckIds: List<String>,
    ): String {
        val normalizedStudentId = studentId.takeIf { it.isNotBlank() } ?: ANONYMOUS_STUDENT_ID
        val normalizedDeckIds = deckIds
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val decksPart = if (normalizedDeckIds.isEmpty()) {
            NO_DECKS_MARKER
        } else {
            normalizedDeckIds.joinToString(SESSION_IDS_SEPARATOR)
        }
        return "$LAST_SESSION_PREFIX$normalizedStudentId|$decksPart"
    }
}
