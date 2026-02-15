package com.cerebus.flashcards.data

import com.cerebus.core.utils.CustomResult
import com.cerebus.flashcards.data.entity.FlashcardEntity
import com.cerebus.flashcards.data.storage.FlashcardStorage
import com.cerebus.flashcards.domain.models.BulkInsertResult
import com.cerebus.flashcards.domain.models.Flashcard
import com.cerebus.flashcards.domain.repositories.FlashcardRepository

class FlashcardRepositoryImpl(private val storage: FlashcardStorage) : FlashcardRepository {
    override suspend fun getFlashcard(id: String): Flashcard? {
        return storage.getById(id)?.toDomain()
    }

    override suspend fun getFlashcardsByDeckId(deckId: String): List<Flashcard> {
        return storage.getByDeckId(deckId).map { it.toDomain() }
    }

    override suspend fun getFlashcardsByIds(ids: List<String>): List<Flashcard> {
        return storage.getByIds(ids).map { it.toDomain() }
    }

    override suspend fun searchFlashcards(query: String): List<Flashcard> {
        return storage.search(query).map { it.toDomain() }
    }

    override suspend fun addFlashcard(flashcard: Flashcard): Boolean {
        return storage.insert(flashcard.toEntity())
    }

    override suspend fun deleteFlashcard(id: String): Boolean {
        return storage.delete(id)
    }

    override suspend fun updateFlashcard(id: String, newData: Flashcard): Boolean {
        return storage.update(id, newData.toEntity())
    }

    override suspend fun addFlashcardsBulk(cards: List<Flashcard>): CustomResult<BulkInsertResult> {
        return storage.insertBulk(cards.map { it.toEntity() })
    }

    private fun FlashcardEntity.toDomain() = Flashcard(
        id = id,
        imageUrl = imageUrl,
        name = name,
        deckId = deckId,
    )

    private fun Flashcard.toEntity() = FlashcardEntity(
        id = id,
        imageUrl = imageUrl,
        name = name,
        deckId = deckId,
    )
}
