package com.cerebus.readwrite

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cerebus.create_screen.navigation.DeckNavigationState
import com.cerebus.game_screen.presentation.GameScreenWrapper
import com.cerebus.readwrite.view.CreateScreenRoute
import com.cerebus.readwrite.view.DeckScreenRoute
import com.cerebus.readwrite.view.HomeScreen
import com.cerebus.tutube.navigation.Screens

@Composable
fun ReadWriteAppNavigation() = MaterialTheme {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screens.HOME.route) {

        composable(Screens.HOME.route) {
            HomeScreen(
                onNavigateToCreate = {
                    navController.navigate(Screens.CREATE.route)
                }
            )
        }
        composable(Screens.CREATE.route) {
            CreateScreenRoute(
                onBackClick = { navController.popBackStack() },
                onNavigateToDeck = { deckId ->
                    DeckNavigationState.selectedDeckId = deckId
                    navController.navigate(Screens.DECK.route)
                }
            )
        }
        composable(Screens.DECK.route) {
            DeckScreenRoute(
                deckId = DeckNavigationState.selectedDeckId,
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(Screens.GAME.route) {
            GameScreenWrapper(navController)
        }
    }
}
