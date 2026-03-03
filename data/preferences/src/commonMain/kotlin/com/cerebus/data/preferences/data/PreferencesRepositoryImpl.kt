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
}
