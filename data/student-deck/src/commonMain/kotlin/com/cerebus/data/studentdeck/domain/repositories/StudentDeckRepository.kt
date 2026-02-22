package com.cerebus.data.studentdeck.domain.repositories

import com.cerebus.core.utils.CustomResult
import com.cerebus.data.studentdeck.domain.models.BulkAssignResult
import com.cerebus.data.studentdeck.domain.models.BulkUnassignResult
import com.cerebus.data.studentdeck.domain.models.StudentWithDecks

interface StudentDeckRepository {
    suspend fun getStudentWithDecks(studentId: String): StudentWithDecks?
    suspend fun assignDeckToStudent(studentId: String, deckId: String): Boolean
    suspend fun assignDecksToStudent(
        studentId: String,
        deckIds: List<String>,
    ): CustomResult<BulkAssignResult>

    suspend fun unassignDeckFromStudent(studentId: String, deckId: String): Boolean
    suspend fun unassignDecksFromStudent(
        studentId: String,
        deckIds: List<String>,
    ): CustomResult<BulkUnassignResult>
}
