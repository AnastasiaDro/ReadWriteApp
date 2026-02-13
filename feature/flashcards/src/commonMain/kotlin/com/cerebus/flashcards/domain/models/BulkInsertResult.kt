package com.cerebus.flashcards.domain.models

data class BulkInsertResult(
    val insertedCount: Int,
    val failedCount: Int,
    val failedIds: List<String>,
    val alreadyExists: List<String>
)
