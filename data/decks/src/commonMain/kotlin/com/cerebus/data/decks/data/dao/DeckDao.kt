package com.cerebus.data.decks.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cerebus.data.decks.data.entity.DeckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Query("SELECT * FROM decks ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<DeckEntity>

    @Query("SELECT * FROM decks ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DeckEntity?

    @Query("SELECT * FROM decks WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<DeckEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(deck: DeckEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(decks: List<DeckEntity>): List<Long>

    @Query("DELETE FROM decks WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM decks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>): Int

    @Query("UPDATE decks SET name = :name WHERE id = :id")
    suspend fun updateName(id: String, name: String): Int

    @Query("UPDATE decks SET coverUri = :coverUri WHERE id = :id")
    suspend fun updateCoverUri(id: String, coverUri: String?): Int

    @Query("SELECT id FROM decks WHERE id IN (:ids)")
    suspend fun getExistingIds(ids: List<String>): List<String>
}
