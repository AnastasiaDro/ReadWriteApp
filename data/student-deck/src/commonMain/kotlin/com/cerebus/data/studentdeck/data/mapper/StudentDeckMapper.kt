package com.cerebus.data.studentdeck.data.mapper

import com.cerebus.data.decks.domain.models.Deck
import com.cerebus.data.student.domain.models.Student
import com.cerebus.data.studentdeck.data.entity.StudentWithDecks
import com.cerebus.data.studentdeck.domain.models.StudentWithDecks as StudentWithDecksDomain

fun StudentWithDecks.toDomain(): StudentWithDecksDomain {
    return StudentWithDecksDomain(
        student = Student(
            id = student.id,
            name = student.name,
            activeLetters = student.activeLetters,
        ),
        decks = decks.map { deck ->
            Deck(
                id = deck.id,
                name = deck.name,
                coverUri = deck.coverUri,
            )
        },
    )
}
