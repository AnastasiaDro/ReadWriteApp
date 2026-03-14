package com.cerebus.customkeyboard.navigation

object KeyboardSettingsNavigationState {
    var selectedStudentId: String? = null
    var returnRoute: String? = null

    fun open(studentId: String, returnRoute: String) {
        selectedStudentId = studentId
        this.returnRoute = returnRoute
    }

    fun clear() {
        selectedStudentId = null
        returnRoute = null
    }
}
