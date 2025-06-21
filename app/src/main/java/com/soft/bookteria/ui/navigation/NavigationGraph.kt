package com.soft.bookteria.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.soft.bookteria.helpers.NetworkObserver
import com.soft.bookteria.ui.screens.main.MainScreen
import com.soft.bookteria.ui.screens.welcome.composables.WelcomeScreen

@Composable
fun NavigationGraph(
    startDestination: String,
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {
        composable(
            route = NavigationScreens.WelcomeScreen.route
        ) {
            WelcomeScreen(navController = navController)
        }
        
        composable(
            route = "main_screen"
        ) {
            MainScreen(
                startDestination = BottomBarScreen.Home.route,
                networkStatus = NetworkObserver.Status.Avaiable // TODO: Get from NetworkObserver
            )
        }
    }
}

