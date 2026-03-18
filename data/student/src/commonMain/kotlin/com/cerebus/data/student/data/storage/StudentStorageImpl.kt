package com.cerebus.data.student.data.storage

import com.cerebus.data.student.data.dao.StudentDao
import com.cerebus.data.student.data.entity.StudentEntity

class StudentStorageImpl(
    private val dao: StudentDao,
) : StudentStorage {
    override suspend fun create(student: StudentEntity): Boolean {
        return runCatching {
            dao.insert(student)
            true
        }.getOrDefault(false)
    }

    override suspend fun delete(id: String): Boolean {
        return runCatching { dao.deleteById(id) > 0 }.getOrDefault(false)
    }

    override suspend fun getById(id: String): StudentEntity? {
        return runCatching { dao.getById(id) }.getOrNull()
    }

    override suspend fun getAllOrderedByCreation(): List<StudentEntity> {
        return runCatching { dao.getAllOrderedByCreation() }.getOrDefault(emptyList())
    }

    override suspend fun hasAnyStudents(): Boolean {
        return runCatching { dao.hasAnyStudents() }.getOrDefault(false)
    }

    override suspend fun getFirstStudentId(): String? {
        return runCatching { dao.getFirstStudentId() }.getOrNull()
    }

    override suspend fun getActiveLettersById(id: String): String? {
        return runCatching { dao.getActiveLettersById(id) }.getOrNull()
    }

    override suspend fun updateName(id: String, newName: String): Boolean {
        return runCatching { dao.updateName(id, newName) > 0 }.getOrDefault(false)
    }

    override suspend fun updateAvatarUri(id: String, avatarUri: String?): Boolean {
        return runCatching { dao.updateAvatarUri(id, avatarUri) > 0 }.getOrDefault(false)
    }

    override suspend fun updateActiveLetters(id: String, activeLetters: String): Boolean {
        return runCatching { dao.updateActiveLetters(id, activeLetters) > 0 }.getOrDefault(false)
    }
}
