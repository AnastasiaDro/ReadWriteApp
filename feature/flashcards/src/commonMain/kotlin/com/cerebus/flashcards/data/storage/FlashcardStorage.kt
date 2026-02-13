package com.cerebus.flashcards.data.storage

import com.cerebus.core.utils.CustomResult
import com.cerebus.flashcards.domain.models.BulkInsertResult
import com.cerebus.flashcards.domain.models.Flashcard

interface FlashcardStorage {
    // Получение одной карточки по ID
    suspend fun getById(id: String): Flashcard?

    // Получение списка карточек по списку ID (для колод, сессий и т.п.)
    suspend fun getByIds(ids: List<String>): List<Flashcard>

    // Получение всех карточек из конкретной колоды
    suspend fun getByDeckId(deckId: String): List<Flashcard>

    // Поиск карточек по подстроке в подписи (или тегам, если есть)
    suspend fun search(query: String): List<Flashcard>

    // Сохранение новой карточки
    suspend fun insert(flashcard: Flashcard): Boolean

    // Обновление существующей карточки
    suspend fun update(id: String, flashcard: Flashcard): Boolean

    // Удаление карточки по ID
    suspend fun delete(id: String): Boolean


    suspend fun insertBulk(cards: List<Flashcard>): CustomResult<BulkInsertResult>

    // Проверка существования карточки
    suspend fun exists(id: String): Boolean

    // Получение количества карточек (например, для статистики)
    suspend fun count(): Int

    // Очистка хранилища (для тестов или сброса)
    suspend fun clear(): Boolean
}
