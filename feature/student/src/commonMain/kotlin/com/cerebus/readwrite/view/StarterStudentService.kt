package com.cerebus.readwrite.view

import com.cerebus.core.utils.CustomResult
import com.cerebus.core.utils.UniqueIdGenerator
import com.cerebus.data.preferences.domain.repositories.PreferencesRepository
import com.cerebus.data.student.domain.models.Student
import com.cerebus.data.student.domain.repositories.StudentRepository

interface StarterDeckInstaller {
    suspend fun installStarterDeck(studentId: String): CustomResult<Unit>
}

interface StarterStudentService {
    suspend fun createStudentWithStarterDeck(
        name: String,
        avatarUri: String?,
    ): CustomResult<String>
}

class StarterStudentServiceImpl(
    private val studentRepository: StudentRepository,
    private val preferencesRepository: PreferencesRepository,
    private val starterDeckInstaller: StarterDeckInstaller,
) : StarterStudentService {
    override suspend fun createStudentWithStarterDeck(
        name: String,
        avatarUri: String?,
    ): CustomResult<String> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Student name is blank"))
        }

        val studentId = UniqueIdGenerator.randomAlphanumeric(prefix = "student")
        val created = studentRepository.createStudent(
            Student(
                id = studentId,
                name = trimmedName,
                avatarUri = avatarUri,
                activeLetters = "",
            )
        )
        if (!created) {
            return CustomResult.Failure(IllegalStateException("Unable to create student"))
        }

        return when (val starterDeckResult = starterDeckInstaller.installStarterDeck(studentId)) {
            is CustomResult.Success -> {
                preferencesRepository.setLastActiveStudentId(studentId)
                CustomResult.Success(studentId)
            }
            is CustomResult.Failure -> {
                runCatching { studentRepository.deleteStudent(studentId) }
                CustomResult.Failure(starterDeckResult.error)
            }
        }
    }
}
