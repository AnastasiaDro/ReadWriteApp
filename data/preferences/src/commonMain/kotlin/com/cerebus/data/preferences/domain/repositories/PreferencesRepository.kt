package com.cerebus.data.preferences.domain.repositories

interface PreferencesRepository {
    fun getLastActiveStudentId(): String?
    fun setLastActiveStudentId(studentId: String)
    fun clearLastActiveStudentId()
}
