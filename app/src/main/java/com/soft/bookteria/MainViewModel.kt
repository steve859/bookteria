package com.soft.bookteria

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.soft.bookteria.database.library.LibraryDAO
import com.soft.bookteria.database.progress.ProgressDAO
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val libraryDAO: LibraryDAO,
    private val progressDAO: ProgressDAO
) : ViewModel() {
    private val _isLoading: MutableState<Boolean> = mutableStateOf(true)
    val isLoading: MutableState<Boolean> = _isLoading
    
    private val _startDestination: MutableState<String> = mutableStateOf("")
    val startDestination: MutableState<String> = _startDestination
    
    companion object{
    
    }
}