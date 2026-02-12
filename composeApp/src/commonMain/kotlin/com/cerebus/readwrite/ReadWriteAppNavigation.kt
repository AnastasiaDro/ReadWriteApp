package com.cerebus.readwrite

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cerebus.readwrite.view.HomeScreen
import com.cerebus.tutube.navigation.Screens

@Composable
fun ReadWriteAppNavigation() = MaterialTheme {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screens.HOME.route) {

        composable(Screens.HOME.route) {
            HomeScreen(
//                onNavigateToProfile = {
//                navController.navigate(Screens.AUTHORIZATION.route)
//            }
            )
        }
//        composable(Screens.AUTHORIZATION.route) {
//            AuthScreenWrapper(navController)
//        }
//        composable(Screens.FILL_USER_PROFILE.route) {
//            ProfileScreenWrapper(navController)
//        }
    }
}