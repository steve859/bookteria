package com.soft.bookteria.ui.screens.reader.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soft.bookteria.database.library.LibraryDAO
import com.soft.bookteria.database.progress.ProgressDAO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReaderDetailScreenState(
    val isLoading: Boolean = true,
    val title: String = "",
    val authors: String = "",
    val coverImage: Any? = null,
    val chapters: List<String> = emptyList(),
    val hasProgressSaved: Boolean = false,
    val progressPercent: String = "",
    val error: String? = null
)

@HiltViewModel
class ReaderDetailViewModel @Inject constructor(
    private val libraryDAO: LibraryDAO,
    private val progressDAO: ProgressDAO
) : ViewModel() {

    var state by mutableStateOf(ReaderDetailScreenState())

    fun loadEbookData(libraryObjectId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val libraryItem = libraryDAO.getObjectById(libraryObjectId.toInt())
                
                // Check if library item exists
                if (libraryItem == null) {
                    state = state.copy(isLoading = false, error = "Library item not found.")
                    return@launch
                }
                
                // Get progress data for the current book
                val progressData = progressDAO.getProgressByLibraryObjectId(libraryObjectId.toInt())
                
                // Generate sample chapters (in real app, this would come from EPUB parser)
                val sampleChapters = generateSampleChapters(libraryItem.title)
                
                // Calculate progress percentage
                val progressPercent = if (progressData != null) {
                    val percentage = ((progressData.lastChapterIndex + 1).toFloat() / sampleChapters.size.toFloat()) * 100
                    String.format("%.1f", percentage)
                } else {
                    "0.0"
                }
                
                state = state.copy(
                    title = libraryItem.title,
                    authors = libraryItem.authors,
                    coverImage = null, // In real app, this would be the cover image
                    chapters = sampleChapters,
                    hasProgressSaved = progressData != null,
                    progressPercent = progressPercent
                )
                
                delay(350) // Small delay for smooth transition
                state = state.copy(isLoading = false)
                
            } catch (e: Exception) {
                Log.e("ReaderDetailViewModel", "Error loading ebook data", e)
                state = state.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Unknown error occurred"
                )
            }
        }
    }
    
    private fun generateSampleChapters(bookTitle: String): List<String> {
        return listOf(
            "Chapter 1: Introduction",
            "Chapter 2: The Beginning",
            "Chapter 3: Rising Action",
            "Chapter 4: Climax",
            "Chapter 5: Falling Action",
            "Chapter 6: Conclusion"
        )
    }
} 