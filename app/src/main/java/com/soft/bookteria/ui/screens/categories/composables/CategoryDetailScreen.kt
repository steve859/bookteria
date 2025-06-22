package com.soft.bookteria.ui.screens.categories.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.soft.bookteria.R
import com.soft.bookteria.ui.common.BookItemCard
import com.soft.bookteria.ui.common.LoadingDots
import com.soft.bookteria.ui.navigation.NavigationScreens
import com.soft.bookteria.ui.screens.categories.viewmodel.CategoriesViewModel
import com.soft.bookteria.ui.theme.ptSerifFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    navController: NavController,
    category: String,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val listState = rememberLazyListState()
    
    // Load books when screen is first displayed
    LaunchedEffect(category) {
        viewModel.loadBookByCategory(category)
    }
    
    // Detect when user scrolls to the end
    val shouldStartPaginating by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= state.books.size - 1 && !state.endReached && !state.isLoading
        }
    }
    
    // Load more items when needed
    LaunchedEffect(shouldStartPaginating) {
        if (shouldStartPaginating) {
            viewModel.loadNextItems()
        }
    }
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = category.replaceFirstChar { it.uppercase() },
                        fontFamily = ptSerifFont,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.error != null -> {
                    // Show error state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.error_no_connection),
                            fontFamily = ptSerifFont,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.error,
                            fontFamily = ptSerifFont,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.reloadItems() }
                        ) {
                            Text(stringResource(id = R.string.error_retry))
                        }
                    }
                }
                
                state.books.isEmpty() && state.isLoading -> {
                    // Show loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingDots()
                    }
                }
                
                state.books.isEmpty() && !state.isLoading -> {
                    // Show empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No books found",
                            fontFamily = ptSerifFont,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try selecting a different category",
                            fontFamily = ptSerifFont,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                else -> {
                    // Show books list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "${state.books.size} books found",
                                fontFamily = ptSerifFont,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        items(state.books) { book ->
                            BookItemCard(
                                title = book.title,
                                author = book.authors.firstOrNull()?.name ?: "Unknown Author",
                                subjects = book.subjects.take(3).joinToString(", "),
                                coverImageUrl = book.formats.imagejpeg ?: book.formats.applicationepubzip,
                                loadingEffect = false,
                                onClick = {
                                    navController.navigate(
                                        NavigationScreens.BookDetailScreen.withBookId(book.id.toString())
                                    )
                                }
                            )
                        }
                        
                        // Show loading indicator at the bottom when loading more
                        if (state.isLoading && state.books.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LoadingDots()
                                }
                            }
                        }
                        
                        // Show end reached message
                        if (state.endReached && state.books.isNotEmpty()) {
                            item {
                                Text(
                                    text = "No more books to load",
                                    fontFamily = ptSerifFont,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 