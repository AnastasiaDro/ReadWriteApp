package com.cerebus.data.decks.data

import com.cerebus.core.utils.CustomResult
import com.cerebus.data.decks.data.entity.DeckEntity
import com.cerebus.data.decks.data.storage.DeckStorage
import com.cerebus.data.decks.domain.models.BulkDeleteResult
import com.cerebus.data.decks.domain.models.BulkInsertResult
import com.cerebus.data.decks.domain.models.Deck
import com.cerebus.data.decks.domain.repositories.DeckRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DeckRepositoryImpl(
    private val storage: DeckStorage,
) : DeckRepository {
    override suspend fun getAllDecks(): List<Deck> {
        return storage.getAll().map { it.toDomain() }
    }

    override suspend fun getDeckById(id: String): Deck? {
        return storage.getById(id)?.toDomain()
    }

    override fun observeAllDecks(): Flow<List<Deck>> {
        return storage.observeAll().map { decks -> decks.map { it.toDomain() } }
    }

    override fun observeDeckById(id: String): Flow<Deck?> {
        return storage.observeById(id).map { it?.toDomain() }
    }

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

    override suspend fun updateDeckName(id: String, name: String): Boolean {
        return storage.updateName(id, name)
    }

    override suspend fun updateDeckCoverUri(id: String, coverUri: String?): Boolean {
        return storage.updateCoverUri(id, coverUri)
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
        coverUri = coverUri,
    )

    private fun DeckEntity.toDomain() = Deck(
        id = id,
        name = name,
        coverUri = coverUri,
    )
}
