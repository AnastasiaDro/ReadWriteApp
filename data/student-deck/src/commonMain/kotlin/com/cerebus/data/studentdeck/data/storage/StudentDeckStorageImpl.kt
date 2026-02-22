package com.cerebus.data.studentdeck.data.storage

import com.cerebus.core.utils.CustomResult
import com.cerebus.data.studentdeck.data.dao.StudentDeckDao
import com.cerebus.data.studentdeck.data.entity.StudentDeckCrossRef
import com.cerebus.data.studentdeck.data.entity.StudentWithDecks
import com.cerebus.data.studentdeck.domain.models.BulkAssignResult
import com.cerebus.data.studentdeck.domain.models.BulkUnassignResult

class StudentDeckStorageImpl(
    private val dao: StudentDeckDao,
) : StudentDeckStorage {
    override suspend fun getStudentWithDecks(studentId: String): StudentWithDecks? {
        return runCatching { dao.getStudentWithDecks(studentId) }.getOrNull()
    }

    override suspend fun assignDeckToStudent(studentId: String, deckId: String): Boolean {
        return runCatching {
            dao.insertCrossRef(
                StudentDeckCrossRef(
                    studentId = studentId,
                    deckId = deckId,
                )
            ) != -1L
        }.getOrDefault(false)
    }

    override suspend fun assignDecksToStudent(
        studentId: String,
        deckIds: List<String>,
    ): CustomResult<BulkAssignResult> {
        if (deckIds.isEmpty()) {
            return CustomResult.Success(
                BulkAssignResult(
                    insertedCount = 0,
                    failedCount = 0,
                    failedDeckIds = emptyList(),
                    alreadyLinkedDeckIds = emptyList(),
                )
            )
        }

        return runCatching {
            val crossRefs = deckIds.map { deckId ->
                StudentDeckCrossRef(
                    studentId = studentId,
                    deckId = deckId,
                )
            }
            val results = dao.insertCrossRefs(crossRefs)
            val alreadyLinkedDeckIds = buildList {
                results.forEachIndexed { index, value ->
                    if (value == -1L) add(deckIds[index])
                }
            }

            CustomResult.Success(
                BulkAssignResult(
                    insertedCount = results.count { it != -1L },
                    failedCount = 0,
                    failedDeckIds = emptyList(),
                    alreadyLinkedDeckIds = alreadyLinkedDeckIds,
                )
            )
        }.getOrElse { error ->
            CustomResult.Failure(error)
        }
    }

    override suspend fun unassignDeckFromStudent(studentId: String, deckId: String): Boolean {
        return runCatching { dao.deleteCrossRef(studentId, deckId) > 0 }.getOrDefault(false)
    }

    override suspend fun unassignDecksFromStudent(
        studentId: String,
        deckIds: List<String>,
    ): CustomResult<BulkUnassignResult> {
        if (deckIds.isEmpty()) {
            return CustomResult.Success(
                BulkUnassignResult(
                    deletedCount = 0,
                    failedCount = 0,
                    failedDeckIds = emptyList(),
                    notLinkedDeckIds = emptyList(),
                )
            )
        }

        return runCatching {
            val linkedDeckIds = dao.getLinkedDeckIds(studentId, deckIds).toSet()
            val notLinkedDeckIds = deckIds.filterNot { it in linkedDeckIds }

            dao.deleteCrossRefs(studentId, deckIds)
            val failedDeckIds = dao.getLinkedDeckIds(studentId, deckIds)
            val deletedCount = deckIds.size - notLinkedDeckIds.size - failedDeckIds.size

            CustomResult.Success(
                BulkUnassignResult(
                    deletedCount = deletedCount,
                    failedCount = failedDeckIds.size,
                    failedDeckIds = failedDeckIds,
                    notLinkedDeckIds = notLinkedDeckIds,
                )
            )
        }.getOrElse { error ->
            CustomResult.Failure(error)
        }
    }
}
