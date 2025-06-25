package com.soft.bookteria.ui.screens.reader

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.soft.bookteria.ui.screens.reader.viewmodel.ReaderDetailViewModel
import com.soft.bookteria.ui.screens.reader.viewmodel.ReaderDetailScreenState
import com.soft.bookteria.ui.theme.ptSerifFont

@Composable
fun ReaderDetailScreen(
    libraryObjectId: String,
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: ReaderDetailViewModel = hiltViewModel()
    val state = viewModel.state

    LaunchedEffect(libraryObjectId) {
        viewModel.loadEbookData(libraryObjectId)
    }

    Crossfade(targetState = state.isLoading, label = "ReaderDetailLoadingCrossFade") { isLoading ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 65.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            if (state.error != null) {
                LaunchedEffect(state.error) {
                    navController.navigateUp()
                }
            } else {
                ReaderDetailScaffold(
                    libraryObjectId = libraryObjectId,
                    state = state,
                    navController = navController
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderDetailScaffold(
    libraryObjectId: String,
    state: ReaderDetailScreenState,
    navController: NavController
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Reader Detail",
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
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val intent = Intent(context, com.soft.bookteria.ui.screens.reader.ReaderActivity::class.java)
                    intent.putExtra(com.soft.bookteria.ui.screens.reader.ReaderActivity.EXTRA_LIBRARY_OBJECT_ID, libraryObjectId.toInt())
                    context.startActivity(intent)
                },
                modifier = Modifier.padding(end = 10.dp, bottom = 8.dp)
            ) {
                Text(
                    text = if (state.hasProgressSaved) "Continue Reading" else "Start Reading"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
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
                            text = state.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ptSerifFont,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = state.authors,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontFamily = ptSerifFont
                        )
                        
                        if (state.hasProgressSaved) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Progress: ${state.progressPercent}%",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = ptSerifFont
                            )
                        }
                    }
                }
            }

            Divider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
            )

            // Chapters list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(state.chapters) { chapterTitle ->
                    ChapterItem(
                        chapterTitle = chapterTitle,
                        onClick = {
                            val intent = Intent(context, com.soft.bookteria.ui.screens.reader.ReaderActivity::class.java)
                            intent.putExtra(com.soft.bookteria.ui.screens.reader.ReaderActivity.EXTRA_LIBRARY_OBJECT_ID, libraryObjectId.toInt())
                            intent.putExtra(com.soft.bookteria.ui.screens.reader.ReaderActivity.EXTRA_CHAPTER_INDEX, state.chapters.indexOf(chapterTitle))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterItem(
    chapterTitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        ),
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Text(
                modifier = Modifier
                    .weight(3f)
                    .padding(start = 12.dp),
                text = chapterTitle,
                fontFamily = ptSerifFont,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Icon(
                modifier = Modifier
                    .size(15.dp)
                    .weight(0.4f),
                painter = painterResource(id = R.drawable.ic_library),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
} 