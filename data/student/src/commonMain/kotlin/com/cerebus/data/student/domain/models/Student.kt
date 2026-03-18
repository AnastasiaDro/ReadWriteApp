package com.cerebus.data.student.domain.models

data class Student(
    val id: String,
    val name: String,
    val avatarUri: String? = null,
    val activeLetters: String,
)
