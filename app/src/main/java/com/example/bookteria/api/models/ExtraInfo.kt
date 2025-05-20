package com.example.bookteria.api.models

data class ExtraInfo (
    val image: String ="",
    val pageCount: Int = 0,
    val description: String = "",
    val isCached: Boolean = false
)