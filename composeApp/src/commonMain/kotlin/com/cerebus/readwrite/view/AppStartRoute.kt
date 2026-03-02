package com.cerebus.readwrite.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.cerebus.tutube.navigation.Screens
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppStartRoute(
    navController: NavHostController,
) {
    val viewModel = koinViewModel<AppStartViewModel>()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.pendingRoute) {
        when (state.pendingRoute) {
            StartRouteTarget.HAS_STUDENTS -> {
                navController.openFlowForExistingStudents()
                viewModel.onRouteHandled()
            }

            StartRouteTarget.NO_STUDENTS -> {
                navController.openFlowWhenNoStudents()
                viewModel.onRouteHandled()
            }

            null -> Unit
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (state.isLoading) {
            CircularProgressIndicator()
        }
    }
}

private fun NavHostController.openFlowForExistingStudents() {
    navigate(Screens.ACTIVE_STUDENT.route) {
        popUpTo(graph.startDestinationId) { inclusive = true }
        launchSingleTop = true
    }
}

private fun NavHostController.openFlowWhenNoStudents() {
    // TODO: Open onboarding flow with student setup entry point.
    navigate(Screens.NO_STUDENTS.route) {
        popUpTo(graph.startDestinationId) { inclusive = true }
        launchSingleTop = true
    }
}
