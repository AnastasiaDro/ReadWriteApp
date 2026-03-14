package com.cerebus.data.preferences.data.storage

interface PreferencesStorage {
    fun getLastActiveStudentId(): String?
    fun setLastActiveStudentId(studentId: String)
    fun clearLastActiveStudentId()
    fun getKeyboardLanguage(studentId: String): String?
    fun setKeyboardLanguage(studentId: String, languageCode: String)
    fun getKeyboardShiftEnabled(studentId: String): Boolean?
    fun setKeyboardShiftEnabled(studentId: String, isEnabled: Boolean)
    fun getPreventWrongKeyPressEnabled(studentId: String): Boolean?
    fun setPreventWrongKeyPressEnabled(studentId: String, isEnabled: Boolean)
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
