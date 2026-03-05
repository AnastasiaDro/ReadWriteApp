package com.cerebus.core.game_engine.domain.repository

import com.cerebus.core.game_engine.domain.model.StudentSrsPrefs

interface StudentPrefsRepository {
    suspend fun getPrefs(studentId: String): StudentSrsPrefs

    suspend fun savePrefs(prefs: StudentSrsPrefs)
}
