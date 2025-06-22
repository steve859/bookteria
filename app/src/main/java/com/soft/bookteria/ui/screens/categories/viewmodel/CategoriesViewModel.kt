package com.soft.bookteria.ui.screens.categories.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soft.bookteria.api.BookApi
import com.soft.bookteria.api.models.Book
import com.soft.bookteria.api.models.BookCollection
import com.soft.bookteria.helpers.Paginator
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


data class CategoriesBookState(
    val categories: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedCategory: String? = null,
    val books: List<Book> = emptyList(),
    val page: Long = 1L,
    val endReached: Boolean = false
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val booksApi: BookApi
): ViewModel() {
    private lateinit var pagination: Paginator<Long, BookCollection>
    var state by mutableStateOf(CategoriesBookState())
    
    fun loadBookByCategory(category: String) {
        Log.d("CategoriesViewModel", "Loading books for category: $category")
        if (!this::pagination.isInitialized || state.selectedCategory != category) {
            state = state.copy(
                selectedCategory = category,
                books = emptyList(),
                page = 1L,
                endReached = false,
                error = null
            )
            
            pagination = Paginator(
                initialPage = 1L,
                loadPage = { page ->
                    try {
                        if (page == 1L) delay(400L)
                        Log.d("CategoriesViewModel", "Making API call for category: $category, page: $page")
                        val result = booksApi.getBookByCategory(category, page)
                        Log.d("CategoriesViewModel", "API result: $result")
                        result
                    } catch (exc: Exception) {
                        Log.e("CategoriesViewModel", "API error: ${exc.message}", exc)
                        Result.failure(exc)
                    }
                },
                getNextPage = { bookCollection ->
                    if (bookCollection.books.isNotEmpty()) {
                        state.page + 1L
                    } else {
                        null
                    }
                },
                onError = { error ->
                    Log.e("CategoriesViewModel", "Paginator error: ${error?.message}", error)
                    state = state.copy(
                        error = error?.localizedMessage ?: "Unknown error occurred"
                    )
                },
                onSuccess = { bookCollection, currentPage ->
                    Log.d("CategoriesViewModel", "Received ${bookCollection.books.size} books for category: $category")
                    val books = bookCollection.books.filter {
                        it.formats.applicationepubzip != null
                    }
                    Log.d("CategoriesViewModel", "Filtered to ${books.size} books with EPUB format")
                    
                    state = state.copy(
                        books = (state.books + books),
                        page = currentPage,
                        endReached = books.isEmpty(),
                        isLoading = false
                    )
                },
                onLoadingChanged = { isLoading ->
                    state = state.copy(isLoading = isLoading)
                }
            )

            loadNextItems()
        }
    }
    
    fun loadNextItems() {
        if (this::pagination.isInitialized) {
            viewModelScope.launch {
                pagination.loadNextItems()
            }
        }
    }
    
    fun reloadItems() {
        state.selectedCategory?.let { category ->
            loadBookByCategory(category)
        }
    }
    
    fun clearSelection() {
        state = state.copy(
            selectedCategory = null,
            books = emptyList(),
            page = 1L,
            endReached = false,
            error = null
        )
    }
}