package com.cerebus.data.studentdeck.data.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.cerebus.data.decks.data.entity.DeckEntity
import com.cerebus.data.student.data.entity.StudentEntity

data class StudentWithDecks(
    @Embedded val student: StudentEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = StudentDeckCrossRef::class,
            parentColumn = "studentId",
            entityColumn = "deckId",
        ),
    )
    val decks: List<DeckEntity>,
)
