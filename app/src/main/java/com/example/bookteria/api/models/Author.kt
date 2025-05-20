package com.example.bookteria.api.models

import androidx.annotation.Keep
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@OptIn(InternalSerializationApi::class)
@Serializable
data class Author(
    @SerialName("name")
    val name: String = "N/A",
    @SerialName("birth_year")
    val birthYear: Int? = null,
    @SerialName("death_year")
    val deathYear: Int? = null
)
