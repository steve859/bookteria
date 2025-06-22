package com.soft.bookteria.helpers

class Paginator<Page, BookCollection>(
    private val initialPage: Page,
    private val loadPage: suspend (Page) -> Result<BookCollection>,
    private val getNextPage: (BookCollection) -> Page?,
    private val onError: (Throwable?) -> Unit,
    private val onSuccess: (BookCollection, Page) -> Unit,
    private val onLoadingChanged: (Boolean) -> Unit
) {
    private var currentPage = initialPage
    private var isLoading = false
    
    suspend fun loadNextItems() {
        if (isLoading) return
        
        isLoading = true
        onLoadingChanged(true)
        
        val result = loadPage(currentPage)
        isLoading = false
        onLoadingChanged(false)
        
        result.fold(
            onSuccess = { data ->
                onSuccess(data, currentPage)
                getNextPage(data)?.let { nextPage ->
                    currentPage = nextPage
                }
            },
            onFailure = { error ->
                onError(error)
            }
        )
    }
    
    fun reset() {
        currentPage = initialPage
        isLoading = false
    }
}