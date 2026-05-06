package com.mdr.offline.mangas.application

import kotlinx.serialization.Serializable

@Serializable  // For decompose purpose
data class Manga (
    val id: String,
    val title: String,
    val description: String,
    val author: String,
    val coverImageUrl: String? = null,
    val coverImage: ByteArray? = null,
    val originalLanguage: String,
    val status: String,
    val year: String,
    val state: String,
    val format: String,
    val publicationDemographic: String,
    val contentRating: String,
    val genres: List<String>
)