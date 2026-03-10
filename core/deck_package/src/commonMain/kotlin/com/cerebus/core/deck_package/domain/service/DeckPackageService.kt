package com.cerebus.core.deck_package.domain.service

import com.cerebus.core.utils.CustomResult

data class DeckPackageExportFile(
    val path: String,
    val fileName: String,
)

data class DeckPackageImportResult(
    val deckId: String,
    val deckName: String,
    val importedCardsCount: Int,
)

interface DeckPackageService {
    suspend fun exportDeck(deckId: String): CustomResult<DeckPackageExportFile>

    suspend fun importDeck(
        archiveUri: String,
        assignToStudentId: String?,
    ): CustomResult<DeckPackageImportResult>
}
