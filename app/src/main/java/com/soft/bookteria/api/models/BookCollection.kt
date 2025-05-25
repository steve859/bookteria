package com.soft.bookteria.api.models

import androidx.annotation.Keep
import androidx.annotation.OptIn
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@OptIn(InternalSerializationApi::class)
@Serializable
data class BookCollection (
    @SerialName("count")
    val count: Int = 0,
    @SerialName("next")
    val next: String? = null,
    @SerialName("previous")
    val previous: String? = null,
    @SerialName("results")
    val books: List<Book> = listOf(),
    val isCached: Boolean = false
)