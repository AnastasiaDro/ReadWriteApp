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

enum class DeckImportMode {
    REPLACE_EXISTING,
    ADD_MISSING_CARDS,
}

data class DeckPackageImportPreview(
    val deckName: String,
    val existingDeckId: String? = null,
    val existingDeckName: String? = null,
    val matchingCardsCount: Int = 0,
    val newCardsCount: Int = 0,
    val staleCardsCount: Int = 0,
)

interface DeckPackageService {
    suspend fun exportDeck(deckId: String): CustomResult<DeckPackageExportFile>

    suspend fun inspectDeckImport(
        archiveUri: String,
    ): CustomResult<DeckPackageImportPreview>

    suspend fun importDeck(
        archiveUri: String,
        assignToStudentId: String?,
        mode: DeckImportMode = DeckImportMode.REPLACE_EXISTING,
    ): CustomResult<DeckPackageImportResult>
}
