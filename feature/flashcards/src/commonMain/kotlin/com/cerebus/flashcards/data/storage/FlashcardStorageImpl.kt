package com.cerebus.flashcards.data.storage

import com.cerebus.core.utils.CustomResult
import com.cerebus.flashcards.domain.models.BulkInsertResult
import com.cerebus.flashcards.domain.models.Flashcard

class FlashcardStorageImpl : FlashcardStorage {
    override suspend fun getById(id: String): Flashcard? {
        TODO("Not yet implemented")
    }

    override suspend fun getByIds(ids: List<String>): List<Flashcard> {
        TODO("Not yet implemented")
    }

    override suspend fun getByDeckId(deckId: String): List<Flashcard> {
        TODO("Not yet implemented")
    }

    override suspend fun search(query: String): List<Flashcard> {
        TODO("Not yet implemented")
    }

    override suspend fun insert(flashcard: Flashcard): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun update(
        id: String,
        flashcard: Flashcard
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun delete(id: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun insertBulk(cards: List<Flashcard>): CustomResult<BulkInsertResult> {
        TODO("Not yet implemented")
    }

    override suspend fun exists(id: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun count(): Int {
        TODO("Not yet implemented")
    }

    override suspend fun clear(): Boolean {
        TODO("Not yet implemented")
    }
}