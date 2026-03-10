package com.cerebus.core.deck_package.domain.model

import kotlinx.serialization.Serializable

const val RW_DECK_FORMAT = "rwdeck"
const val RW_DECK_SCHEMA_VERSION = 1

@Serializable
data class DeckPackageManifest(
    val format: String = RW_DECK_FORMAT,
    val schemaVersion: Int = RW_DECK_SCHEMA_VERSION,
    val exportedAtEpochMillis: Long,
    val deck: DeckPackageDeck,
    val cards: List<DeckPackageCard>,
    val media: List<DeckPackageMediaAsset> = emptyList(),
)

@Serializable
data class DeckPackageDeck(
    val sourceDeckId: String? = null,
    val name: String,
    val coverMedia: DeckPackageMediaRef? = null,
)

@Serializable
data class DeckPackageCard(
    val sourceCardId: String? = null,
    val text: String,
    val media: DeckPackageMediaRef? = null,
)

@Serializable
data class DeckPackageMediaRef(
    val kind: DeckPackageMediaKind,
    val file: String,
)

@Serializable
enum class DeckPackageMediaKind {
    IMAGE,
    VIDEO,
}

@Serializable
data class DeckPackageMediaAsset(
    val file: String,
    val sha256: String? = null,
    val sizeBytes: Long? = null,
)
