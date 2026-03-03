package com.cerebus.data.student.data.storage

import com.cerebus.data.student.data.entity.StudentEntity

interface StudentStorage {
    suspend fun create(student: StudentEntity): Boolean
    suspend fun delete(id: String): Boolean
    suspend fun getById(id: String): StudentEntity?
    suspend fun getAllOrderedByCreation(): List<StudentEntity>
    suspend fun hasAnyStudents(): Boolean
    suspend fun getFirstStudentId(): String?
    suspend fun getActiveLettersById(id: String): String?
    suspend fun updateName(id: String, newName: String): Boolean
    suspend fun updateActiveLetters(id: String, activeLetters: String): Boolean
}
