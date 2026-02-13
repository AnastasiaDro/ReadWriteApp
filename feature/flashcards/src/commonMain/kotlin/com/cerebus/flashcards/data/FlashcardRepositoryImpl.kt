package com.cerebus.flashcards.data

import com.cerebus.flashcards.data.storage.FlashcardStorage
import com.cerebus.flashcards.domain.models.BulkInsertResult
import com.cerebus.flashcards.domain.models.Flashcard
import com.cerebus.flashcards.domain.repositories.FlashcardRepository

class FlashcardRepositoryImpl(private val storage: FlashcardStorage) : FlashcardRepository {
    override suspend fun getFlashcard(id: String): Flashcard? {
        return storage.getById(id)
    }

    override suspend fun getFlashcardsByDeckId(deckId: String): List<Flashcard> {
        return storage.getByDeckId(deckId)
    }

    override suspend fun getFlashcardsByIds(ids: List<String>): List<Flashcard> {
        return storage.getByIds(ids)
    }

    override suspend fun searchFlashcards(query: String): List<Flashcard> {
        return storage.search(query)
    }

    override suspend fun addFlashcard(flashcard: Flashcard): Boolean {
        return storage.insert(flashcard)
    }

    override suspend fun deleteFlashcard(id: String): Boolean {
        return storage.delete(id)
    }

    override suspend fun updateFlashcard(id: String, newData: Flashcard): Boolean {
        return storage.update(id, newData)
    }

    override suspend fun addFlashcardsBulk(cards: List<Flashcard>): Result<BulkInsertResult> {
        return storage.insertBulk(cards)
    }
}
