package com.cerebus.decks.domain.models

data class BulkDeleteResult(
    val deletedCount: Int,
    val failedCount: Int,
    val failedIds: List<String>,
    val notFound: List<String>,
)
