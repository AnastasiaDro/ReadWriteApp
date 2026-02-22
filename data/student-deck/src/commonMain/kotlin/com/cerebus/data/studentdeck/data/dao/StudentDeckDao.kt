package com.cerebus.data.studentdeck.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cerebus.data.studentdeck.data.entity.StudentDeckCrossRef
import com.cerebus.data.studentdeck.data.entity.StudentWithDecks

@Dao
interface StudentDeckDao {
    @Transaction
    @Query("SELECT * FROM students WHERE id = :studentId LIMIT 1")
    suspend fun getStudentWithDecks(studentId: String): StudentWithDecks?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: StudentDeckCrossRef): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(crossRefs: List<StudentDeckCrossRef>): LongArray

    @Query("DELETE FROM StudentDeckCrossRef WHERE studentId = :studentId AND deckId = :deckId")
    suspend fun deleteCrossRef(studentId: String, deckId: String): Int

    @Query("DELETE FROM StudentDeckCrossRef WHERE studentId = :studentId AND deckId IN (:deckIds)")
    suspend fun deleteCrossRefs(studentId: String, deckIds: List<String>): Int

    @Query("SELECT deckId FROM StudentDeckCrossRef WHERE studentId = :studentId AND deckId IN (:deckIds)")
    suspend fun getLinkedDeckIds(studentId: String, deckIds: List<String>): List<String>
}
