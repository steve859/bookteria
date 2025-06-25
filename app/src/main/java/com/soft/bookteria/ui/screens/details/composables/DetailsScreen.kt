package com.soft.bookteria.ui.screens.details.composables

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

import coil.compose.AsyncImage
import com.soft.bookteria.R
import com.soft.bookteria.ui.screens.reader.ReaderActivity
import com.soft.bookteria.ui.screens.details.viewmodel.DetailsViewModel
import com.soft.bookteria.ui.theme.ptSerifFont
import com.soft.bookteria.ui.navigation.NavigationScreens
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    navController: NavController,
    bookId: String
) {
    val viewModel: DetailsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val isDownloaded by viewModel.isDownloaded.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load book details when entering screen
    LaunchedEffect(bookId) {
        Log.d("DetailScreenLog", "LaunchedEffect: loadBookDetails + checkIfDownloaded for bookId=$bookId")
        viewModel.loadBookDetails(bookId.toLong())
        viewModel.checkIfDownloaded(bookId.toLong())
    }
    
    // Show download messages and clear them after delay
    LaunchedEffect(uiState.downloadMessage) {
        uiState.downloadMessage?.let { message ->
            Log.d("DetailScreenLog", "Snackbar downloadMessage: $message")
            snackbarHostState.showSnackbar(message)
            // Clear the message after showing
            delay(100)
            viewModel.clearDownloadMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Details", fontWeight = FontWeight.Bold, fontFamily = ptSerifFont) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.error ?: "Unknown error", color = Color.Red)
            }
        } else {
            val book = uiState.bookCollection.books.firstOrNull()
            if (book == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Book not found", color = Color.Red)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .background(MaterialTheme.colorScheme.background),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    AsyncImage(
                        model = book.formats.imagejpeg.takeIf { !it.isNullOrBlank() },
                        contentDescription = "Book cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 160.dp, height = 220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Gray.copy(alpha = 0.2f)),
                        error = painterResource(id = R.drawable.ic_placeholder),
                        placeholder = painterResource(id = R.drawable.ic_placeholder)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = book.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = TextAlign.Justify
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "by " + book.authors.joinToString(", ") { it.name },
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = TextAlign.Justify
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Download count: ${book.downloadCount}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Summary:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Start).padding(start = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (book.summaries.isNotEmpty()) {
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            book.summaries.forEach { summary ->
                                Text(
                                    text = summary,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                    textAlign = TextAlign.Justify
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No summary available.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 24.dp),
                            textAlign = TextAlign.Justify
                        )
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                    Button(
                        onClick = {
                            Log.d("DetailScreenLog", "Button clicked. isDownloaded=$isDownloaded, bookId=${book.id}")
                            if (isDownloaded) {
                                val libraryObjectId = viewModel.getLibraryObjectId(book.id)
                                Log.d("DetailScreenLog", "Try open reader. getLibraryObjectId(${book.id}) = $libraryObjectId")
                                if (libraryObjectId != null) {
                                    // Điều hướng sang ReaderScreen với đúng id
                                    navController.navigate(
                                        com.soft.bookteria.ui.navigation.NavigationScreens.ReaderScreen.withBookId(libraryObjectId.toString())
                                    )
                                } else {
                                    Log.e("DetailScreenLog", "libraryObjectId is null! Cannot open reader.")
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Cannot find downloaded book in library. Please try again.")
                                    }
                                }
                            } else {
                                Log.d("DetailScreenLog", "Start download for bookId=${book.id}")
                                viewModel.downloadBook(
                                    book,
                                    onResult = { success, message ->
                                        Log.d("DetailScreenLog", "Download result: success=$success, message=$message")
                                        // The ViewModel already updates isDownloaded state
                                    },
                                    downloadProgressListener = { progress, status ->
                                        Log.d("DetailScreenLog", "Download progress: $progress, status=$status")
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.isDownloading
                    ) {
                        if (uiState.isDownloading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Downloading...")
                            }
                        } else {
                            Text(text = if (isDownloaded) "Read" else "Download")
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

