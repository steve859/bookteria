package com.soft.bookteria.ui.screens.details.composables

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.soft.bookteria.R
import com.soft.bookteria.ui.screens.details.viewmodel.DetailsViewModel
import com.soft.bookteria.ui.theme.ptSerifFont
import kotlinx.coroutines.launch

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

    // Load book details when entering screen
    LaunchedEffect(bookId) {
        viewModel.loadBookDetails(bookId.toLong())
        viewModel.checkIfDownloaded(bookId.toLong())
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
        }
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
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "by " + book.authors.joinToString(", ") { it.name },
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 16.dp)
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
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No summary available.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                    Button(
                        onClick = {
                            if (isDownloaded) {
                                // TODO: Open reader screen
                            } else {
                                viewModel.downloadBook(
                                    book,
                                    onResult = { success, message ->
                                        // Optional: Hiển thị thông báo tải về thành công/thất bại
                                        // Ví dụ: Toast.makeText(context, message ?: "", Toast.LENGTH_SHORT).show()
                                    },
                                    downloadProgressListener = { progress, status ->
                                        // Optional: Cập nhật UI tiến trình tải về nếu muốn
                                        // Log.d("Download", "progress: $progress, status: $status")
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = if (isDownloaded) "Read" else "Download")
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

