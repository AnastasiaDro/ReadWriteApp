package com.cerebus.readwrite.navigation

object CreateStudentNavigationState {
    private var returnToActiveStudent: Boolean = false
    private var pendingCreatedStudentId: String? = null

    fun setReturnToActiveStudent(value: Boolean) {
        returnToActiveStudent = value
    }

    fun setPendingCreatedStudentId(studentId: String) {
        pendingCreatedStudentId = studentId
    }

    fun consumePendingCreatedStudentId(): String? {
        val studentId = pendingCreatedStudentId
        pendingCreatedStudentId = null
        return studentId
    }

    fun consumeReturnToActiveStudent(): Boolean {
        val value = returnToActiveStudent
        returnToActiveStudent = false
        return value
    }
}
