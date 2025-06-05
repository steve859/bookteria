package com.soft.bookteria.ui.screens.main

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.soft.bookteria.helpers.NetworkObserver
import com.soft.bookteria.ui.navigation.BottomBarScreen
import com.soft.bookteria.ui.screens.home.composables.HomeScreenContainer

@Composable
fun MainScreen(
    intent: Intent,            // Nếu bạn cần xử lý intent, có thể truyền vào
    startDestination: String,  // Ví dụ: BottomBarScreen.Home.route
    networkStatus: NetworkObserver.Status  // Trạng thái mạng
) {
    val navController = rememberNavController()
    
    // Scaffold bao quanh NavHost, với bottomBar được vẽ bởi BottomNavigationBar()
    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Tab Home
            composable(route = BottomBarScreen.Home.route) {
                HomeScreenContainer(
                    viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                    networkStatus = networkStatus,
                    navController = navController,
                    backState = remember { mutableStateOf(false) }
                )
            }
//            // Tab Categories (placeholder)
//            composable(route = BottomBarScreen.Categories.route) {
//                CategoriesScreen()
//            }
//            // Tab Library (placeholder)
//            composable(route = BottomBarScreen.Library.route) {
//                LibraryScreen()
//            }
//            // Tab Settings (placeholder)
//            composable(route = BottomBarScreen.Settings.route) {
//                SettingsScreen()
//            }
        }
    }
}



@Composable
private fun BottomNavigationBar(navController: NavHostController) {
    val screens = listOf(
        BottomBarScreen.Home,
        BottomBarScreen.Categories,
        BottomBarScreen.Library,
        BottomBarScreen.Settings
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        screens.forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = screen.icon),
                        contentDescription = stringResource(id = screen.title)
                    )
                },
                label = { androidx.compose.material3.Text(text = stringResource(id = screen.title)) },
                selected = selected,
                onClick = {
                    // Khi nhấn một tab, chuyển đến route tương ứng
                    navController.navigate(screen.route) {
                        // Tránh tạo nhiều bản sao của cùng route
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Giữ lại state nếu quay lại
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
