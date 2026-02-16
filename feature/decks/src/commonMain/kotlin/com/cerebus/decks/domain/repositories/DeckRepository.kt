package com.cerebus.decks.domain.repositories

import com.cerebus.core.utils.CustomResult
import com.cerebus.decks.domain.models.BulkDeleteResult
import com.cerebus.decks.domain.models.BulkInsertResult
import com.cerebus.decks.domain.models.Deck

interface DeckRepository {
    suspend fun addDeck(deck: Deck): Boolean
    suspend fun addDecks(decks: List<Deck>): CustomResult<BulkInsertResult>

    suspend fun deleteDeck(id: String): Boolean
    suspend fun deleteDecks(ids: List<String>): CustomResult<BulkDeleteResult>

    suspend fun downloadDeck(id: String): CustomResult<Deck>
}
