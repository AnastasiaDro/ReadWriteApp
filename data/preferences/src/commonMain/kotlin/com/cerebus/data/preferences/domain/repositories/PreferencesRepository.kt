package com.cerebus.data.preferences.domain.repositories

import com.cerebus.data.preferences.domain.models.KeyboardPressDelay
import com.cerebus.data.preferences.domain.models.NeighborTypoSensitivity

interface PreferencesRepository {
    fun getLastActiveStudentId(): String?
    fun setLastActiveStudentId(studentId: String)
    fun clearLastActiveStudentId()
    fun getKeyboardLanguage(studentId: String): String?
    fun setKeyboardLanguage(studentId: String, languageCode: String)
    fun getKeyboardShiftEnabled(studentId: String): Boolean?
    fun setKeyboardShiftEnabled(studentId: String, isEnabled: Boolean)
    fun getPreventWrongKeyPressEnabled(studentId: String): Boolean?
    fun setPreventWrongKeyPressEnabled(studentId: String, isEnabled: Boolean)
    fun getAllowNeighborTyposEnabled(studentId: String): Boolean?
    fun setAllowNeighborTyposEnabled(studentId: String, isEnabled: Boolean)
    fun getNeighborTypoSensitivity(studentId: String): NeighborTypoSensitivity?
    fun setNeighborTypoSensitivity(studentId: String, sensitivity: NeighborTypoSensitivity)
    fun getFreeNeighborSlipPresses(studentId: String): Int?
    fun setFreeNeighborSlipPresses(studentId: String, count: Int)
    fun getKeyboardPressDelay(studentId: String): KeyboardPressDelay?
    fun setKeyboardPressDelay(studentId: String, delay: KeyboardPressDelay)
    fun getHideDigitsOnTightScreenEnabled(studentId: String): Boolean?
    fun setHideDigitsOnTightScreenEnabled(studentId: String, isEnabled: Boolean)
    fun getGalleryInputHintEnabled(studentId: String): Boolean?
    fun setGalleryInputHintEnabled(studentId: String, isEnabled: Boolean)
    fun getGallerySimplifiedKeyboardEnabled(studentId: String): Boolean?
    fun setGallerySimplifiedKeyboardEnabled(studentId: String, isEnabled: Boolean)
    fun getFairyTalesSimplifiedKeyboardEnabled(studentId: String): Boolean?
    fun setFairyTalesSimplifiedKeyboardEnabled(studentId: String, isEnabled: Boolean)
    fun getLastSessionCardIds(
        studentId: String,
        deckIds: List<String>,
    ): List<String>?
    fun setLastSessionCardIds(
        studentId: String,
        deckIds: List<String>,
        cardIds: List<String>,
    )
    fun clearLastSessionCardIds(
        studentId: String,
        deckIds: List<String>,
    )
}
