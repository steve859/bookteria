package com.soft.bookteria.api.models

import androidx.annotation.Keep
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@OptIn(InternalSerializationApi::class)
@Serializable
data class Book (
    @SerialName("authors")
    val authors: List<Author> = emptyList(),
    @SerialName("bookshelves")
    val bookshelves: List<String> = emptyList(),
    @SerialName("copyright")
    val copyright: Boolean? = null,
    @SerialName("download_count")
    val downloadCount: Int = 0,
    @SerialName("id")
    var id: Long = 0,
    @SerialName("languages")
    val languages: List<String> = emptyList(),
    @SerialName("media_type")
    val mediaType: String? = null,
    @SerialName("subjects")
    val subjects: List<String> = emptyList(),
    @SerialName("title")
    val title: String = "",
    @SerialName("formats")
    val formats: Formats = Formats(),
    @SerialName("summaries")
    val summaries: List<String> = emptyList()
)