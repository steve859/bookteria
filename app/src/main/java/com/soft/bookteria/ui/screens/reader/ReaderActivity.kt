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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.soft.bookteria.R
import com.soft.bookteria.ui.screens.reader.viewmodel.ReaderViewModel
import com.soft.bookteria.ui.theme.BookteriaTheme
import com.soft.bookteria.ui.theme.ptSerifFont
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

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
    
    override fun onDestroy() {
        // Clean up resources when the activity is destroyed
        viewModel.cleanupResources()
        super.onDestroy()
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
    val coroutineScope = rememberCoroutineScope()
    var showChapterSelector by remember { mutableStateOf(false) }
    var chapterList by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    
    // Load book and set initial chapter
    LaunchedEffect(libraryObjectId) {
        viewModel.loadBook(libraryObjectId)
        // When book is loaded, specifically load the requested chapter
        if (!uiState.isLoading && uiState.bookInfo != null) {
            viewModel.loadChapter(chapterIndex)
        }
    }
    
    // Fetch chapter list when book is loaded
    LaunchedEffect(uiState.bookInfo) {
        uiState.bookInfo?.let {
            chapterList = viewModel.getChapterList()
        }
    }
    
    // Scroll to saved position when chapter content is loaded
    LaunchedEffect(uiState.currentChapter) {
        uiState.progressData?.let { progress ->
            if (progress.lastChapterIndex == uiState.currentChapterIndex && progress.lastChapterOffset > 0) {
                lazyListState.scrollToItem(0, progress.lastChapterOffset)
            }
        }
    }
    
    // Save position when scrolling
    LaunchedEffect(lazyListState) {
        // Track scroll position and save progress
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .collect { offset ->
                // Save position every time the user scrolls significantly
                uiState.libraryObject?.id?.let { id ->
                    viewModel.updateProgress(id, uiState.currentChapterIndex, offset)
                }
            }
    }
    
    // Chapter selector dialog
    if (showChapterSelector) {
        AlertDialog(
            onDismissRequest = { showChapterSelector = false },
            title = { Text("Chapters") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(chapterList) { chapterItem ->
                        val (index, title) = chapterItem
                        Text(
                            text = title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        viewModel.loadChapter(index)
                                        showChapterSelector = false
                                    }
                                }
                                .padding(vertical = 12.dp),
                            fontWeight = if (index == uiState.currentChapterIndex) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChapterSelector = false }) {
                    Text("Close")
                }
            }
        )
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
                    IconButton(onClick = { showChapterSelector = true }) {
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
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading book...")
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_error),
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = { onBackPressed() }) {
                        Text("Go Back")
                    }
                }
            }
        } else if (uiState.bookInfo == null || uiState.currentChapter == null) {
            // Still loading book content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading book content...")
            }
        } else {
            // Reader content
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Chapter title
                Text(
                    text = uiState.currentChapter?.title ?: "Chapter",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                // Chapter content with HTML support
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item {
                        // Use AndroidView with WebView for HTML content
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { context ->
                                android.webkit.WebView(context).apply {
                                    settings.javaScriptEnabled = false
                                    settings.loadsImagesAutomatically = true
                                    settings.defaultFontSize = 16
                                    settings.defaultTextEncodingName = "UTF-8"
                                    
                                    // CSS to make content look good
                                    val css = """
                                        <style>
                                            body {
                                                font-family: serif;
                                                font-size: 16px;
                                                line-height: 1.6;
                                                padding: 0;
                                                margin: 0;
                                                color: #333;
                                            }
                                            img {
                                                max-width: 100%;
                                                height: auto;
                                                display: block;
                                                margin: 10px auto;
                                            }
                                            h1, h2, h3 {
                                                margin-top: 20px;
                                                margin-bottom: 10px;
                                            }
                                            p {
                                                margin-bottom: 16px;
                                            }
                                        </style>
                                    """.trimIndent()
                                    
                                    // Create a complete HTML document
                                    val htmlContent = """
                                        <!DOCTYPE html>
                                        <html>
                                        <head>
                                            <meta charset="UTF-8">
                                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                            $css
                                        </head>
                                        <body>
                                            ${uiState.currentChapter?.content ?: ""}
                                        </body>
                                        </html>
                                    """.trimIndent()
                                    
                                    loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )
                    }
                }
                
                // Navigation controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { viewModel.previousChapter() },
                        enabled = uiState.currentChapterIndex > 0
                    ) {
                        Text("Previous")
                    }
                    
                    Text(
                        text = "${uiState.currentChapterIndex + 1}/${uiState.bookInfo?.totalChapters ?: 1}",
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    
                    Button(
                        onClick = { viewModel.nextChapter() },
                        enabled = uiState.currentChapterIndex < (uiState.bookInfo?.totalChapters ?: 1) - 1
                    ) {
                        Text("Next")
                    }
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