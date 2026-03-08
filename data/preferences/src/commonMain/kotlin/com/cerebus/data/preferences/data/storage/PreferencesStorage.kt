package com.cerebus.data.preferences.data.storage

interface PreferencesStorage {
    fun getLastActiveStudentId(): String?
    fun setLastActiveStudentId(studentId: String)
    fun clearLastActiveStudentId()
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
