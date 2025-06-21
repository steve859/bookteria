package com.soft.bookteria

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.soft.bookteria.ui.navigation.BottomBarScreen
import com.soft.bookteria.ui.navigation.NavigationGraph
import com.soft.bookteria.ui.navigation.NavigationScreens
import com.soft.bookteria.ui.screens.main.MainScreen
import com.soft.bookteria.ui.theme.BookteriaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        installSpashScreen().setKeepOnScreenCondition{
//
//        }
        enableEdgeToEdge()
        setContent {
            BookteriaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // Start with welcome screen
                    NavigationGraph(
                        startDestination = NavigationScreens.WelcomeScreen.route,
                        navController = navController
                    )
                }
            }
        }
    }
}