package com.cerebus.decks.data.storage

import com.cerebus.core.utils.CustomResult
import com.cerebus.decks.data.entity.DeckEntity
import com.cerebus.decks.domain.models.BulkDeleteResult
import com.cerebus.decks.domain.models.BulkInsertResult

interface DeckStorage {
    suspend fun getAll(): List<DeckEntity>
    suspend fun getById(id: String): DeckEntity?

    suspend fun add(deck: DeckEntity): Boolean
    suspend fun addBulk(decks: List<DeckEntity>): CustomResult<BulkInsertResult>

    suspend fun delete(id: String): Boolean
    suspend fun deleteBulk(ids: List<String>): CustomResult<BulkDeleteResult>
    suspend fun updateName(id: String, name: String): Boolean
    suspend fun updateCoverUri(id: String, coverUri: String?): Boolean

    suspend fun downloadStub(id: String): CustomResult<DeckEntity>
}
