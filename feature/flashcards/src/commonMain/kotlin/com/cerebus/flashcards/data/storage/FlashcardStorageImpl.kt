package com.cerebus.flashcards.data.storage

import com.cerebus.core.utils.CustomResult
import com.cerebus.flashcards.data.dao.FlashcardDao
import com.cerebus.flashcards.data.entity.FlashcardEntity
import com.cerebus.flashcards.domain.models.BulkInsertResult

class FlashcardStorageImpl(
    private val dao: FlashcardDao
) : FlashcardStorage {
    override suspend fun getById(id: String): FlashcardEntity? {
        return dao.getById(id)
    }

    override suspend fun getByIds(ids: List<String>): List<FlashcardEntity> {
        if (ids.isEmpty()) return emptyList()
        return dao.getByIds(ids)
    }

    override suspend fun getByDeckId(deckId: String): List<FlashcardEntity> {
        return dao.getByDeckId(deckId)
    }

    override suspend fun search(query: String): List<FlashcardEntity> {
        if (query.isBlank()) return dao.getAll()
        return dao.searchByName(query)
    }

    override suspend fun insert(flashcard: FlashcardEntity): Boolean {
        return runCatching {
            dao.insert(flashcard)
            true
        }.getOrDefault(false)
    }

    override suspend fun update(
        id: String,
        flashcard: FlashcardEntity,
    ): Boolean {
        return runCatching {
            dao.update(flashcard.copy(id = id)) > 0
        }.getOrDefault(false)
    }

    override suspend fun delete(id: String): Boolean {
        return runCatching {
            dao.deleteById(id) > 0
        }.getOrDefault(false)
    }

    override suspend fun insertBulk(cards: List<FlashcardEntity>): CustomResult<BulkInsertResult> {
        if (cards.isEmpty()) {
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
            val results = dao.insertIgnore(cards)
            val alreadyExists = buildList {
                results.forEachIndexed { index, value ->
                    if (value == -1L) add(cards[index].id)
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

    override suspend fun exists(id: String): Boolean {
        return dao.exists(id)
    }

    override suspend fun count(): Int {
        return dao.count()
    }

    override suspend fun clear(): Boolean {
        return runCatching {
            dao.clear()
            true
        }.getOrDefault(false)
    }
}
