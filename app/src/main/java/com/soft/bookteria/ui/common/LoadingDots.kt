package com.soft.bookteria.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import com.soft.bookteria.R

/**
 * Composable hiển thị ba dấu chấm nhấp nhô (bouncing) để báo hiệu loading.
 */
@Composable
fun LoadingDots(
    modifier: Modifier = Modifier,
    dotColor: Color = MaterialTheme.colorScheme.primary,
    dotSize: Dp = 12.dp,
    spacing: Dp = 8.dp
) {
    val animations = remember {
        List(3) { Animatable(0.5f) }
    }
    animations.forEachIndexed { index, animatable ->
        LaunchedEffect(animatable) {
            // Độ trễ khởi đầu để tạo hiệu ứng thứ tự (index * 150ms)
            delay(index * 150L)
            animatable.animateTo(
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        animations.forEach { animatable ->
            Box(
                Modifier
                    .size(dotSize)
                    .scale(animatable.value)
                    .background(color = dotColor, shape = CircleShape)
            )
        }
    }
}




/**
 * Preview cho LoadingDots và ErrorScreen
 */
@Composable
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
fun PreviewLoadingAndError() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LoadingDots(
            dotColor = Color(0xFF6200EE),
            dotSize = 16.dp,
            spacing = 12.dp
        )
        
        ErrorScreen(
            errorMessage = "Không thể kết nối mạng",
            onRetry = { /* giả lập hành động thử lại */ }
        )
    }
}
