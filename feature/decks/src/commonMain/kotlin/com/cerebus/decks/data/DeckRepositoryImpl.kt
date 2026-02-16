package com.cerebus.decks.data

import com.cerebus.core.utils.CustomResult
import com.cerebus.decks.data.entity.DeckEntity
import com.cerebus.decks.data.storage.DeckStorage
import com.cerebus.decks.domain.models.BulkDeleteResult
import com.cerebus.decks.domain.models.BulkInsertResult
import com.cerebus.decks.domain.models.Deck
import com.cerebus.decks.domain.repositories.DeckRepository

class DeckRepositoryImpl(
    private val storage: DeckStorage,
) : DeckRepository {
    override suspend fun addDeck(deck: Deck): Boolean {
        return storage.add(deck.toEntity())
    }

    override suspend fun addDecks(decks: List<Deck>): CustomResult<BulkInsertResult> {
        return storage.addBulk(decks.map { it.toEntity() })
    }

    override suspend fun deleteDeck(id: String): Boolean {
        return storage.delete(id)
    }

    override suspend fun deleteDecks(ids: List<String>): CustomResult<BulkDeleteResult> {
        return storage.deleteBulk(ids)
    }

    override suspend fun downloadDeck(id: String): CustomResult<Deck> {
        return when (val result = storage.downloadStub(id)) {
            is CustomResult.Success -> CustomResult.Success(result.data.toDomain())
            is CustomResult.Failure -> result
        }
    }

    private fun Deck.toEntity() = DeckEntity(
        id = id,
        name = name,
    )

    private fun DeckEntity.toDomain() = Deck(
        id = id,
        name = name,
    )
}
