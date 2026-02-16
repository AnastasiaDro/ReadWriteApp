package com.cerebus.decks.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cerebus.decks.data.entity.DeckEntity

@Dao
interface DeckDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(deck: DeckEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(decks: List<DeckEntity>): List<Long>

    @Query("DELETE FROM decks WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM decks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>): Int

    @Query("SELECT id FROM decks WHERE id IN (:ids)")
    suspend fun getExistingIds(ids: List<String>): List<String>
}
