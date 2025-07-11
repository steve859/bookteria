package com.soft.bookteria.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.soft.bookteria.helpers.NetworkObserver
import com.soft.bookteria.ui.screens.categories.composables.CategoryDetailScreen
import com.soft.bookteria.ui.screens.details.composables.DetailsScreen
import com.soft.bookteria.ui.screens.main.MainScreen
import com.soft.bookteria.ui.screens.reader.ReaderDetailScreen
import com.soft.bookteria.ui.screens.reader.ReaderScreen
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
                networkStatus = NetworkObserver.Status.Avaiable, // TODO: Get from NetworkObserver
                navController = navController // Truyền NavController cấp cao nhất vào
            )
        }
        
        composable(
            route = NavigationScreens.CategoryScreen.route,
            arguments = listOf(
                navArgument(CATEGORY_DETAIL_ARG_KEY) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString(CATEGORY_DETAIL_ARG_KEY) ?: ""
            CategoryDetailScreen(
                navController = navController,
                category = category
            )
        }
        
        composable(
            route = NavigationScreens.BookDetailScreen.route,
            arguments = listOf(
                navArgument(BOOK_ID_ARG_KEY) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString(BOOK_ID_ARG_KEY) ?: ""
            DetailsScreen(
                navController = navController,
                bookId = bookId
            )
        }
        
        composable(
            route = NavigationScreens.ReaderDetailScreen.route,
            arguments = listOf(
                navArgument(LIBRARY_OBJECT_ID_ARG_KEY) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val libraryObjectId = backStackEntry.arguments?.getString(LIBRARY_OBJECT_ID_ARG_KEY) ?: ""
            ReaderDetailScreen(
                libraryObjectId = libraryObjectId,
                navController = navController
            )
        }
        
        composable(
            route = NavigationScreens.ReaderScreen.route,
            arguments = listOf(
                navArgument(LIBRARY_OBJECT_ID_ARG_KEY) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val libraryObjectId = backStackEntry.arguments?.getString(LIBRARY_OBJECT_ID_ARG_KEY) ?: ""
            ReaderScreen(
                navController = navController,
                libraryObjectId = libraryObjectId
            )
        }
    }
}

