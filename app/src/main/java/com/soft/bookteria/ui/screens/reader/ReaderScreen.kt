package com.soft.bookteria.ui.screens.reader

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.soft.bookteria.R
import com.soft.bookteria.ui.screens.reader.ReaderActivity
import com.soft.bookteria.ui.screens.reader.viewmodel.ReaderViewModel
import com.soft.bookteria.ui.theme.BookteriaTheme
import com.soft.bookteria.ui.theme.ptSerifFont
import com.soft.bookteria.database.library.LibraryObject
import com.soft.bookteria.database.progress.ProgressData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    navController: NavController,
    libraryObjectId: String
) {
    val viewModel: ReaderViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Load book data when the composable first loads
    LaunchedEffect(libraryObjectId) {
        viewModel.loadBook(libraryObjectId.toInt())
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Reader",
                        fontWeight = FontWeight.Bold,
                        fontFamily = ptSerifFont
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Main content
            if (uiState.isLoading) {
                LoadingScreen()
            } else if (uiState.error != null) {
                ErrorScreen(uiState.error) {
                    viewModel.loadBook(libraryObjectId.toInt())
                }
            } else {
                // Book info is loaded, show UI
                BookInfoScreen(
                    uiState = uiState,
                    onStartReading = {
                        val intent = Intent(context, ReaderActivity::class.java).apply {
                            putExtra(ReaderActivity.EXTRA_LIBRARY_OBJECT_ID, libraryObjectId.toInt())
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(errorMessage: String?, onRetry: () -> Unit) {
    val message = errorMessage ?: "Unknown error"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
fun BookInfoScreen(
    uiState: com.soft.bookteria.ui.screens.reader.viewmodel.ReaderUIState,
    onStartReading: () -> Unit
) {
    val libraryObject = uiState.libraryObject ?: return
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            // Book cover and info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Book cover
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("BOOK")
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Book details
                Column {
                    Text(
                        text = libraryObject.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = ptSerifFont
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = libraryObject.authors,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (uiState.bookInfo != null) {
                        Text(
                            text = "${uiState.bookInfo.totalChapters} chapters",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Reading progress
                    if (uiState.hasProgressSaved && uiState.progressData != null) {
                        val progress = uiState.progressData
                        Text(
                            text = "Last read: Chapter ${progress.lastChapterIndex + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // File information
            Text(
                text = "File Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "File: ${libraryObject.filePath.substringAfterLast('/')}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Start reading button
            Button(
                onClick = onStartReading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (uiState.hasProgressSaved) "Continue Reading" else "Start Reading",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Preview
@Composable
fun ReaderScreenPreview() {
    BookteriaTheme {
        ReaderScreen(
            navController = rememberNavController(),
            libraryObjectId = "1"
        )
    }
}