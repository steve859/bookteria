package com.soft.bookteria.ui.screens.home.composables

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.soft.bookteria.R
import com.soft.bookteria.api.BookApi
import com.soft.bookteria.api.models.Author
import com.soft.bookteria.api.models.Book
import com.soft.bookteria.api.models.BookCollection
import com.soft.bookteria.api.models.Formats
import com.soft.bookteria.helpers.NetworkObserver
import com.soft.bookteria.ui.common.LoadingDots
import com.soft.bookteria.ui.common.ErrorScreen
import com.soft.bookteria.ui.navigation.BottomBarScreen
import com.soft.bookteria.ui.navigation.NavigationScreens
import com.soft.bookteria.ui.screens.home.viewmodels.AllBooksStates
import com.soft.bookteria.ui.screens.home.viewmodels.HomeViewModel
import com.soft.bookteria.ui.screens.home.viewmodels.SearchBarStates
import com.soft.bookteria.ui.screens.home.viewmodels.UserAction
import com.soft.bookteria.ui.theme.ptSerifFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * inject data from viewmodel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    networkStatus: NetworkObserver.Status
) {
    val viewModel: HomeViewModel = hiltViewModel()

    // Pass viewmodel to container
    HomeScreenContainer(
        viewModel = viewModel,
        networkStatus = networkStatus,
        navController = navController,
        backState = remember { mutableStateOf(false) }
    )
}


@Composable
public fun HomeScreenContainer(
    viewModel: HomeViewModel,
    networkStatus: NetworkObserver.Status,
    navController: NavController,
    backState: MutableState<Boolean>
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    
    // 6. Lấy state từ ViewModel
    val searchState: SearchBarStates by remember { viewModel::searchBarState }
    val allBooksState: AllBooksStates by remember { viewModel::allBooksState }

    // Tải sách khi màn hình được hiển thị lần đầu
    LaunchedEffect(key1 = true) {
        if (allBooksState.items.isEmpty()) {
            viewModel.loadNextItems()
        }
    }
    
    //dong search bar truoc khi tat app
    BackHandler(enabled = backState.value) {
        if (viewModel.searchBarState.isVisible) { // mo searchbar co text -> xoa text
            if (viewModel.searchBarState.text.isNotEmpty()) {
                viewModel.onAction(UserAction.TextFieldInput("", networkStatus))
            } else {
                viewModel.onAction(UserAction.CloseClicked)
            }
        }
    }
    
    // chuyen qua tab khac -> dong searchbar
    val backStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(backStackEntry) {
        if (backStackEntry?.destination?.route != BottomBarScreen.Home.route) {
            viewModel.onAction(UserAction.TextFieldInput("", networkStatus))
            viewModel.onAction(UserAction.CloseClicked)
        }
    }
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                Crossfade(
                    targetState = searchState.isVisible,
                    animationSpec = tween(durationMillis = 200),
                    modifier = Modifier.animateContentSize(),
                    label = "home_search_crossfade"
                ) { isSearchOpen ->
                    if (isSearchOpen) {
                        // searchbar mo
                        OutlinedTextField(
                            value = searchState.text,
                            onValueChange = { newText ->
                                viewModel.onAction(
                                    UserAction.TextFieldInput(newText, networkStatus)
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "Search Book...",
                                    fontFamily = ptSerifFont,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Icon",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (searchState.text.isNotBlank()) {
                                        viewModel.onAction(
                                            UserAction.TextFieldInput("", networkStatus)
                                        )
                                    } else {
                                        viewModel.onAction(UserAction.CloseClicked)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Close Search",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                cursorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(24.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(remember { FocusRequester() })
                        )
                        backState.value = true
                        
                    } else {
                        // searchbar dong -> hien thi title + icon search
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            IconButton(onClick = {
                                viewModel.onAction(UserAction.SearchClicked)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Open Searchbar",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                        backState.value = false
                    }
                }
                
                Divider(
                    thickness = 1.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    ) { paddingValues ->
        // main chua search -> AllBooksList,
        // khong search -> SearchResultsList
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            if (searchState.text.isBlank()) {
                AllBooksList(
                    allBooksState = allBooksState,
                    networkStatus = networkStatus,
                    navController = navController,
                    onRetryClicked = { viewModel.reloadItems() },
                    onLoadNextItems = { viewModel.loadNextItems() }
                )
            } else {
                SearchResultsList(
                    searchBarState = searchState,
                    navController = navController
                )
            }
        }
    }
}


@Composable
private fun AllBooksList(
    allBooksState: AllBooksStates,
    networkStatus: NetworkObserver.Status,
    navController: NavController,
    onRetryClicked: () -> Unit,
    onLoadNextItems: () -> Unit
) {
    // hien thi shimmer loader khi load trang dau
    AnimatedVisibility(
        visible = (allBooksState.page == 1L && allBooksState.isLoading),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingDots()
        }
    }
    
    // loi -> NetworkError
    AnimatedVisibility(
        visible = (!allBooksState.isLoading && allBooksState.error != null),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        ErrorScreen(onRetry = { onRetryClicked() }) // Can Sua
        
        
    }
    
    AnimatedVisibility(
        visible = (!allBooksState.isLoading || allBooksState.error == null),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(allBooksState.items) { index, book ->
                // tu goi loadNextItems() khi keo xuong
                if (networkStatus == NetworkObserver.Status.Avaiable
                    && index >= allBooksState.items.size - 1
                    && !allBooksState.endReached
                    && !allBooksState.isLoading
                ) {
                    onLoadNextItems()
                }
                
                BookRow(book = book) {
                    navController.navigate(
                        NavigationScreens.BookDetailScreen.withBookId(book.id.toString())
                    )
                }
            }
            
            // loading dots khi load trang
            item {
                if (allBooksState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingDots()
                    }
                }
            }
        }
    }
}


@Composable
private fun SearchResultsList(
    searchBarState: SearchBarStates,
    navController: NavController
) {
    // dang tim kiem -> LoadingDots
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (searchBarState.isSearching) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingDots()
                }
            }
        }
        
        items(searchBarState.returnedBookList) { book ->
            BookRow(book = book) {
                navController.navigate(
                    NavigationScreens.BookDetailScreen.withBookId(book.id.toString())
                )
            }
        }
    }
}


@Composable
private fun BookRow(
    book: Book,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            
            AsyncImage(
                model = book.formats.imagejpeg,
                contentDescription = "Book cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 80.dp, height = 100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.2f))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = book.authors.joinToString(", ") { it.name },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
            }
        }
    }
}


class FakeBookApi(context: Context) : BookApi(context) {
    // seed data
    private val sampleBooks = listOf(
        Book(
            id = 100,
            title = "Lap trinh huong doi tuong",
            authors = listOf(com.soft.bookteria.api.models.Author("Tran Anh Dung")),
            subjects = listOf("Coding"),
            bookshelves = emptyList(),
            copyright = false,
            downloadCount = 0,
            languages = listOf("en"),
            mediaType = "Text",
            formats = com.soft.bookteria.api.models.Formats(
                imagejpeg = "https://placehold.co/80x100.png"
            )
        ),
        Book(
            id = 101,
            title = "NMLT",
            authors = listOf(com.soft.bookteria.api.models.Author("Mai Tuan Kiet")),
            subjects = listOf("Coding"),
            bookshelves = emptyList(),
            copyright = false,
            downloadCount = 0,
            languages = listOf("en"),
            mediaType = "Text",
            formats = com.soft.bookteria.api.models.Formats(
                imagejpeg = "https://placehold.co/80x100.png"
            )
        )
    )
    
    override suspend fun getAllBooks(page: Long): Result<BookCollection> {
        
        withContext(Dispatchers.IO) { delay(200) }
        return Result.success(
            BookCollection(
                books = sampleBooks,
            )
        )
    }
    
    override suspend fun searchBooks(query: String): Result<BookCollection> {
        withContext(Dispatchers.IO) { delay(200) }
        return if (query.isBlank()) {
            Result.success(BookCollection(books = emptyList()))
        } else {
            val matched = sampleBooks.filter { it.title.contains(query, ignoreCase = true) }
            Result.success(BookCollection(books = matched))
        }
    }
    
    override suspend fun getBookByCategory(category: String, page: Long): Result<BookCollection> {
        withContext(Dispatchers.IO) { delay(200) }
        return Result.success(BookCollection(books = emptyList()))
    }
}

class FakeHomeVM(context: Context) : HomeViewModel(bookAPI = FakeBookApi(context)) {
    init {
        allBooksState = allBooksState.copy(
            isLoading = false,
            items = listOf(
                Book(
                    id = 100,
                    title = "Demo Book One",
                    authors = listOf(com.soft.bookteria.api.models.Author("Tác giả 1")),
                    subjects = listOf("Thể loại A"),
                    bookshelves = emptyList(),
                    copyright = false,
                    downloadCount = 0,
                    languages = listOf("en"),
                    mediaType = "Text",
                    formats = com.soft.bookteria.api.models.Formats(
                        imagejpeg = "https://placehold.co/80x100.png"
                    )
                ),
                Book(
                    id = 101,
                    title = "Demo Book Two",
                    authors = listOf(com.soft.bookteria.api.models.Author("Tác giả 2")),
                    subjects = listOf("Thể loại B"),
                    bookshelves = emptyList(),
                    copyright = false,
                    downloadCount = 0,
                    languages = listOf("en"),
                    mediaType = "Text",
                    formats = com.soft.bookteria.api.models.Formats(
                        imagejpeg = "https://placehold.co/80x100.png"
                    )
                )
            ),
            page = 1L,
            endReached = true,
            error = null
        )

        searchBarState = searchBarState.copy(
            text = "",
            isSearching = false,
            returnedBookList = emptyList(),
            isVisible = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHomeScreenWithFakeVM() {
    val context = LocalContext.current
    val fakeViewModel = remember { FakeHomeVM(context) }
    val navController = rememberNavController()

    HomeScreenContainer(
        viewModel = fakeViewModel,
        networkStatus = NetworkObserver.Status.Avaiable,
        navController = navController,
        backState = remember { mutableStateOf(false) }
    )
}