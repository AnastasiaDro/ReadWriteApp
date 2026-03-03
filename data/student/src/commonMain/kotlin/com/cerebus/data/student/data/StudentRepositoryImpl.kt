package com.cerebus.data.student.data

import com.cerebus.data.student.data.mapper.toDomain
import com.cerebus.data.student.data.mapper.toEntity
import com.cerebus.data.student.data.storage.StudentStorage
import com.cerebus.data.student.domain.models.Student
import com.cerebus.data.student.domain.repositories.StudentRepository

class StudentRepositoryImpl(
    private val storage: StudentStorage,
) : StudentRepository {
    override suspend fun createStudent(student: Student): Boolean {
        return storage.create(student.toEntity())
    }

    override suspend fun deleteStudent(id: String): Boolean {
        return storage.delete(id)
    }

    override suspend fun getStudentById(id: String): Student? {
        return storage.getById(id)?.toDomain()
    }

    override suspend fun getAllStudentsOrderedByCreation(): List<Student> {
        return storage.getAllOrderedByCreation().map { it.toDomain() }
    }

    override suspend fun hasAnyStudents(): Boolean {
        return storage.hasAnyStudents()
    }

    override suspend fun getFirstStudentId(): String? {
        return storage.getFirstStudentId()
    }

    override suspend fun getActiveLettersById(id: String): String? {
        return storage.getActiveLettersById(id)
    }

    override suspend fun addLetter(id: String, letter: Char): Boolean {
        if (!letter.isLetter()) return false
        val current = storage.getById(id) ?: return false
        val normalized = normalizeLetters(current.activeLetters)
        val toAdd = letter.lowercaseChar()
        val updatedLetters = if (toAdd in normalized) normalized else normalized + toAdd
        return storage.updateActiveLetters(id, updatedLetters)
    }

    override suspend fun removeLetter(id: String, letter: Char): Boolean {
        if (!letter.isLetter()) return false
        val current = storage.getById(id) ?: return false
        val normalized = normalizeLetters(current.activeLetters)
        val toRemove = letter.lowercaseChar()
        val updatedLetters = normalized.filterNot { it == toRemove }
        return storage.updateActiveLetters(id, updatedLetters)
    }

    override suspend fun updateName(id: String, newName: String): Boolean {
        val normalizedName = newName.trim()
        if (normalizedName.isBlank()) return false
        return storage.updateName(id, normalizedName)
    }

    private fun normalizeLetters(value: String): String {
        return value
            .lowercase()
            .filter { it.isLetter() }
            .asSequence()
            .distinct()
            .joinToString(separator = "")
    }
}
