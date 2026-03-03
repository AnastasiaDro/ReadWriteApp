package com.cerebus.data.studyprogress.domain.models

enum class StudyState(val dbValue: Int) {
    NEW(dbValue = 0),
    LEARNING(dbValue = 1),
    REVIEW(dbValue = 2),
    ;

    companion object {
        fun fromDbValue(value: Int): StudyState {
            return entries.firstOrNull { it.dbValue == value } ?: NEW
        }
    }
}
