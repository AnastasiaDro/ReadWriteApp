package com.cerebus.readwrite

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Modifier
import com.cerebus.create_screen.presentation.DeckGalleryRoute
import com.cerebus.create_screen.presentation.DeckGalleryStrings
import com.cerebus.customkeyboard.KeyboardSettingsRoute
import com.cerebus.customkeyboard.navigation.KeyboardSettingsNavigationState
import com.cerebus.core.ui.insets.bottomSystemBarPadding
import com.cerebus.core.ui.insets.topSystemBarPadding
import com.cerebus.create_screen.navigation.CreateNavigationState
import com.cerebus.create_screen.navigation.DeckNavigationState
import com.cerebus.core.utils.GameLaunchMode
import com.cerebus.game_screen.navigation.GameSessionNavigationState
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
import com.cerebus.session_settings.navigation.SessionSettingsNavigationState
import com.cerebus.session_settings.navigation.SessionSettingsScrollTarget
import com.cerebus.session_settings.presentation.SessionSettingsRoute
import com.cerebus.tutube.navigation.Screens
import org.jetbrains.compose.resources.stringResource
import readwriteapp.composeapp.generated.resources.Res
import readwriteapp.composeapp.generated.resources.back
import readwriteapp.composeapp.generated.resources.deck_gallery_empty
import readwriteapp.composeapp.generated.resources.deck_gallery_copy_stage
import readwriteapp.composeapp.generated.resources.deck_gallery_correct_feedback
import readwriteapp.composeapp.generated.resources.deck_gallery_edit
import readwriteapp.composeapp.generated.resources.deck_gallery_next
import readwriteapp.composeapp.generated.resources.deck_gallery_previous
import readwriteapp.composeapp.generated.resources.deck_gallery_recall_stage
import readwriteapp.composeapp.generated.resources.deck_gallery_show_word
import readwriteapp.composeapp.generated.resources.deck_gallery_simplify_keyboard
import readwriteapp.composeapp.generated.resources.deck_gallery_submit
import readwriteapp.composeapp.generated.resources.deck_gallery_wrong_feedback
import readwriteapp.composeapp.generated.resources.unnamed_deck

@Composable
fun ReadWriteAppNavigation() = MaterialTheme {

    val navController = rememberNavController()
    val pendingImportDeckArchiveUri = CreateNavigationState.pendingImportDeckArchiveUri.collectAsState().value
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val containerTopPadding = topSystemBarPadding()
    val containerBottomPadding = bottomSystemBarPadding()

    LaunchedEffect(pendingImportDeckArchiveUri, currentRoute) {
        if (pendingImportDeckArchiveUri.isNullOrBlank()) return@LaunchedEffect
        if (currentRoute == null || currentRoute == Screens.SPLASH.route) return@LaunchedEffect
        if (currentRoute == Screens.CREATE.route) return@LaunchedEffect
        navController.navigate(Screens.CREATE.route) {
            launchSingleTop = true
        }
    }

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
                    onOpenGame = { deckIds, mode ->
                        GameSessionNavigationState.selectedDeckIds = deckIds
                        GameSessionNavigationState.launchMode = mode
                        navController.navigate(Screens.GAME.route)
                    },
                    onOpenDeckGallery = {
                        navController.navigate(Screens.DECK_GALLERY.route)
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
                    onOpenSessionSettings = { studentId ->
                        navController.openSessionSettings(
                            studentId = studentId,
                            returnRoute = Screens.ACTIVE_STUDENT.route,
                        )
                    },
                    onOpenKeyboardSettings = { studentId ->
                        navController.openKeyboardSettings(
                            studentId = studentId,
                            returnRoute = Screens.ACTIVE_STUDENT.route,
                        )
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
                        navController.navigate(Screens.ACTIVE_STUDENT.route) {
                            popUpTo(Screens.ACTIVE_STUDENT.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Screens.CREATE.route) {
                CreateScreenRoute(
                    onBackClick = { navController.popBackStack() },
                    onNavigateToDeck = { deckId, openAddCardDialog ->
                        DeckNavigationState.selectDeck(
                            deckId = deckId,
                            openAddCardDialog = openAddCardDialog,
                        )
                        navController.navigate(Screens.DECK.route)
                    }
                )
            }
            composable(Screens.DECK.route) {
                DeckScreenRoute(
                    deckId = DeckNavigationState.selectedDeckId,
                    onBackClick = { navController.openActiveStudentFromDeck() },
                    onOpenGame = { deckId, mode ->
                        GameSessionNavigationState.selectedDeckIds = listOf(deckId)
                        GameSessionNavigationState.launchMode = mode
                        navController.navigate(Screens.GAME.route)
                    },
                    onOpenGallery = {
                        navController.navigate(Screens.DECK_GALLERY.route)
                    },
                )
            }
            composable(Screens.DECK_GALLERY.route) {
                val initialCardId = remember {
                    DeckNavigationState.consumeOpenGalleryCardIdOnNextOpen()
                }
                DeckGalleryRoute(
                    deckId = DeckNavigationState.selectedDeckId,
                    initialCardId = initialCardId,
                    strings = DeckGalleryStrings(
                        back = stringResource(Res.string.back),
                        titleFallback = stringResource(Res.string.unnamed_deck),
                        previous = stringResource(Res.string.deck_gallery_previous),
                        next = stringResource(Res.string.deck_gallery_next),
                        empty = stringResource(Res.string.deck_gallery_empty),
                        edit = stringResource(Res.string.deck_gallery_edit),
                        submit = stringResource(Res.string.deck_gallery_submit),
                        showWord = stringResource(Res.string.deck_gallery_show_word),
                        simplifyKeyboard = stringResource(Res.string.deck_gallery_simplify_keyboard),
                        copyStage = stringResource(Res.string.deck_gallery_copy_stage),
                        recallStage = stringResource(Res.string.deck_gallery_recall_stage),
                        correctFeedback = stringResource(Res.string.deck_gallery_correct_feedback),
                        wrongFeedback = stringResource(Res.string.deck_gallery_wrong_feedback),
                    ),
                    onBackClick = { navController.popBackStack() },
                    onEditCard = { deckId, cardId ->
                        navController.openDeckEditor(deckId, cardId)
                    },
                )
            }
            composable(Screens.GAME.route) {
                GameScreenWrapper(
                    navController = navController,
                    deckIds = GameSessionNavigationState.selectedDeckIds.ifEmpty {
                        listOfNotNull(DeckNavigationState.selectedDeckId.takeIf { it.isNotBlank() })
                    },
                    launchMode = GameSessionNavigationState.launchMode,
                    onOpenSessionSettings = { studentId, scrollToTypos ->
                        navController.openSessionSettings(
                            studentId = studentId,
                            returnRoute = Screens.GAME.route,
                            scrollTarget = if (scrollToTypos) {
                                SessionSettingsScrollTarget.TypoSettings
                            } else {
                                null
                            },
                        )
                    },
                    onOpenKeyboardSettings = { studentId ->
                        navController.openKeyboardSettings(
                            studentId = studentId,
                            returnRoute = Screens.GAME.route,
                        )
                    },
                )
            }
            composable(Screens.SESSION_SETTINGS.route) {
                val studentId = remember { SessionSettingsNavigationState.selectedStudentId }
                val returnRoute = remember { SessionSettingsNavigationState.returnRoute }
                val scrollTarget = remember { SessionSettingsNavigationState.scrollTarget }
                if (studentId.isNullOrBlank()) {
                    SessionSettingsNavigationState.clear()
                    LaunchedEffect(Unit) {
                        val popped = navController.returnFromSessionSettings(returnRoute)
                        if (!popped) {
                            navController.navigate(Screens.ACTIVE_STUDENT.route) {
                                launchSingleTop = true
                            }
                        }
                    }
                    return@composable
                }
                SessionSettingsRoute(
                    studentId = studentId,
                    scrollTarget = scrollTarget,
                    onOpenKeyboardSettings = {
                        navController.openKeyboardSettings(
                            studentId = studentId,
                            returnRoute = Screens.SESSION_SETTINGS.route,
                        )
                    },
                    onClose = {
                        val popped = navController.returnFromSessionSettings(returnRoute)
                        if (!popped) {
                            navController.navigate(returnRoute ?: Screens.ACTIVE_STUDENT.route) {
                                launchSingleTop = true
                            }
                        }
                        SessionSettingsNavigationState.clear()
                    },
                )
            }
            composable(Screens.KEYBOARD_SETTINGS.route) {
                val studentId = remember { KeyboardSettingsNavigationState.selectedStudentId }
                val returnRoute = remember { KeyboardSettingsNavigationState.returnRoute }
                if (studentId.isNullOrBlank()) {
                    KeyboardSettingsNavigationState.clear()
                    LaunchedEffect(Unit) {
                        val popped = navController.returnFromKeyboardSettings(returnRoute)
                        if (!popped) {
                            navController.navigate(Screens.ACTIVE_STUDENT.route) {
                                launchSingleTop = true
                            }
                        }
                    }
                    return@composable
                }

                KeyboardSettingsRoute(
                    studentId = studentId,
                    onClose = {
                        val popped = navController.returnFromKeyboardSettings(returnRoute)
                        if (!popped) {
                            navController.navigate(returnRoute ?: Screens.ACTIVE_STUDENT.route) {
                                launchSingleTop = true
                            }
                        }
                        KeyboardSettingsNavigationState.clear()
                    },
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

private fun androidx.navigation.NavHostController.openDeckEditor(
    deckId: String,
    cardId: String,
) {
    DeckNavigationState.selectDeck(
        deckId = deckId,
        openEditCardId = cardId,
    )
    val replacedExistingDeck = popBackStack(Screens.DECK.route, inclusive = true)
    navigate(Screens.DECK.route) {
        launchSingleTop = true
        if (replacedExistingDeck) {
            restoreState = false
        }
    }
}

private fun androidx.navigation.NavHostController.openKeyboardSettings(
    studentId: String,
    returnRoute: String,
) {
    KeyboardSettingsNavigationState.open(
        studentId = studentId,
        returnRoute = returnRoute,
    )
    navigate(Screens.KEYBOARD_SETTINGS.route)
}

private fun androidx.navigation.NavHostController.openSessionSettings(
    studentId: String,
    returnRoute: String,
    scrollTarget: SessionSettingsScrollTarget? = null,
) {
    SessionSettingsNavigationState.open(
        studentId = studentId,
        returnRoute = returnRoute,
        scrollTarget = scrollTarget,
    )
    navigate(Screens.SESSION_SETTINGS.route)
}

private fun androidx.navigation.NavHostController.returnFromKeyboardSettings(
    returnRoute: String?,
): Boolean {
    val targetRoute = returnRoute ?: return popBackStack()
    val poppedToTarget = popBackStack(targetRoute, inclusive = false)
    if (poppedToTarget) return true

    popBackStack()
    return false
}

private fun androidx.navigation.NavHostController.returnFromSessionSettings(
    returnRoute: String?,
): Boolean {
    val targetRoute = returnRoute ?: return popBackStack()
    val poppedToTarget = popBackStack(targetRoute, inclusive = false)
    if (poppedToTarget) return true

    popBackStack()
    return false
}
