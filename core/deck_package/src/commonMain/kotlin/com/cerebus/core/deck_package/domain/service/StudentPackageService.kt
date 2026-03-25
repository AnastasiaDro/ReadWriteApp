package com.cerebus.core.deck_package.domain.service

import com.cerebus.core.utils.CustomResult

data class StudentPackageExportFile(
    val path: String,
    val fileName: String,
)

data class StudentPackageImportPreview(
    val studentName: String,
    val existingStudentId: String? = null,
    val existingStudentName: String? = null,
    val matchedDecksCount: Int = 0,
    val missingDecksCount: Int = 0,
    val matchedCardsCount: Int = 0,
    val missingCardsCount: Int = 0,
)

data class StudentPackageImportResult(
    val studentId: String,
    val studentName: String,
    val matchedDecksCount: Int,
    val restoredCardsCount: Int,
    val skippedDecksCount: Int,
    val skippedCardsCount: Int,
    val updatedExistingStudent: Boolean,
)

interface StudentPackageService {
    suspend fun exportStudent(studentId: String): CustomResult<StudentPackageExportFile>

    suspend fun inspectStudentImport(
        archiveUri: String,
    ): CustomResult<StudentPackageImportPreview>

    suspend fun importStudent(
        archiveUri: String,
    ): CustomResult<StudentPackageImportResult>
}
