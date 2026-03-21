package com.cerebus.fairy_tales.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.cerebus.fairy_tales.presentation.FairyTalesScreenRoute
import com.cerebus.fairy_tales.presentation.FairyTalesRoute

object FairyTalesGraph {
    const val GRAPH_ROUTE = "fairy_tales_graph"
    const val LIST_ROUTE = "fairy_tales"
    const val DETAIL_ROUTE = "fairy_tale"
}

object FairyTalesNavigationState {
    var selectedFairyTaleId: String = ""
}

fun NavGraphBuilder.fairyTalesGraph(
    navController: NavHostController,
) {
    navigation(
        startDestination = FairyTalesGraph.LIST_ROUTE,
        route = FairyTalesGraph.GRAPH_ROUTE,
    ) {
        composable(FairyTalesGraph.LIST_ROUTE) {
            FairyTalesRoute(
                onOpenFairyTale = { fairyTaleId ->
                    FairyTalesNavigationState.selectedFairyTaleId = fairyTaleId
                    navController.navigate(FairyTalesGraph.DETAIL_ROUTE)
                },
            )
        }
        composable(FairyTalesGraph.DETAIL_ROUTE) {
            FairyTalesScreenRoute(
                fairyTaleId = FairyTalesNavigationState.selectedFairyTaleId,
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}
