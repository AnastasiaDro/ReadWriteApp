package com.cerebus.data.studentdeck.domain.models

import com.cerebus.data.decks.domain.models.Deck
import com.cerebus.data.student.domain.models.Student

data class StudentWithDecks(
    val student: Student,
    val decks: List<Deck>,
)
