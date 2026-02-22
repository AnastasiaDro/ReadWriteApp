package com.cerebus.data.studentdeck.domain.models

data class BulkAssignResult(
    val insertedCount: Int,
    val failedCount: Int,
    val failedDeckIds: List<String>,
    val alreadyLinkedDeckIds: List<String>,
)
