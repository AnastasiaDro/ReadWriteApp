package com.cerebus.data.flashcards.data

import com.cerebus.core.utils.CustomResult
import com.cerebus.core.utils.persistLocalFileUri
import com.cerebus.core.utils.resolvePersistedLocalFileUri
import com.cerebus.data.flashcards.data.entity.FlashcardEntity
import com.cerebus.data.flashcards.data.storage.FlashcardStorage
import com.cerebus.data.flashcards.domain.models.BulkInsertResult
import com.cerebus.data.flashcards.domain.models.Flashcard
import com.cerebus.data.flashcards.domain.repositories.FlashcardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FlashcardRepositoryImpl(private val storage: FlashcardStorage) : FlashcardRepository {
    override suspend fun getFlashcard(id: String): Flashcard? {
        return storage.getById(id)?.toDomain()
    }

    override suspend fun getFlashcardsByDeckId(id: String): List<Flashcard> {
        return storage.getByDeckId(id).map { it.toDomain() }
    }

    override fun observeFlashcardsByDeckId(id: String): Flow<List<Flashcard>> {
        return storage.observeByDeckId(id).map { cards -> cards.map { it.toDomain() } }
    }

    override suspend fun getAllCardsOfDeck(deckId: String): List<Flashcard> {
        return getFlashcardsByDeckId(deckId)
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

    private fun buildActiveLetters(name: String): String {
        return name
            .lowercase()
            .filter { it.isLetter() }
            .asSequence()
            .distinct()
            .joinToString(separator = "")
    }

    private fun FlashcardEntity.toDomain() = Flashcard(
        id = id,
        imageUrl = resolvePersistedLocalFileUri(imageUrl).orEmpty(),
        name = name,
        activeLetters = activeLetters,
        deckId = deckId,
    )

    private fun Flashcard.toEntity() = FlashcardEntity(
        id = id,
        imageUrl = persistLocalFileUri(imageUrl).orEmpty(),
        name = name,
        activeLetters = buildActiveLetters(name),
        deckId = deckId,
    )
}
