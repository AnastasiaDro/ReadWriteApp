package com.cerebus.fairy_tales.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.cerebus.fairy_tales.presentation.FairyTalesRoute

object FairyTalesGraph {
    const val GRAPH_ROUTE = "fairy_tales_graph"
    const val SCREEN_ROUTE = "fairy_tales"
}

fun NavGraphBuilder.fairyTalesGraph() {
    navigation(
        startDestination = FairyTalesGraph.SCREEN_ROUTE,
        route = FairyTalesGraph.GRAPH_ROUTE,
    ) {
        composable(FairyTalesGraph.SCREEN_ROUTE) {
            FairyTalesRoute()
        }
    }
}
