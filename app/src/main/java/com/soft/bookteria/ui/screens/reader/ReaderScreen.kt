package com.soft.bookteria.ui.screens.reader

import android.content.Context
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import com.soft.bookteria.ui.theme.ptSerifFont
import com.soft.bookteria.database.library.LibraryObject
import com.soft.bookteria.database.progress.ProgressData
import kotlin.Int

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    navController: NavController,
    libraryObjectId: String
) {
    val viewModel: ReaderViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_error),
                        contentDescription = "Error",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Book file may have been deleted or corrupted",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            viewModel.loadBook(libraryObjectId.toInt())
                        }
                    ) {
                        Text("Retry")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val coroutineScope = rememberCoroutineScope()
                    
                    Button(
                        onClick = {
                            // Sử dụng coroutine để lấy bookId
                            coroutineScope.launch {
                                // Điều hướng tới màn hình chi tiết sách để tải lại
                                val bookId = viewModel.getBookIdFromLibraryIdSuspend(libraryObjectId.toInt())
                                if (bookId != null) {
                                    navController.navigate(
                                        com.soft.bookteria.ui.navigation.NavigationScreens.BookDetailScreen.withBookId(bookId.toString())
                                    ) {
                                        popUpTo(navController.graph.startDestinationId)
                                    }
                                } else {
                                    navController.navigateUp()
                                }
                            }
                        }
                    ) {
                        Text("Download Again")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = {
                            navController.navigateUp()
                        }
                    ) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            val libraryObject = uiState.libraryObject
            if (libraryObject == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Book not found",
                        color = Color.Red,
                        fontSize = 16.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Book cover and info
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = R.drawable.ic_placeholder,
                                contentDescription = "Book cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(80.dp, 100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Gray.copy(alpha = 0.2f))
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = libraryObject.title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ptSerifFont,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = libraryObject.authors,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontFamily = ptSerifFont
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "File: ${libraryObject.getFileName()}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontFamily = ptSerifFont
                                )
                            }
                        }
                    }
                    
                    // Start reading button
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.hasProgressSaved)
                                MaterialTheme.colorScheme.secondaryContainer
                            else
                                MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            val intent = Intent(context, ReaderActivity::class.java).apply {
                                putExtra(ReaderActivity.EXTRA_LIBRARY_OBJECT_ID, libraryObject.id)
                                if (uiState.hasProgressSaved && uiState.progressData != null) {
                                    putExtra(ReaderActivity.EXTRA_CHAPTER_INDEX, uiState.progressData!!.lastChapterIndex)
                                }
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (uiState.hasProgressSaved) "Continue Reading" else "Start Reading",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = ptSerifFont,
                                color = if (uiState.hasProgressSaved)
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                else
                                    MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    // Progress info
                    if (uiState.hasProgressSaved && uiState.progressData != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Reading Progress",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = ptSerifFont,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Last read: Chapter ${uiState.progressData!!.lastChapterIndex + 1}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontFamily = ptSerifFont
                                )
                                
                                Text(
                                    text = "Progress: ${uiState.progressData!!.getProgressPercentage(5)}%",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontFamily = ptSerifFont
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Chapters list (placeholder)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(5) { index ->
                            ChapterItem(
                                title = "Chapter ${index + 1}",
                                onClick = {
                                    val intent = Intent(context, ReaderActivity::class.java).apply {
                                        putExtra(ReaderActivity.EXTRA_LIBRARY_OBJECT_ID, libraryObject.id)
                                        putExtra(ReaderActivity.EXTRA_CHAPTER_INDEX, index)
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterItem(
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = ptSerifFont,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Read chapter",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReaderScreenPreview() {
    val navController = rememberNavController()
    ReaderScreen(navController, "1")
}

@Preview(showBackground = true, name = "Reader Screen Content Preview")
@Composable
fun ReaderScreenContentPreview() {
    androidx.compose.material3.MaterialTheme {
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Book cover and info
            androidx.compose.material3.Card(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.ic_placeholder),
                        contentDescription = "Book cover",
                        modifier = androidx.compose.ui.Modifier
                            .size(80.dp, 100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray.copy(alpha = 0.2f))
                    )
                    
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(16.dp))
                    
                    androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                        androidx.compose.material3.Text(
                            text = "Sample Book Title",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ptSerifFont,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                        
                        androidx.compose.material3.Text(
                            text = "Sample Author",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontFamily = ptSerifFont
                        )
                        
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                        
                        androidx.compose.material3.Text(
                            text = "File: sample_book.epub",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontFamily = ptSerifFont
                        )
                    }
                }
            }
            
            // Start reading button
            androidx.compose.material3.Card(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = "Start Reading",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ptSerifFont,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Progress info
            androidx.compose.material3.Card(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    androidx.compose.material3.Text(
                        text = "Reading Progress",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ptSerifFont,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                    
                    androidx.compose.material3.Text(
                        text = "Last read: Chapter 3",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontFamily = ptSerifFont
                    )
                    
                    androidx.compose.material3.Text(
                        text = "Progress: 60.00%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontFamily = ptSerifFont
                    )
                }
            }
            
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
            
            // Sample chapters
            androidx.compose.foundation.layout.Column {
                repeat(3) { index ->
                    ChapterItem(
                        title = "Chapter ${index + 1}",
                        onClick = {}
                    )
                }
            }
        }
    }
}