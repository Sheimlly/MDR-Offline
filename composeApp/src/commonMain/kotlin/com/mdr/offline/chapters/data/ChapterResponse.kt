package com.mdr.offline.chapters.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChapterResponse(
    @SerialName("result")
    val result: String,
    @SerialName("data")
    val chapters: List<ChapterRaw>
)

@Serializable
data class SingleChapterResponse(
    @SerialName("result")
    val result: String,
    @SerialName("data")
    val chapter: ChapterRaw
)