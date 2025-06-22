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
    val authors: List<Author>,
    @SerialName("bookshelves")
    val bookshelves: List<String>,
    @SerialName("copyright")
    val copyright: Boolean? = null,
    @SerialName("download_count")
    val downloadCount: Int,
    @SerialName("id")
    var id: Long,
    @SerialName("languages")
    val languages: List<String>,
    @SerialName("media_type")
    val mediaType: String? = null,
    @SerialName("subjects")
    val subjects: List<String>,
    @SerialName("title")
    val title: String,
    @SerialName("formats")
    val formats: Formats,
    @SerialName("summaries")
    val summaries: List<String>,
    @SerialName("image/jpeg")
    val imageJpeg: String? = null
)