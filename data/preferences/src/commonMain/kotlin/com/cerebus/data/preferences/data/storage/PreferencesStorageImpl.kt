package com.cerebus.data.preferences.data.storage

import com.russhwolf.settings.Settings

private const val LAST_ACTIVE_STUDENT_KEY = "last_active_student"

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
}
