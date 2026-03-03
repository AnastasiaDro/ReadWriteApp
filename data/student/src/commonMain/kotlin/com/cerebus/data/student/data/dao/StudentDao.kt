package com.cerebus.data.student.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cerebus.data.student.data.entity.StudentEntity

@Dao
interface StudentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(student: StudentEntity): Long

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StudentEntity?

    @Query("SELECT * FROM students ORDER BY rowid DESC")
    suspend fun getAllOrderedByCreation(): List<StudentEntity>

    @Query("SELECT activeLetters FROM students WHERE id = :id LIMIT 1")
    suspend fun getActiveLettersById(id: String): String?

    @Query("UPDATE students SET name = :newName WHERE id = :id")
    suspend fun updateName(id: String, newName: String): Int

    @Query("UPDATE students SET activeLetters = :activeLetters WHERE id = :id")
    suspend fun updateActiveLetters(id: String, activeLetters: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM students LIMIT 1)")
    suspend fun hasAnyStudents(): Boolean

    @Query("SELECT id FROM students LIMIT 1")
    suspend fun getFirstStudentId(): String?
}
