package com.cerebus.data.preferences.data.storage

interface PreferencesStorage {
    fun getLastActiveStudentId(): String?
    fun setLastActiveStudentId(studentId: String)
    fun clearLastActiveStudentId()
}
