package com.soft.bookteria

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.soft.bookteria.ui.screens.welcome.composables.WelcomeScreen
import com.soft.bookteria.ui.theme.BookteriaTheme


class MainActivity : AppCompatActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        installSpashScreen().setKeepOnScreenCondition{
//
//        }
        enableEdgeToEdge()
        setContent{
            BookteriaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ){
                
                }
            }
        }
    }
}