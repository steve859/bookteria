package com.soft.bookteria.ui.screens.reader.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soft.bookteria.database.library.LibraryDAO
import com.soft.bookteria.database.library.LibraryObject
import com.soft.bookteria.database.progress.ProgressDAO
import com.soft.bookteria.database.progress.ProgressData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReaderUIState(
    val isLoading: Boolean = true,
    val libraryObject: LibraryObject? = null,
    val progressData: ProgressData? = null,
    val error: String? = null,
    val hasProgressSaved: Boolean = false
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val libraryDAO: LibraryDAO,
    private val progressDAO: ProgressDAO
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ReaderUIState())
    val uiState: StateFlow<ReaderUIState> = _uiState
    
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
                
                // Load progress data
                val progressData = progressDAO.getProgressByLibraryObjectId(libraryObjectId)
                
                _uiState.value = _uiState.value.copy(
                    libraryObject = libraryObject,
                    progressData = progressData,
                    hasProgressSaved = progressData != null,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
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
} 