package com.cerebus.data.flashcards.data.storage

import com.cerebus.core.utils.CustomResult
import com.cerebus.data.flashcards.data.entity.FlashcardEntity
import com.cerebus.data.flashcards.domain.models.BulkInsertResult

interface FlashcardStorage {
    // Получение одной карточки по ID
    suspend fun getById(id: String): FlashcardEntity?

    // Получение списка карточек по списку ID (для колод, сессий и т.п.)
    suspend fun getByIds(ids: List<String>): List<FlashcardEntity>

    // Получение всех карточек из конкретной колоды
    suspend fun getByDeckId(deckId: String): List<FlashcardEntity>

    // Поиск карточек по подстроке в подписи (или тегам, если есть)
    suspend fun search(query: String): List<FlashcardEntity>

    // Сохранение новой карточки
    suspend fun insert(flashcard: FlashcardEntity): Boolean

    // Обновление существующей карточки
    suspend fun update(id: String, flashcard: FlashcardEntity): Boolean

    // Удаление карточки по ID
    suspend fun delete(id: String): Boolean


    suspend fun insertBulk(cards: List<FlashcardEntity>): CustomResult<BulkInsertResult>

    // Проверка существования карточки
    suspend fun exists(id: String): Boolean

    // Получение количества карточек (например, для статистики)
    suspend fun count(): Int

    // Очистка хранилища (для тестов или сброса)
    suspend fun clear(): Boolean
}
