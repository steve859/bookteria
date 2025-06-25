package com.soft.bookteria.ui.screens.reader

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.soft.bookteria.ui.screens.reader.viewmodel.ReaderViewModel
import com.soft.bookteria.ui.theme.BookteriaTheme
import com.soft.bookteria.ui.theme.ptSerifFont
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.ui.tooling.preview.Preview

@AndroidEntryPoint
class ReaderActivity : AppCompatActivity() {
    
    private val viewModel: ReaderViewModel by viewModels()
    
    companion object {
        const val EXTRA_LIBRARY_OBJECT_ID = "library_object_id"
        const val EXTRA_CHAPTER_INDEX = "chapter_index"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupFullscreenMode()
        
        val libraryObjectId = intent.getIntExtra(EXTRA_LIBRARY_OBJECT_ID, -1)
        val chapterIndex = intent.getIntExtra(EXTRA_CHAPTER_INDEX, 0)
        
        android.util.Log.d("ReaderActivity", "libraryObjectId: $libraryObjectId, chapterIndex: $chapterIndex")
        
        if (libraryObjectId == -1) {
            android.util.Log.e("ReaderActivity", "Invalid libraryObjectId")
            finish()
            return
        }
        
        setContent {
            BookteriaTheme {
                ReaderActivityContent(
                    libraryObjectId = libraryObjectId,
                    chapterIndex = chapterIndex,
                    onBackPressed = { finish() }
                )
            }
        }
    }
    
    private fun setupFullscreenMode() {
        // Fullscreen mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.hide(WindowInsetsCompat.Type.displayCutout())
        
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderActivityContent(
    libraryObjectId: Int,
    chapterIndex: Int,
    onBackPressed: () -> Unit
) {
    val viewModel: ReaderViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()
    
    LaunchedEffect(libraryObjectId) {
        viewModel.loadBook(libraryObjectId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.libraryObject?.title ?: "Reader",
                        fontWeight = FontWeight.Bold,
                        fontFamily = ptSerifFont,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Show chapter list */ }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Chapters"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            // Reader content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                state = lazyListState,
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    Text(
                        text = "Chapter ${chapterIndex + 1}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ptSerifFont,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                items(20) { index ->
                    ChapterContentItem(
                        content = "This is paragraph ${index + 1} of chapter ${chapterIndex + 1}. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur."
                    )
                }
            }
        }
    }
}

@Composable
fun ChapterContentItem(
    content: String
) {
    Text(
        text = content,
        fontSize = 16.sp,
        fontFamily = ptSerifFont,
        color = MaterialTheme.colorScheme.onBackground,
        lineHeight = 24.sp,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Preview
@Composable
fun ReaderActivityPreview() {
    BookteriaTheme {
        ReaderActivityContent(
            libraryObjectId = 1,
            chapterIndex = 0,
            onBackPressed = {}
        )
    }
} 