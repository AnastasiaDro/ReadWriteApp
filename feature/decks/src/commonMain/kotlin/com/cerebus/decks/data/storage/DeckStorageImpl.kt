package com.cerebus.decks.data.storage

import com.cerebus.core.utils.CustomResult
import com.cerebus.decks.data.dao.DeckDao
import com.cerebus.decks.data.entity.DeckEntity
import com.cerebus.decks.domain.models.BulkDeleteResult
import com.cerebus.decks.domain.models.BulkInsertResult

class DeckStorageImpl(
    private val dao: DeckDao,
) : DeckStorage {
    override suspend fun getAll(): List<DeckEntity> {
        return runCatching { dao.getAll() }.getOrDefault(emptyList())
    }

    override suspend fun getById(id: String): DeckEntity? {
        return runCatching { dao.getById(id) }.getOrNull()
    }

    override suspend fun add(deck: DeckEntity): Boolean {
        return runCatching {
            dao.insert(deck)
            true
        }.getOrDefault(false)
    }

    override suspend fun addBulk(decks: List<DeckEntity>): CustomResult<BulkInsertResult> {
        if (decks.isEmpty()) {
            return CustomResult.Success(
                BulkInsertResult(
                    insertedCount = 0,
                    failedCount = 0,
                    failedIds = emptyList(),
                    alreadyExists = emptyList(),
                )
            )
        }

        return runCatching {
            val results = dao.insertIgnore(decks)
            val alreadyExists = buildList {
                results.forEachIndexed { index, value ->
                    if (value == -1L) add(decks[index].id)
                }
            }
            val insertedCount = results.count { it != -1L }

            CustomResult.Success(
                BulkInsertResult(
                    insertedCount = insertedCount,
                    failedCount = 0,
                    failedIds = emptyList(),
                    alreadyExists = alreadyExists,
                )
            )
        }.getOrElse { error ->
            CustomResult.Failure(error)
        }
    }

    override suspend fun delete(id: String): Boolean {
        return runCatching {
            dao.deleteById(id) > 0
        }.getOrDefault(false)
    }

    override suspend fun deleteBulk(ids: List<String>): CustomResult<BulkDeleteResult> {
        if (ids.isEmpty()) {
            return CustomResult.Success(
                BulkDeleteResult(
                    deletedCount = 0,
                    failedCount = 0,
                    failedIds = emptyList(),
                    notFound = emptyList(),
                )
            )
        }

        return runCatching {
            val existingIds = dao.getExistingIds(ids).toSet()
            val notFound = ids.filterNot { it in existingIds }

            dao.deleteByIds(ids)
            val failedIds = dao.getExistingIds(ids)
            val deletedCount = ids.size - failedIds.size - notFound.size

            CustomResult.Success(
                BulkDeleteResult(
                    deletedCount = deletedCount,
                    failedCount = failedIds.size,
                    failedIds = failedIds,
                    notFound = notFound,
                )
            )
        }.getOrElse { error ->
            CustomResult.Failure(error)
        }
    }

    override suspend fun updateName(id: String, name: String): Boolean {
        return runCatching { dao.updateName(id, name) > 0 }.getOrDefault(false)
    }

    override suspend fun updateCoverUri(id: String, coverUri: String?): Boolean {
        return runCatching { dao.updateCoverUri(id, coverUri) > 0 }.getOrDefault(false)
    }

    override suspend fun downloadStub(id: String): CustomResult<DeckEntity> {
        return CustomResult.Failure(
            UnsupportedOperationException("Deck download is not implemented yet")
        )
    }
}
