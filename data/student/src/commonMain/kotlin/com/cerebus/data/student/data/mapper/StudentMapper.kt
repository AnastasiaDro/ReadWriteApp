package com.cerebus.data.student.data.mapper

import com.cerebus.data.student.data.entity.StudentEntity
import com.cerebus.data.student.domain.models.Student

fun StudentEntity.toDomain(): Student = Student(
    id = id,
    name = name,
    avatarUri = avatarUri,
    activeLetters = activeLetters,
)

fun Student.toEntity(): StudentEntity = StudentEntity(
    id = id,
    name = name,
    avatarUri = avatarUri,
    activeLetters = activeLetters,
)
