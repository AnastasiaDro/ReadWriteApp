package com.cerebus.readwrite

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Modifier
import com.cerebus.core.ui.insets.bottomSystemBarPadding
import com.cerebus.core.ui.insets.topSystemBarPadding
import com.cerebus.create_screen.navigation.CreateNavigationState
import com.cerebus.create_screen.navigation.DeckNavigationState
import com.cerebus.game_screen.presentation.GameScreenWrapper
import com.cerebus.readwrite.navigation.CreateStudentNavigationState
import com.cerebus.readwrite.view.CreateScreenRoute
import com.cerebus.readwrite.view.CreateStudentRoute
import com.cerebus.readwrite.view.DeckScreenRoute
import com.cerebus.readwrite.view.HomeScreen
import com.cerebus.readwrite.view.AppStartRoute
import com.cerebus.readwrite.view.ActiveStudentRoute
import com.cerebus.readwrite.view.ChangeStudentRoute
import com.cerebus.readwrite.view.NoStudentsScreen
import com.cerebus.tutube.navigation.Screens

@Composable
fun ReadWriteAppNavigation() = MaterialTheme {

    val navController = rememberNavController()
    val containerTopPadding = topSystemBarPadding()
    val containerBottomPadding = bottomSystemBarPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = containerTopPadding, bottom = containerBottomPadding),
    ) {
        NavHost(
            navController = navController,
            startDestination = Screens.SPLASH.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Screens.SPLASH.route) {
                AppStartRoute(navController = navController)
            }

            composable(Screens.HOME.route) {
                HomeScreen(
                    onNavigateToCreate = {
                        navController.navigate(Screens.CREATE.route)
                    }
                )
            }
            composable(Screens.ACTIVE_STUDENT.route) {
                ActiveStudentRoute(
                    onOpenDeck = {
                        navController.navigate(Screens.DECK.route)
                    },
                    onOpenGame = {
                        navController.navigate(Screens.GAME.route)
                    },
                    onOpenDeckList = { openCreateDialog ->
                        if (openCreateDialog) {
                            CreateNavigationState.requestOpenCreateDialog()
                        }
                        navController.navigate(Screens.CREATE.route)
                    },
                    onOpenChangeStudent = {
                        navController.navigate(Screens.CHANGE_STUDENT.route)
                    },
                )
            }
            composable(Screens.CHANGE_STUDENT.route) {
                ChangeStudentRoute(
                    onBackClick = { navController.popBackStack() },
                    onOpenDeck = {
                        navController.navigate(Screens.DECK.route)
                    },
                    onOpenCreateStudent = {
                        CreateStudentNavigationState.setReturnToActiveStudent(true)
                        navController.navigate(Screens.CREATE_STUDENT.route)
                    },
                )
            }
            composable(Screens.NO_STUDENTS.route) {
                NoStudentsScreen(
                    onAddStudentClick = {
                        CreateStudentNavigationState.setReturnToActiveStudent(false)
                        navController.navigate(Screens.CREATE_STUDENT.route)
                    },
                    onTryDemoClick = {
                        onTryDemoClicked()
                    },
                )
            }
            composable(Screens.CREATE_STUDENT.route) {
                CreateStudentRoute(
                    onNavigateToDeckList = {
                        navController.navigate(Screens.CREATE.route)
                    },
                    onNavigateToActiveStudent = {
                        val openedFromActiveStack =
                            navController.popBackStack(Screens.ACTIVE_STUDENT.route, inclusive = false)
                        if (!openedFromActiveStack) {
                            navController.navigate(Screens.ACTIVE_STUDENT.route) {
                                launchSingleTop = true
                            }
                        }
                    },
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
                    onBackClick = { navController.openActiveStudentFromDeck() },
                    onOpenGame = {
                        navController.navigate(Screens.GAME.route)
                    },
                )
            }
            composable(Screens.GAME.route) {
                GameScreenWrapper(
                    navController = navController,
                    deckId = DeckNavigationState.selectedDeckId,
                )
            }
        }
    }
}

private fun onTryDemoClicked() {
    // TODO: Navigate to demo flow.
}

private fun androidx.navigation.NavHostController.openActiveStudentFromDeck() {
    val openedFromActiveStack = popBackStack(Screens.ACTIVE_STUDENT.route, inclusive = false)
    if (openedFromActiveStack) return

    navigate(Screens.ACTIVE_STUDENT.route) {
        popUpTo(Screens.NO_STUDENTS.route) { inclusive = true }
        launchSingleTop = true
    }
}
