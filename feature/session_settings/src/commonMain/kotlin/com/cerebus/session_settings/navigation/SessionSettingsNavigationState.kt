package com.cerebus.session_settings.navigation

enum class SessionSettingsScrollTarget {
    TypoSettings,
}

object SessionSettingsNavigationState {
    var selectedStudentId: String? = null
    var returnRoute: String? = null
    var scrollTarget: SessionSettingsScrollTarget? = null

    fun open(
        studentId: String,
        returnRoute: String,
        scrollTarget: SessionSettingsScrollTarget? = null,
    ) {
        selectedStudentId = studentId
        this.returnRoute = returnRoute
        this.scrollTarget = scrollTarget
    }

    fun clear() {
        selectedStudentId = null
        returnRoute = null
        scrollTarget = null
    }
}
