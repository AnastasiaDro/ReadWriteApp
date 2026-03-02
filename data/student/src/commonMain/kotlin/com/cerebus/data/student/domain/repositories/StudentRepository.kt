package com.cerebus.data.student.domain.repositories

import com.cerebus.data.student.domain.models.Student

interface StudentRepository {
    suspend fun createStudent(student: Student): Boolean
    suspend fun deleteStudent(id: String): Boolean
    suspend fun getStudentById(id: String): Student?
    suspend fun hasAnyStudents(): Boolean
    suspend fun getFirstStudentId(): String?
    suspend fun getActiveLettersById(id: String): String?
    suspend fun addLetter(id: String, letter: Char): Boolean
    suspend fun removeLetter(id: String, letter: Char): Boolean
    suspend fun updateName(id: String, newName: String): Boolean
}
