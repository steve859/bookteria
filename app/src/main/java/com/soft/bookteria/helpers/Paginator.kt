package com.soft.bookteria.helpers

class Paginator<Page, BookCollection>(
    private val initialPage: Page,
    private val loadPage: suspend (Page) -> Result<BookCollection>,
    private val getNextPage: suspend (BookCollection) -> Page,
    private val onError: suspend (Throwable?) -> Unit,
    private val onSuccess: suspend (BookCollection, Long) -> Unit,
    private val onLoadingChanged: (Boolean) -> Unit,
    private val onDataLoaded: (BookCollection) -> Unit,
    private val onLoadUpdated: (Boolean) -> Unit,
    private val onRequest: suspend (nextPage: Page) -> Result<BookCollection>
    ){
    private var currentPage = initialPage
    private var isLoading = false
    suspend fun loadNext() {
        if (isLoading) return
        isLoading = true
        onLoadingChanged(true)
        
        val result = loadPage(currentPage)
        isLoading = false
        onLoadingChanged(false)
        
        result.fold(
            onSuccess = { data ->
                onDataLoaded(data)
                getNextPage(data)?.let {
                    currentPage = it
                }
            },
            onFailure = { error ->
                onError(error)
            }
        )
    }
    
    fun reset() {
        currentPage = initialPage
    }
}