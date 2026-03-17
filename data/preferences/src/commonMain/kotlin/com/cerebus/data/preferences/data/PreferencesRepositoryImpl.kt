package com.cerebus.data.preferences.data

import com.cerebus.data.preferences.domain.models.KeyboardPressDelay
import com.cerebus.data.preferences.data.storage.PreferencesStorage
import com.cerebus.data.preferences.domain.models.NeighborTypoSensitivity
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

    override fun getPreventWrongKeyPressEnabled(studentId: String): Boolean? {
        return storage.getPreventWrongKeyPressEnabled(studentId)
    }

    override fun setPreventWrongKeyPressEnabled(studentId: String, isEnabled: Boolean) {
        storage.setPreventWrongKeyPressEnabled(studentId, isEnabled)
    }

    override fun getAllowNeighborTyposEnabled(studentId: String): Boolean? {
        return storage.getAllowNeighborTyposEnabled(studentId)
    }

    override fun setAllowNeighborTyposEnabled(studentId: String, isEnabled: Boolean) {
        storage.setAllowNeighborTyposEnabled(studentId, isEnabled)
    }

    override fun getNeighborTypoSensitivity(studentId: String): NeighborTypoSensitivity? {
        return storage.getNeighborTypoSensitivity(studentId)
    }

    override fun setNeighborTypoSensitivity(
        studentId: String,
        sensitivity: NeighborTypoSensitivity,
    ) {
        storage.setNeighborTypoSensitivity(
            studentId = studentId,
            sensitivity = sensitivity,
        )
    }

    override fun getFreeNeighborSlipPresses(studentId: String): Int? {
        return storage.getFreeNeighborSlipPresses(studentId)
    }

    override fun setFreeNeighborSlipPresses(studentId: String, count: Int) {
        storage.setFreeNeighborSlipPresses(
            studentId = studentId,
            count = count,
        )
    }

    override fun getKeyboardPressDelay(studentId: String): KeyboardPressDelay? {
        return storage.getKeyboardPressDelay(studentId)
    }

    override fun setKeyboardPressDelay(studentId: String, delay: KeyboardPressDelay) {
        storage.setKeyboardPressDelay(
            studentId = studentId,
            delay = delay,
        )
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
