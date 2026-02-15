package com.cerebus.flashcards.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cerebus.flashcards.data.entity.FlashcardEntity

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards")
    suspend fun getAll(): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FlashcardEntity?

    @Query("SELECT * FROM flashcards WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId")
    suspend fun getByDeckId(deckId: String): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE name LIKE '%' || :query || '%'")
    suspend fun searchByName(query: String): List<FlashcardEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(card: FlashcardEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(cards: List<FlashcardEntity>): List<Long>

    @Update
    suspend fun update(card: FlashcardEntity): Int

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM flashcards WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    @Query("SELECT COUNT(*) FROM flashcards")
    suspend fun count(): Int

    @Query("DELETE FROM flashcards")
    suspend fun clear(): Int
}
