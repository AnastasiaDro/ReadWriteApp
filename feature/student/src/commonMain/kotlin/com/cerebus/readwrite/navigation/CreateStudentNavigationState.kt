package com.cerebus.readwrite.navigation

object CreateStudentNavigationState {
    private var returnToActiveStudent: Boolean = false

    fun setReturnToActiveStudent(value: Boolean) {
        returnToActiveStudent = value
    }

    fun consumeReturnToActiveStudent(): Boolean {
        val value = returnToActiveStudent
        returnToActiveStudent = false
        return value
    }
}
