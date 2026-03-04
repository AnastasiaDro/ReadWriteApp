package com.cerebus.data.decks.data.storage

import com.cerebus.core.utils.CustomResult
import com.cerebus.data.decks.data.entity.DeckEntity
import com.cerebus.data.decks.domain.models.BulkDeleteResult
import com.cerebus.data.decks.domain.models.BulkInsertResult
import kotlinx.coroutines.flow.Flow

interface DeckStorage {
    suspend fun getAll(): List<DeckEntity>
    suspend fun getById(id: String): DeckEntity?
    fun observeAll(): Flow<List<DeckEntity>>
    fun observeById(id: String): Flow<DeckEntity?>

    suspend fun add(deck: DeckEntity): Boolean
    suspend fun addBulk(decks: List<DeckEntity>): CustomResult<BulkInsertResult>

    suspend fun delete(id: String): Boolean
    suspend fun deleteBulk(ids: List<String>): CustomResult<BulkDeleteResult>
    suspend fun updateName(id: String, name: String): Boolean
    suspend fun updateCoverUri(id: String, coverUri: String?): Boolean

    suspend fun downloadStub(id: String): CustomResult<DeckEntity>
}
