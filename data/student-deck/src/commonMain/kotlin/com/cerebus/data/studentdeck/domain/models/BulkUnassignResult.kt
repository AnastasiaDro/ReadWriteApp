package com.cerebus.data.studentdeck.domain.models

data class BulkUnassignResult(
    val deletedCount: Int,
    val failedCount: Int,
    val failedDeckIds: List<String>,
    val notLinkedDeckIds: List<String>,
)
