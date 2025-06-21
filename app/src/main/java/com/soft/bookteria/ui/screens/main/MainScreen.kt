package com.soft.bookteria.ui.screens.main

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.soft.bookteria.helpers.NetworkObserver
import com.soft.bookteria.ui.navigation.BottomBarScreen
import com.soft.bookteria.ui.screens.categories.composables.CategoriesScreen
import com.soft.bookteria.ui.screens.home.composables.HomeScreenContainer
import com.soft.bookteria.ui.screens.library.composables.MyLibraryScreen
import com.soft.bookteria.ui.screens.settings.composables.SettingsScreen

@Composable
fun MainScreen(
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
            
            // Tab Categories
            composable(route = BottomBarScreen.Categories.route) {
                CategoriesScreen(navController = navController)
            }
            
            // Tab Library
            composable(route = BottomBarScreen.Library.route) {
                MyLibraryScreen(navController = navController)
            }
            
            // Tab Settings
            composable(route = BottomBarScreen.Settings.route) {
                SettingsScreen(navController = navController)
            }
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
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.height(64.dp)
    ) {
        screens.forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = screen.icon),
                        contentDescription = stringResource(id = screen.title),
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = { 
                    androidx.compose.material3.Text(
                        text = stringResource(id = screen.title)
                    ) 
                },
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
