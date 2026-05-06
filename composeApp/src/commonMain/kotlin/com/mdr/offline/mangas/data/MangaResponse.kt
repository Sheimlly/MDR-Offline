package com.mdr.offline.mangas.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MangaResponse(
    @SerialName("result")
    val result: String,
    @SerialName("data")
    val mangas: List<MangaRaw>
)

class MangaReadChapters {
    @Serializable
    data class OK_200(
        @SerialName("result")
        val result: String,
        @SerialName("data")
        val data: Array<String>
    )

}


@Serializable
data class MangaReadUpdateRequest(
    val chapterIdsRead: List<String>,
    val chapterIdsUnread: List<String>
)