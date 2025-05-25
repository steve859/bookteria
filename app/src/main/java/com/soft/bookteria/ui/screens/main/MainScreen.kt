package com.soft.bookteria.ui.screens.main

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.soft.bookteria.ui.navigation.BottomBarScreen

@Composable
fun MainScreen(
    intent: Intent,
    startDestination: String
    //networkObserver: NetworkObserver.status
){

}

@Composable
private fun BottomBar(navController: NavController){
    val screens = listOf(
        BottomBarScreen.Home,
        BottomBarScreen.Categories,
        BottomBarScreen.Library,
        BottomBarScreen.Settings
    )
}