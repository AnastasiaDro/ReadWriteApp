package com.cerebus.flashcards.domain.repositories

import com.cerebus.core.utils.CustomResult
import com.cerebus.flashcards.domain.models.BulkInsertResult
import com.cerebus.flashcards.domain.models.Flashcard

interface FlashcardRepository {

    suspend fun getFlashcard(id: String): Flashcard?

    suspend fun getFlashcardsByDeckId(id: String): List<Flashcard>
    suspend fun getFlashcardsByIds(ids: List<String>): List<Flashcard>

    suspend fun searchFlashcards(query: String): List<Flashcard>

    suspend fun addFlashcard(flashcard: Flashcard): Boolean

    suspend fun deleteFlashcard(id: String): Boolean

    suspend fun updateFlashcard(id: String, newData: Flashcard): Boolean

    suspend fun addFlashcardsBulk(cards: List<Flashcard>): CustomResult<BulkInsertResult>

}