package com.soft.bookteria.ui.screens.reader.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soft.bookteria.database.library.LibraryDAO
import com.soft.bookteria.database.library.LibraryObject
import com.soft.bookteria.database.progress.ProgressDAO
import com.soft.bookteria.database.progress.ProgressData
import com.soft.bookteria.ui.screens.reader.utils.EpubResourceExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.github.mertakdut.Reader
import com.github.mertakdut.exception.OutOfPagesException
import com.github.mertakdut.exception.ReadingException
import org.jsoup.Jsoup
import java.io.File

data class ChapterInfo(
    val index: Int,
    val title: String,
    val content: String
)

data class BookInfo(
    val title: String,
    val author: String,
    val totalChapters: Int,
    val chapters: MutableList<ChapterInfo> = mutableListOf(),
    val resourceMap: MutableMap<String, String> = mutableMapOf() // Map of resource paths to local cached files
)

data class ReaderUIState(
    val isLoading: Boolean = true,
    val libraryObject: LibraryObject? = null,
    val progressData: ProgressData? = null,
    val error: String? = null,
    val hasProgressSaved: Boolean = false,
    val bookInfo: BookInfo? = null,
    val currentChapter: ChapterInfo? = null,
    val currentChapterIndex: Int = 0,
    val isLoadingChapter: Boolean = false
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val libraryDAO: LibraryDAO,
    private val progressDAO: ProgressDAO,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ReaderUIState())
    val uiState: StateFlow<ReaderUIState> = _uiState
    
    private val epubReader = Reader()
    private var bookFilePath: String? = null
    
    fun loadBook(libraryObjectId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Load library object
                val libraryObject = libraryDAO.getObjectById(libraryObjectId)
                if (libraryObject == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Book not found"
                    )
                    return@launch
                }
                
                // Check if the book file actually exists
                val file = File(libraryObject.filePath)
                if (!file.exists()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Cannot find downloaded Book. Please try again."
                    )
                    return@launch
                }
                
                // Load progress data
                val progressData = progressDAO.getProgressByLibraryObjectId(libraryObjectId)
                
                // Save file path for later use
                bookFilePath = libraryObject.filePath
                
                // Initialize reader with the EPUB file
                try {
                    epubReader.setMaxContentPerSection(3000) // Characters per section
                    epubReader.setIsIncludingTextContent(true)
                    epubReader.setFullContent(libraryObject.filePath)
                    
                    // Count chapters/sections
                    var totalChapters = 0
                    var moreContent = true
                    while (moreContent) {
                        try {
                            // Just check if the section exists
                            epubReader.readSection(totalChapters)
                            totalChapters++
                        } catch (e: OutOfPagesException) {
                            moreContent = false
                        } catch (e: Exception) {
                            Log.e("ReaderViewModel", "Error counting chapters", e)
                            moreContent = false
                        }
                    }
                    
                    if (totalChapters == 0) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "No readable content found in the book"
                        )
                        return@launch
                    }
                    
                    Log.d("ReaderViewModel", "Total chapters found: $totalChapters")
                    
                    // Create BookInfo
                    val bookInfo = BookInfo(
                        title = libraryObject.title,
                        author = libraryObject.authors,
                        totalChapters = totalChapters
                    )
                    
                    // Initialize UI state with book info
                    _uiState.value = _uiState.value.copy(
                        libraryObject = libraryObject,
                        progressData = progressData,
                        hasProgressSaved = progressData != null,
                        isLoading = true,  // Keep loading while extracting resources
                        error = null,
                        bookInfo = bookInfo
                    )
                    
                    // Extract resources in the background
                    // This helps with showing images in the EPUB
                    val bookId = libraryObject.bookId
                    try {
                        val resourceMap = EpubResourceExtractor.extractResources(
                            context, 
                            libraryObject.filePath, 
                            bookId
                        )
                        
                        // Add the resource map to book info
                        bookInfo.resourceMap.putAll(resourceMap)
                        
                        Log.d("ReaderViewModel", "Extracted ${resourceMap.size} resources from EPUB")
                    } catch (e: Exception) {
                        Log.e("ReaderViewModel", "Failed to extract resources", e)
                        // Continue even if resource extraction fails
                    }
                    
                    // Update UI state with completed loading
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        bookInfo = bookInfo
                    )
                    
                    // Load chapter from saved progress or first chapter
                    val chapterIndex = progressData?.lastChapterIndex ?: 0
                    loadChapter(chapterIndex)
                    
                } catch (e: Exception) {
                    Log.e("ReaderViewModel", "Error parsing EPUB file", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to parse EPUB file: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            } catch (e: Exception) {
                Log.e("ReaderViewModel", "Error loading book", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Unknown error"
                )
            }
        }
    }
    
    fun updateProgress(libraryObjectId: Int, chapterIndex: Int, chapterOffset: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentProgress = progressDAO.getProgressByLibraryObjectId(libraryObjectId)
                
                if (currentProgress != null) {
                    // Update existing progress
                    val updatedProgress = currentProgress.copy(
                        lastChapterIndex = chapterIndex,
                        lastChapterOffset = chapterOffset,
                        lastRead = System.currentTimeMillis()
                    )
                    progressDAO.update(updatedProgress)
                } else {
                    // Create new progress
                    val newProgress = ProgressData(
                        libraryObjectId = libraryObjectId,
                        lastChapterIndex = chapterIndex,
                        lastChapterOffset = chapterOffset,
                        lastRead = System.currentTimeMillis()
                    )
                    progressDAO.insert(newProgress)
                }
                
                // Update UI state
                _uiState.value = _uiState.value.copy(
                    progressData = progressDAO.getProgressByLibraryObjectId(libraryObjectId),
                    hasProgressSaved = true
                )
            } catch (e: Exception) {
                // Handle error silently for progress updates
            }
        }
    }
    
    fun clearProgress(libraryObjectId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                progressDAO.deleteByLibraryObjectId(libraryObjectId)
                
                _uiState.value = _uiState.value.copy(
                    progressData = null,
                    hasProgressSaved = false
                )
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    // Phiên bản cũ, không an toàn cho UI thread
    fun getBookIdFromLibraryId(libraryObjectId: Int): Long? {
        Log.w("ReaderViewModel", "WARNING: Called non-suspend getBookIdFromLibraryId on main thread!")
        return null
    }
    
    // Phiên bản suspend an toàn cho tương tác database
    suspend fun getBookIdFromLibraryIdSuspend(libraryObjectId: Int): Long? {
        return withContext(Dispatchers.IO) {
            try {
                val libraryObject = libraryDAO.getObjectById(libraryObjectId)
                libraryObject?.bookId?.toLong()
            } catch (e: Exception) {
                Log.e("ReaderViewModel", "Error getting bookId from libraryId", e)
                null
            }
        }
    }
    
    /**
     * Loads a specific chapter from the book
     */
    fun loadChapter(chapterIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoadingChapter = true
                )
                
                // Validate chapter index
                val totalChapters = _uiState.value.bookInfo?.totalChapters ?: 0
                if (chapterIndex < 0 || chapterIndex >= totalChapters) {
                    Log.e("ReaderViewModel", "Invalid chapter index: $chapterIndex, total: $totalChapters")
                    _uiState.value = _uiState.value.copy(
                        isLoadingChapter = false,
                        error = "Invalid chapter index"
                    )
                    return@launch
                }
                
                // Check if we've already loaded this chapter
                val existingChapter = _uiState.value.bookInfo?.chapters?.find { it.index == chapterIndex }
                if (existingChapter != null) {
                    // Use cached chapter
                    Log.d("ReaderViewModel", "Using cached chapter: ${existingChapter.title}")
                    _uiState.value = _uiState.value.copy(
                        currentChapter = existingChapter,
                        currentChapterIndex = chapterIndex,
                        isLoadingChapter = false
                    )
                    
                    // Save reading progress
                    _uiState.value.libraryObject?.let { libraryObject ->
                        updateProgress(libraryObject.id, chapterIndex, 0)
                    }
                    return@launch
                }
                
                // If file path is not available, show error
                if (bookFilePath == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingChapter = false,
                        error = "Book file path is not available"
                    )
                    return@launch
                }
                
                // Load the chapter content from EPUB
                try {
                    // Read the chapter content
                    val sectionInfo = epubReader.readSection(chapterIndex)
                    var content = sectionInfo.sectionContent ?: "Content not available"
                    
                    // Extract better chapter title from HTML content if possible
                    val chapterTitle = extractChapterTitle(content, chapterIndex)
                    
                    // Process HTML content to improve reading experience
                    content = processHtmlContent(content)
                    
                    Log.d("ReaderViewModel", "Loaded chapter: $chapterTitle with ${content.length} characters")
                    
                    // Create chapter info
                    val chapterInfo = ChapterInfo(
                        index = chapterIndex,
                        title = chapterTitle,
                        content = content
                    )
                    
                    // Add to cached chapters
                    _uiState.value.bookInfo?.chapters?.add(chapterInfo)
                    
                    // Update current chapter in the UI state
                    _uiState.value = _uiState.value.copy(
                        currentChapter = chapterInfo,
                        currentChapterIndex = chapterIndex,
                        isLoadingChapter = false
                    )
                    
                    // Save reading progress
                    _uiState.value.libraryObject?.let { libraryObject ->
                        updateProgress(libraryObject.id, chapterIndex, 0)
                    }
                } catch (e: OutOfPagesException) {
                    // Handle attempt to read past the end of the book
                    Log.e("ReaderViewModel", "No more chapters available", e)
                    _uiState.value = _uiState.value.copy(
                        isLoadingChapter = false,
                        error = "No more chapters available"
                    )
                } catch (e: ReadingException) {
                    // Handle epub reading errors
                    Log.e("ReaderViewModel", "Error reading chapter", e)
                    _uiState.value = _uiState.value.copy(
                        isLoadingChapter = false,
                        error = "Error reading chapter: ${e.localizedMessage}"
                    )
                }
            } catch (e: Exception) {
                Log.e("ReaderViewModel", "Error loading chapter $chapterIndex", e)
                _uiState.value = _uiState.value.copy(
                    isLoadingChapter = false,
                    error = "Error loading chapter: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }
    
    /**
     * Process HTML content for better reading experience
     * Maintains formatting and updates image references to point to local files
     */
    private fun processHtmlContent(html: String): String {
        try {
            // First, replace image references using our extracted resources
            val resourceMap = _uiState.value.bookInfo?.resourceMap ?: emptyMap()
            var processedHtml = if (resourceMap.isNotEmpty()) {
                EpubResourceExtractor.processHtmlWithLocalImages(html, resourceMap)
            } else {
                html
            }
            
            // Parse HTML with JSoup
            val document = Jsoup.parse(processedHtml)
            
            // Process images to make them visible
            document.select("img").forEach { img ->
                // Get the src attribute
                val src = img.attr("src")
                
                // Set default styling for images
                img.attr("width", "100%")
                img.attr("height", "auto")
                
                Log.d("ReaderViewModel", "Processing image reference: $src")
            }
            
            // Clean up the HTML
            document.select("script").remove() // Remove any scripts
            
            // Return the HTML as string - this preserves formatting
            return document.body()?.html() ?: processedHtml
        } catch (e: Exception) {
            Log.e("ReaderViewModel", "Error processing HTML content", e)
            return html
        }
    }
    
    /**
     * Get a list of all chapter titles for navigation
     * Uses improved title extraction
     */
    suspend fun getChapterList(): List<Pair<Int, String>> {
        return withContext(Dispatchers.IO) {
            try {
                val chapterList = mutableListOf<Pair<Int, String>>()
                val bookInfo = _uiState.value.bookInfo ?: return@withContext emptyList()
                val totalChapters = bookInfo.totalChapters
                
                // First use already loaded chapters
                bookInfo.chapters.forEach { chapter ->
                    chapterList.add(Pair(chapter.index, chapter.title))
                }
                
                // Fill in any missing chapters with improved title extraction
                for (i in 0 until totalChapters) {
                    if (!chapterList.any { it.first == i }) {
                        try {
                            epubReader.setIsIncludingTextContent(true) // Need content for title extraction
                            val sectionInfo = epubReader.readSection(i)
                            val content = sectionInfo.sectionContent ?: ""
                            
                            // Extract a meaningful title
                            val title = extractChapterTitle(content, i)
                            chapterList.add(Pair(i, title))
                            
                            // Set back to false to save memory for future calls
                            epubReader.setIsIncludingTextContent(false)
                        } catch (e: Exception) {
                            Log.e("ReaderViewModel", "Error getting title for chapter $i", e)
                            chapterList.add(Pair(i, "Chapter ${i + 1}"))
                        }
                    }
                }
                
                // Sort by index
                chapterList.sortBy { it.first }
                
                // If no chapters found, create a placeholder
                if (chapterList.isEmpty() && totalChapters > 0) {
                    chapterList.add(Pair(0, "Chapter 1"))
                }
                
                chapterList
            } catch (e: Exception) {
                Log.e("ReaderViewModel", "Error getting chapter list", e)
                emptyList()
            }
        }
    }
    
    /**
     * Navigate to the next chapter if available
     * Returns true if navigation was successful
     */
    fun nextChapter(): Boolean {
        val currentIndex = _uiState.value.currentChapterIndex
        val totalChapters = _uiState.value.bookInfo?.totalChapters ?: 0
        
        if (currentIndex < totalChapters - 1) {
            loadChapter(currentIndex + 1)
            return true
        } else {
            // We're at the last chapter, show a toast or some feedback
            Log.d("ReaderViewModel", "Already at the last chapter")
            return false
        }
    }
    
    /**
     * Navigate to the previous chapter if available
     * Returns true if navigation was successful
     */
    fun previousChapter(): Boolean {
        val currentIndex = _uiState.value.currentChapterIndex
        
        if (currentIndex > 0) {
            loadChapter(currentIndex - 1)
            return true
        } else {
            // We're at the first chapter, show a toast or some feedback
            Log.d("ReaderViewModel", "Already at the first chapter")
            return false
        }
    }
    
    /**
     * Returns the current reading progress as a percentage through the book
     */
    fun getCurrentReadingProgress(): Float {
        val currentIndex = _uiState.value.currentChapterIndex
        val totalChapters = _uiState.value.bookInfo?.totalChapters ?: 1
        
        return currentIndex.toFloat() / totalChapters.toFloat()
    }
    
    /**
     * Search for text within the current chapter
     * Future enhancement possibility
     */
    fun searchInCurrentChapter(searchText: String): List<Int> {
        // This is a placeholder for future implementation
        // Would return list of positions where the text is found
        return emptyList()
    }
    
    /**
     * Extracts a meaningful chapter title from HTML content
     * Uses common HTML elements like headings to find a title
     */
    private fun extractChapterTitle(html: String, chapterIndex: Int): String {
        try {
            // Default title as fallback
            val defaultTitle = "Chapter ${chapterIndex + 1}"
            
            // Try to parse the HTML with JSoup
            val document = Jsoup.parse(html)
            
            // Look for heading elements in order of importance
            val h1 = document.select("h1").firstOrNull()?.text()
            if (!h1.isNullOrBlank()) return h1
            
            val h2 = document.select("h2").firstOrNull()?.text()
            if (!h2.isNullOrBlank()) return h2
            
            val h3 = document.select("h3").firstOrNull()?.text()
            if (!h3.isNullOrBlank()) return h3
            
            // Look for elements with common title class names
            val titleElements = document.select(".title, .chapter-title, .heading")
            if (titleElements.isNotEmpty()) {
                val text = titleElements.first()?.text()
                if (!text.isNullOrBlank()) {
                    return text
                }
            }
            
            // Look for the first paragraph which might contain the title
            val firstP = document.select("p").firstOrNull()
            if (firstP != null && firstP.text().length < 100) {
                // If the first paragraph is short, it might be a title
                return firstP.text()
            }
            
            // If we can't find a suitable title, return the default
            return defaultTitle
        } catch (e: Exception) {
            Log.e("ReaderViewModel", "Error extracting chapter title", e)
            return "Chapter ${chapterIndex + 1}"
        }
    }
    
    /**
     * Cleans up resources when the reader is done
     * Should be called when leaving the reader
     */
    fun cleanupResources() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val libraryObject = _uiState.value.libraryObject
                if (libraryObject != null) {
                    // Clear extracted resources
                    EpubResourceExtractor.clearBookCache(context, libraryObject.bookId)
                    Log.d("ReaderViewModel", "Cleaned up resources for book ID: ${libraryObject.bookId}")
                }
            } catch (e: Exception) {
                Log.e("ReaderViewModel", "Error cleaning up resources", e)
            }
        }
    }
}