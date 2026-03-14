package com.cerebus.data.preferences.data

import com.cerebus.data.preferences.data.storage.PreferencesStorage
import com.cerebus.data.preferences.domain.repositories.PreferencesRepository

class PreferencesRepositoryImpl(
    private val storage: PreferencesStorage,
) : PreferencesRepository {
    override fun getLastActiveStudentId(): String? {
        return storage.getLastActiveStudentId()
    }

    override fun setLastActiveStudentId(studentId: String) {
        storage.setLastActiveStudentId(studentId)
    }

    override fun clearLastActiveStudentId() {
        storage.clearLastActiveStudentId()
    }

    override fun getKeyboardLanguage(studentId: String): String? {
        return storage.getKeyboardLanguage(studentId)
    }

    override fun setKeyboardLanguage(studentId: String, languageCode: String) {
        storage.setKeyboardLanguage(studentId, languageCode)
    }

    override fun getKeyboardShiftEnabled(studentId: String): Boolean? {
        return storage.getKeyboardShiftEnabled(studentId)
    }

    override fun setKeyboardShiftEnabled(studentId: String, isEnabled: Boolean) {
        storage.setKeyboardShiftEnabled(studentId, isEnabled)
    }

    override fun getLastSessionCardIds(
        studentId: String,
        deckIds: List<String>,
    ): List<String>? {
        return storage.getLastSessionCardIds(
            studentId = studentId,
            deckIds = deckIds,
        )
    }

    override fun setLastSessionCardIds(
        studentId: String,
        deckIds: List<String>,
        cardIds: List<String>,
    ) {
        storage.setLastSessionCardIds(
            studentId = studentId,
            deckIds = deckIds,
            cardIds = cardIds,
        )
    }

    override fun clearLastSessionCardIds(
        studentId: String,
        deckIds: List<String>,
    ) {
        storage.clearLastSessionCardIds(
            studentId = studentId,
            deckIds = deckIds,
        )
    }
}
