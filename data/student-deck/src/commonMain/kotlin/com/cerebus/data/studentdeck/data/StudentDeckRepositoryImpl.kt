package com.cerebus.data.studentdeck.data

import com.cerebus.core.utils.CustomResult
import com.cerebus.data.studentdeck.data.mapper.toDomain
import com.cerebus.data.studentdeck.data.storage.StudentDeckStorage
import com.cerebus.data.studentdeck.domain.models.BulkAssignResult
import com.cerebus.data.studentdeck.domain.models.BulkUnassignResult
import com.cerebus.data.studentdeck.domain.models.StudentWithDecks
import com.cerebus.data.studentdeck.domain.repositories.StudentDeckRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StudentDeckRepositoryImpl(
    private val storage: StudentDeckStorage,
) : StudentDeckRepository {
    override suspend fun getStudentWithDecks(studentId: String): StudentWithDecks? {
        return storage.getStudentWithDecks(studentId)?.toDomain()
    }

    override fun observeStudentWithDecks(studentId: String): Flow<StudentWithDecks?> {
        return storage.observeStudentWithDecks(studentId).map { it?.toDomain() }
    }

    override fun observeStudentsWithDecksOrderedByCreation(): Flow<List<StudentWithDecks>> {
        return storage.observeStudentsWithDecksOrderedByCreation().map { students ->
            students.map { it.toDomain() }
        }
    }

    override suspend fun assignDeckToStudent(studentId: String, deckId: String): Boolean {
        return storage.assignDeckToStudent(studentId, deckId)
    }

    override suspend fun assignDecksToStudent(
        studentId: String,
        deckIds: List<String>,
    ): CustomResult<BulkAssignResult> {
        return storage.assignDecksToStudent(studentId, deckIds)
    }

    override suspend fun unassignDeckFromStudent(studentId: String, deckId: String): Boolean {
        return storage.unassignDeckFromStudent(studentId, deckId)
    }

    override suspend fun unassignDecksFromStudent(
        studentId: String,
        deckIds: List<String>,
    ): CustomResult<BulkUnassignResult> {
        return storage.unassignDecksFromStudent(studentId, deckIds)
    }
}
