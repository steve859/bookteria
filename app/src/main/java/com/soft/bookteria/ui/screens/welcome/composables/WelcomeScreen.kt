package com.soft.bookteria.ui.screens.welcome.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavController
import coil.compose.AsyncImage
//import com.soft.bookteria.ui.theme.poppinsFont
import com.soft.bookteria.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soft.bookteria.helpers.weakHapticFeedback
import com.soft.bookteria.ui.navigation.BottomBarScreen
import com.soft.bookteria.ui.screens.welcome.viewmodels.WelcomeViewModel
import com.soft.bookteria.ui.theme.loraFont
import com.soft.bookteria.ui.theme.ptSerifFont
import java.nio.file.WatchEvent
import com.soft.bookteria.ui.navigation.NavigationScreens

@Composable
fun WelcomeScreen(navController: NavController) {
    val viewModel: WelcomeViewModel = hiltViewModel()
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize()
    ){
        AsyncImage(
            model = R.drawable.book_welcome_screen,
            contentDescription = null,
            alpha = 0.3f,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier.fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black
                        )
                        , startY = 8f
                    )
                )
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ){
            SelectionUI (
                onClicked = {
                    navController.navigate("main_screen") {
                        popUpTo(NavigationScreens.WelcomeScreen.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SelectionUI(
    onClicked: () -> Unit
){
    val view = LocalView.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
        Text(
            text = stringResource(id = R.string.welcome_text),
            fontFamily = ptSerifFont,
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Spacer(modifier = Modifier.height(30.dp))
        OutlinedButton(
            onClick = {
                view.weakHapticFeedback()
                onClicked()
            },
            modifier = Modifier
                .width(200.dp)
                .height(50.dp)
        ) {
            Text(
                text = stringResource(id = R.string.get_started),
                fontFamily = loraFont,
                color = Color.Black
            )
        
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomePreview(){
    SelectionUI(
        onClicked = {}
    )
}

