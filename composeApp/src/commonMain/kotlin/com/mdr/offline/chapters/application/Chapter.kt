package com.mdr.offline.chapters.application

import com.mdr.offline.chapters.data.ChapterDetailsService
import kotlinx.serialization.Serializable

@Serializable  // For decompose purpose
data class Chapter (
    val combinedId: String? = null,
    val mangaId: String? = null,
    val id: String,
    val title: String?,
    val volume: String?,
    val chapter: String,
    val scanlationGroup: String,
    val pageNumbers: Int,
    val pages: List<String>? = emptyList(),
    val imagesPath: List<String>? = emptyList(),
    val lastReadPage: Int? = 1,
    val read: Boolean,
    val filesDownloaded: Boolean
)

private data class ChapterIds(
    val id: String,
    val chapter: String
)