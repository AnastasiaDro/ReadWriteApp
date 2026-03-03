package com.cerebus.data.studyprogress.domain.models

enum class EaseLevel(val dbValue: Int) {
    HARD(dbValue = 0),
    NORMAL(dbValue = 1),
    EASY(dbValue = 2),
    ;

    companion object {
        fun fromDbValue(value: Int): EaseLevel {
            return entries.firstOrNull { it.dbValue == value } ?: NORMAL
        }
    }
}
