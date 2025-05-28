package com.soft.bookteria.ui.screens.home.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import com.soft.bookteria.ui.screens.home.viewmodels.HomeViewModel
import kotlinx.coroutines.delay

@Composable
fun HomeSceen(
    navController: NavController
){
    val viewModel: HomeViewModel
    LaunchedEffect(key1 = true) {
        delay(200)
        //viewModel.loadNextItems()
    }
}