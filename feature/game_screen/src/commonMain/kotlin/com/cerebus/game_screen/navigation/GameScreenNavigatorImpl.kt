package com.cerebus.game_screen.navigation

import androidx.navigation.NavHostController

class GameScreenNavigatorImpl(
    private val navController: NavHostController,
) : GameScreenNavigator {

    override fun openActiveStudent() {
        val openedFromActiveStack = navController.popBackStack("active_student", inclusive = false)
        if (openedFromActiveStack) return

        navController.navigate("active_student") {
            launchSingleTop = true
        }
    }

    override fun closeGame() {
        val returnRoute = GameSessionNavigationState.returnRoute
        if (!returnRoute.isNullOrBlank()) {
            val poppedToReturnRoute = navController.popBackStack(returnRoute, inclusive = false)
            if (poppedToReturnRoute) return

            navController.navigate(returnRoute) {
                launchSingleTop = true
            }
            return
        }

        val closed = navController.popBackStack()
        if (closed) return

        openActiveStudent()
    }
}
