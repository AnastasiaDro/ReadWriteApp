package com.cerebus.game_screen.navigation

import androidx.navigation.NavHostController

class GameScreenNavigatorImpl(private val navController: NavHostController) : GameScreenNavigator {

    override fun goBack() {
        navController.navigateUp()
    }
}